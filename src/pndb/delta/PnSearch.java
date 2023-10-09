package pndb.delta;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;

import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXPlayer;
import pndb.delta.constants.Auxiliary;
import pndb.delta.constants.CellState;
import pndb.delta.constants.Constants;
import pndb.delta.constants.GameState;
import pndb.delta.tt.TranspositionTable;
import pndb.delta.tt.TTElementNode;
import pndb.delta.tt.TTElementProved;
import pndb.delta.tt.TTElementProved.KeyDepth;



/**
 * notes:
 * <p>	-	i'm always CellState.ME, GameState.P1.
 * <p>	-	TT: always used with MY_PLAYER = 0, YOUR = 1 (for state).
 * <p>	-	TT is used for positions evaluated and verified by db, so it contains certain values.
 * <p>	-	(tryit)	TT uses global depths.
 * <P>	-	tt.doppia entry: scopo: in base a chi tocca si usa indice
 * 
 * <p>Enhancements:
 * <p>	1.	(tryit) Saves all levels for endgames (in TT) and, for each visit, saves the deepest one,
 * 			so it can return it in case all moves are losing.
 * <p>	2.	(nonmc) no BoardBit.MC
 * <p>	3.	(nocel) no BoardBitDb.alignments_by_cell.
 * <p>	4.	(gamma) int for BoardBitDb.hasAlignments
 * <p>	5.	(halfn) use half number proof init
 * <p>	6.	(ranch) Randomly shuffles children with same priority, so they are analyzed in a different order
 * <p>	7.	() only create BoardBitDb.alignemts_by_dir when created first element
 * <p>	8.	(scomb) board.getCombined only search for attacker's threats, cell by cell (for those added).
 * <p>	9.	(scomb) board.getCombined: already checks isUseful(threat), so useless to check in findAlignments
 * <p>	10.	(delta) db correctly: cxgame is not like mnkgame, where one more move never gives any disadvantage
 * 			(but usually an advantage), so db performing all defending moves at the same time isn't (always)
 * 			correct. Idea for the correct implementation, which also mixes pn and db: you have to try each
 * 			defending move, individually, and make massive use of TT, as db will also need to check many
 * 			configurations multiple times; when player A creates a X-1, B is forced to reply (so all other moves
 * 			can be removed from pn); when creates a X-2, B is forced to reply (unless he has a better threat,
 * 			enhancement already used in db) - alternatively, he could make another move and be forced in the next
 * 			trun (when it's became a X-1)... (thinking in progress)... so, conclusion: can I just add a new operator,
 * 			for this case?? i.e. add a piece, and the empty cell above is part of a threat (also not in vertical 
 * 			direction).
 * <p>	11.	(delta) draw=max depth + 1, so is preferred in case of loss
 * <p>	12. db not correct, but almost: 1. only tier3 are not correct (involve more responses);
 * 			2. you could reach that position also putting attacker's moves, so wrong cases are rare.
 * <p>	13. (delta) new threat (stacked)... see note Operators.makeStacked()
 * 			...dubious
 * <p>	14. problem with transpositions in dags (thesis), ambiguity
 * <p>	15.	(delta) pnNode: more parents (dag)
 * <p>	16. (delta) no prune, keep nodes for next visits (using tt for each node)
 * <p>	17.	M*N < 2**15, because of short type for numbers
 * <p>	18.	dbSearch: note about wins by mistake, with threats of tier > 1 (intersecting with tier 1 ones)
 * <p>	19. why deleting previous nodes: i guess if you managed to prove a node from a previous position, which was
 * 			more generical, you'll probably be able to re-do it now, with less nodes.
 * <p>	20.	TT.remove at start of each selectColumn, to remove entries from previous rounds
 * <p>	21.	each node is inserted in dag at creation time
 * <p>	22.	in list nodes to remove, same node could be put more times: so use set
 * 
 * TODO;
 * .db corretto: db ricorsivo su tier3 (con più risposte)
 * ..oppure: provale tutte, con accortezze: 1. usa tutte minacce di 1atk 1def (saranno molte combinazioni in più);
 * 2. a questo punto in combine, stai attento che da ognuno si aggiunga l'intera minaccia (1atk e 1def, non solo atk) (se no 
 * rischi di aggiungere più atk che def e la situazione non sarebbe raggiungibile)
 * .stacked 13 dubious (are these new operators well made?)
 * 
 * .merge 2 threat classes, remove appliers
 * .(alla fine) aumenta tempo (tanto basta), ma metti comunque i check per interromperlo nel caso durante l'esecuzione (parti più costose: develop, ancestors, db)
 * .count mem, n of nodes
 * .to reduce mem: 1. implement pnnode.children/parents with lists parent/siblings;
 * 2. merge TTElementNode + PnNode in 1 class
 * 
 * check there are not problems with def.length=0 with new operator
 * 
 */
public class PnSearch implements CXPlayer {

	//#region CONSTANTS
	protected static final byte PROOF		= PnNode.PROOF;
	protected static final byte DISPROOF	= PnNode.DISPROOF;

	protected byte MY_PLAYER	= CellState.P1;
	protected byte YOUR_PLAYER	= CellState.P2;
	protected byte MY_WIN		= GameState.WINP1;
	protected byte YOUR_WIN		= GameState.WINP2;

	//#endregion CONSTANTS

	// board
	private boolean first;
	public BoardBitPn board;			// public for debug
	protected TranspositionTable<TTElementNode, TTElementNode.KeyDepth> TTdag;
	protected TranspositionTable<TTElementProved, KeyDepth> TTproved;
	public byte current_player;			// public for debug
	protected DbSearch dbSearch;
	
	// nodes
	protected PnNode root;
	
	// time / memory
	protected long timer_start;			//turn start (milliseconds)
	protected long timer_duration;		//time (millisecs) at which to stop timer
	protected Runtime runtime;
	
	// implementation
	private TTElementNode.KeyDepth TTdag_key;
	private KeyDepth TTproved_key;
	private int			move_to_deepest;	// auxiliary to getDeepestNode
	protected PnNode	lastIt_root;
	protected short[]	lastIt_freeCols;
	protected long		lastIt_hash;

	// debug
	private final boolean DEBUG_PROVED	= false;
	private final boolean DEBUG_TIME	= false;
	protected String log;
	private long ms;
	private int visit_loops_n;
	private long depth_last;
	private long[] time_last = new long[5];
	private testerMemoryStruct[] mem_last = new testerMemoryStruct[5];
	private long[] time_last_a = new long[3];
	private PnNode node_last_db;
	private int file_rand;
	private FileWriter file_proved;
	private int proved_n = 0;
	private int dag_n = 0;

	
	

	public PnSearch() {}
	
	/**
	 * Complexity: O(5N + 4MN + 2**16 + (5MN + 3M+4N + 2**16) ) = O(9MN + 3M+9N + 2**17)
	 */
	@Override
	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {

		this.first	= first;
		board		= new BoardBitPn(M, N, X);
		dbSearch	= new DbSearch();		
		TTdag		= new TranspositionTable<TTElementNode, TTElementNode.KeyDepth>(M, N, TTElementNode.getTable());
		TTproved	= new TranspositionTable<TTElementProved, KeyDepth>(M, N, TTElementProved.getTable());

		BoardBitPn.TTdag 	= TTdag;
		BoardBitPn.TTproved	= TTproved;
		TTdag_key		= new TTElementNode.KeyDepth();
		TTproved_key	= new TTElementProved.KeyDepth();

		if(first)	current_player = CellState.P1;
		else		current_player = CellState.P2;

		lastIt_freeCols = new short[N];
		
		timer_duration = (timeout_in_secs - 1) * 1000;
		runtime = Runtime.getRuntime();

		// dbSearch instantiated by subclass
		dbSearch.init(M, N, X, first);

		// debug
		for(int i = 0; i < mem_last.length; i++) mem_last[i] = new testerMemoryStruct(0,0,0,0);
		System.out.println("\n-_-\nSTART GAME\n-_-\n");
		file_rand = new Random().nextInt();
		System.out.println("RAND HASH:"+file_rand);
		
	}

	/**
	 * Complexity: O(4X + )
	 */
	@Override
	public int selectColumn(CXBoard B) {

		// debug
		log = "";
		try {
			
			timer_start = System.currentTimeMillis();
			
			/* update own board.
			useless to insert new root in dag now, if need to create it: this root won't exist in next rounds */
			CXCell[] MC = B.getMarkedCells();
			if(MC.length > 0)
				markCheck(B.getLastMove().j);

			// see if new root was already visited, otherwise create it
			TTElementNode entry = TTdag.get(TTElementNode.setKey(TTdag_key, board.hash_dag, MC.length));
			if(entry == null) {
				root = new PnNode((short)MC.length, board.hash_dag);
				root.setProofAndDisproof(1, 1);
			}
			else root = entry.node;

			// remove unreachable nodes from previous rounds
			root.tag = root.depth;		// unique tag for each round
			if(lastIt_root != null) {
				root.tagTree();
				removeUnmarkedTree(lastIt_root, board.hash_dag, current_player);
			}

			// debug
			if(DEBUG_PROVED) file_proved = new FileWriter("debug/pn/"+ file_rand +"proved" + MC.length + ".txt");
			System.out.println("---\n" + playerName());
			System.out.println("Opponent: " + ((B.getLastMove() == null) ? null : B.getLastMove().j) );
			System.out.println("root hash_proved:" + board.hash_proved );
			board.print();
			System.out.println("root existed in dag: " + (TTdag.get(TTElementNode.setKey(TTdag_key, board.hash_dag, MC.length)) != null) );
			testerMemoryStruct mem = new testerMemoryStruct(runtime.maxMemory(), runtime.totalMemory(), runtime.freeMemory(), Auxiliary.freeMemory(runtime));
			System.out.println("time after clean, before gc: " + (System.currentTimeMillis() - timer_start) );
			System.out.println("mem before gc: " + mem);
			
			runtime.gc();
			
			// debug
			mem.set(runtime.maxMemory(), runtime.totalMemory(), runtime.freeMemory(), Auxiliary.freeMemory(runtime));
			System.out.println("time before start visit: " + (System.currentTimeMillis() - timer_start) );
			System.out.println("mem after gc: " + mem);
			
			// visit
			TTElementProved root_eval = getEntryProved(board.hash_proved, TTElementProved.COL_NULL, root.depth, current_player);
			if(root_eval == null) {
				visit();
				root_eval = getEntryProved(board.hash_proved, TTElementProved.COL_NULL, root.depth, current_player);
			}

			
			// debug
			if(DEBUG_PROVED) file_proved.close();
			log += "ended visit\nproved_n=" + proved_n + "\tdag_n=" + dag_n + "\n";
			log += "root_eval=null:" + (root_eval==null) + ",root_children=null:"+(root.children==null) + "\n";
			if(root_eval != null) {
				log += "depths " + root.depth + " " + root_eval.depth_cur + "\n";
				log += root_eval.col() + " "+root_eval.won()+" " +root_eval.depth_reachable + "\n";
			}
			
			
			lastIt_root = root;
			
			// update implementative iteration variables, before marking moves
			lastIt_hash = board.hash_dag;
			if(MC.length > 0) {
				lastIt_hash = TTdag.getHash(lastIt_hash, MC[MC.length - 1].i, MC[MC.length - 1].j, Auxiliary.getPlayerBit(YOUR_PLAYER));
				lastIt_freeCols[MC[MC.length - 1].j]++;
				if(MC.length > 1) {
					lastIt_hash = TTdag.getHash(lastIt_hash, MC[MC.length - 2].i, MC[MC.length - 2].j, Auxiliary.getPlayerBit(YOUR_PLAYER));
					lastIt_freeCols[MC[MC.length - 2].j]++;
				}
			}
			
			int move;
			if(root_eval != null)
				move = root_eval.col();
			else
				move = root.lastMoveForChild(bestNode());
			markCheck(move);

			// debug
			log += "before debug&return\n";
			log += root.debugString(root);
			log += "\nMy move: " + move + "\n";
			log += board.printString(0);
			log += "time,mem before return: " + (System.currentTimeMillis() - timer_start) + " " + Auxiliary.freeMemory(runtime) + "\n";
			System.out.println("\nLOG:\n" + log);

			return move;

		} catch (IOException e) {
			System.out.println("\nIO\nSTART ERROR: log pn: " + log + " error: " + e);
			return -1;
		} catch (Exception e) {
			System.out.println("START ERROR: log pn: " + log + " error: " + e);
			try{
			if(DEBUG_PROVED) file_proved.close();
			}catch(Exception e2) {System.out.println("stocazzo");}
			throw e;
		}

	}

	@Override
	public String playerName() {
		return "PnDb delta";
	}


	//#region PN-SEARCH

		/**
		 * iterative loop for visit.
		 * Complexity: 
		 * 		one iteration:
		 * 			O( (h + 4X) + (2DbSearch + 2N**2 + (17+4X)N) + (Nh) )
		 * 			= O( 2DbSearch + (N+1)h + 2N**2 + (17+8X)N )
		 * 			= O( 2DbSearch + 2N**2 + Nh + 17N + 8XN ), with M similar to N
		 * 
		 * 		full tree:
		 * 			N*O(2Db+2N**2+N+17N+8XN) + N**2*O(2Db+2N**2+2N+17N+8XN) + ...
		 * 			... + N**N*O(2Db+2N**2+NN+17N+8XN) + ...
		 * 			... + N**(N*M)*O(2Db+2N**2+NMN+17N+8XN)
		 * 
		 * 			= (N+N**2+..+N**(NM)) * (it)			+ (N*N + N**2*2N + N**3*3N +..+ N**(NM)*NMN)
		 * 			= ( (N**(NM+1) - N) / (N-1) ) * (it)	+ (N**2 + 2N**3 + 3N**4 +..+ N**(N**2)*N**3)
		 * 			= (N**NM) * (it)						+ ( (N**(N**2+3) - N**2) / (N - 1) )
		 * 			= (N**(N**2)) * (it)					+ (N**(N**2+2))
		 * 
		 * 			= (N**(N**2)) * (2DbSearch + 2N**2 + 17N + 8XN) + (N**(N**2+2))
		 * 			= (N**(N**2)) * (2DbSearch + 2N**2 + 17N + 8XN) + (N**(N**2+2))
		 * 
		 * 			assuming a quite broad development of the tree (or, anyway, you consider an avg h).
		 * 
		 * 		H=max depth reached:
		 * 			= (N + N**2 +..+ N**H) * (it)		+ (N**2 + 2N**3 + 3N**4 +..+ N**(H+1)*HN)
		 * 			= ( (N**(H+1) - N) / (N-1) ) * (it)	+ ( (N**(H+1+1+1) - N**2) / (N - 1) )
		 * 			= O(N**H) * (it)					+ O(N**(H+2))
		 * 			= O(N**H) * (2DbSearch + 2N**2 + 17N + 8XN)					+ O(N**(H+2))
		 * 			= O(N**H * 2DbSearch + 2N**(H+2) + 17N**(H+1) + N**(H+1)8X)	+ O(N**H)
		 * 			= O(N**H * 2DbSearch + 2N**(H+2) + N**(H+2) + N**(H+2))		+ O(N**H) 	-- note(1)
		 * 			= O(N**H * 2DbSearch + 4N**(H+2))	+ O(N**H)
		 * 			= O(N**H * 2DbSearch + 4N**(H+2))
		 * 
		 * 			assuming all moves at each depth are developed, which is not true at all.
		 * 			assuming H << N (otherwise, you would have to add 1 to exponent).
		 * 			actually: updateAncestors doesn't always go to top, so it must cost less.
		 * 			note(1): 8X is probably similar to N, and 17 too (sometimes), so we can round it all, also
		 * 				considering some other overhead from constants, and round al to O(N**(NH+2)), which is absorbed
		 * 				by the other O(N**(NH+2)) anyway.
		 * 
		 * ---
		 * 			Notes: N*M is the max depth, N**h is the number of children at depth h.
		 * 			it = O(iteration) - Nh = O(2DbSearch + 2N**2 + 17N + 8XN);
		 * @return
		 */
		private void visit() throws IOException {

			// debug
			visit_loops_n = 0;
			ms = System.currentTimeMillis();

			HashSet<PnNode> nodes_proved = new HashSet<PnNode>();		// nodes proved, to remove

			/* enhancement: keep track of current node (next to develop), instead of 
			* looking for it at each iteration, restarting from root.
			*/
			PnNode current_node = root, most_proving_node;
			while(!root.isProved() && !isTimeEnded()) {

				// debug
				log = "visit no. " + visit_loops_n + " for player " + current_player + "\n";
				debugVisit(0, null);

				most_proving_node = selectMostProving(current_node);

				// debug
				log += "most_proving col: " + ((most_proving_node == root) ? -1 : most_proving_node.lastMoveFromFirstParent()) + ", isroot: " + (most_proving_node == root) + "\n";
				debugVisit(1, most_proving_node);
				
				developNode(most_proving_node, nodes_proved, current_player);
				
				// debug
				debugVisit(2, most_proving_node);
				
				current_node = updateAncestorsWhileChanged(most_proving_node, nodes_proved, current_player, board.game_state, null);

				// debug
				log += "update ancestors end; now resetBoard, current_node==null?" + (current_node == null) + "\n";
				depth_last = getRelativeDepth(most_proving_node);
				debugVisit(3, most_proving_node);
				if(isTimeEnded()) {
					System.out.println("last most_proving:");
					board.print();
				}
				
				/* at this point of the loop, the most_proving_node was not-expanded, and now just got expanded,
				so its numbers always change in `updateAcestors`, except when, by chance, initialization numbers were the
				same as the newly calculated ones. */
				if(current_node == null) current_node = most_proving_node;
				resetBoard(most_proving_node, current_node);

				// debug
				log += "resetBoard end, now clearProved. size " + nodes_proved.size() + "\n";
				
				clearProvedNodes(nodes_proved);
				
				// debug
				log += "clearProved end\n";
				log += root.debugString(root) + "\n";
				debugVisit(4, most_proving_node);
				visit_loops_n++;
				
			}

			// debug
			System.out.println("TIME at end of loop: " + (System.currentTimeMillis() - timer_start) );
			System.out.println("depth last, n_loops: " + depth_last + " " + visit_loops_n );
			System.out.println("TIMES: before selectMostProving:\t" + time_last[0] + "\nbefore develop: " + time_last[1] + "\nbefore ancestors: " + time_last[2] + "\nbefore reset" + time_last[3] + "\nafter reset: " + time_last[4]);
			System.out.println("MEMS:\nbefore selectMostProving:\t" + mem_last[0].toString() + "\nbefore develop: " + mem_last[1].toString() + "\nbefore ancestors: " + mem_last[2].toString() + "\nbefore reset" + mem_last[3].toString() + "\nafter reset: " + mem_last[4].toString());
			System.out.println("TIMES:\nin develop: before eval:\t" + time_last_a[0] + "\nbefore genchildren: " + time_last_a[1] + "\nafter genchildren: " + time_last_a[2]);
			log += "end of loop\n";
			root.debug(root);

			resetBoard(current_node, root);
		}

		/**
		 * Complexity:
		 * 	-	endgame: O(1)
		 * 	-	else: O(DbSearch)
		 * @param node
		 * @param game_state
		 * @param player who has to move in node's board.
		 * @return true if evaluated the node, i.e. it's an ended state or db found a win sequence.
		 */
		protected boolean evaluate(PnNode node, byte game_state, byte player) throws IOException {

			// debug
			log += "evaluate\n";
		
			if(game_state == GameState.OPEN) {
				TTElementProved evaluation = getEntryProved(board.hash_proved, TTElementProved.COL_NULL, node.depth, player);

				if(evaluation != null)
					return true;
				else
					return evaluateDb(node, player);
			}
			else {
				// debug
				log += "eval: game_state!=open\n";
				
				node.prove(game_state == GameState.WINP1);		// root cant be ended, or the game would be ended
				return true;
			}
		}

		/**
		 * Evaluate `node` according to a DbSearch.
		 * Complexity: O(DbSearch)
		 * @param node
		 * @param player
		 * @return true if found a win.
		 */
		protected boolean evaluateDb(PnNode node, byte player) throws IOException {

			log += "evaluateDb\n";

			DbSearchResult eval = dbSearch.selectColumn(board, node, timer_start + timer_duration - System.currentTimeMillis(), player, Operators.MAX_TIER);
			
			if(eval == null)
				return false;

			node.prove(player == MY_PLAYER);
			addEntryProved(board.hash_proved, node.depth, node.depth + (eval.threats_n * 2 + 1), player == MY_PLAYER, eval.winning_col);

			// debug
			if(DEBUG_PROVED) file_proved.write("proved db, hash:" + board.hash_proved +"\n");

			/* Heuristic: update parent's children with iterated related squares.
			 * If, in the current board, the current player has a winning sequence,
			 * starting with a certain move `x` in column `X` involving certain cells `s` (thus certain columns `S`),
			 * if the other player (who moved in the parent node) was to make a move not in any of `S`,
			 * then the current player could play `x`, and apply his winning sequence as planned,
			 * because the opponent's move is useless for such sequence.
			 *
			 * As an additional proof, if current player could create a new threat or avoid the opponent's one with 
			 * such different move, then `s` wouldn't represent a winning sequence (Db also checks defenses).
			 * 
			 * Probably that's already taken in count in parent's null-move dbSearch (generateAllChildren).
			 */

			return true;
		}


		/**
		 * set proof and disproof; update node.mostProving; in case insert in TTwon.
		 * Complexity: 
		 * 	-	case proved: O(1)
		 * 	-	case expanded: O(2N)
		 * 	-	else: O(1)
		 * @param node
		 * @param player
		 * @param proved
		 * @param offset offset for initProofAndDisproofNumbers, in case of `node` open and not expanded.
		 */
		protected void setProofAndDisproofNumbers(PnNode node, HashSet<PnNode> proved, byte player, byte game_state, int offset) throws IOException {
			/* TTproved management:
				if game_state != OPEN: just put depth, col=last move, won=game_state
				if my turn: if won choose a winning node (depth = its depth, col = col to get there), else search the deepest one
				else: if (I) won it means any moves makes me win, so could choose any (or maybe the deepest in the opponent's interest, but i'd still win);
				for algo purpose, don't leave empty and choose any; otherwise, choose the least deep which wins for him (so tries not to choose a draw)
			*/

			log += "setProof for " + board.hash_proved + "\t,numbers " + node.n[0] + " " + node.n[1] + "\n";

			TTElementProved entry_proved = getEntryProved(board.hash_proved, TTElementProved.COL_NULL, node.depth, player);
			if(entry_proved != null) {
				// debug
				log += "setProof: entry_proved;node==root:" + (node==root) + "; move: " + ((node == root) ? -1 : node.lastMoveFromFirstParent()) + "\n";

				proved.add(node);
				setDeepestProvedDescendant(entry_proved, board.hash_proved, player);
			}
			else if(game_state != GameState.OPEN) {
				// debug
				log += "setProof: game!open;node==root:" + (node==root) + "; move: " + ((node == root) ? -1 : node.lastMoveFromFirstParent()) + " ;hash:" + board.hash_proved + "\n";

				proved.add(node);
				node.prove(game_state == MY_WIN);
				addEntryProved(board.hash_proved, node.depth, (game_state == GameState.DRAW) ? node.depth + 1 : node.depth, game_state == MY_WIN, -1);	// from a leaf it takes 0 moves to get to final depth
			}
			// if node has children, set numbers according to children numbers.
			else if(node.isExpanded())
			{
				// debug
				log += "setProof: node expanded;node==root:" + (node==root) + "; move: " + ((node == root) ? -1 : node.lastMoveFromFirstParent()) + "\n";
				
				// node can be already proved here bc of dag: in that case you could need to update its best move
				if(!node.isProved()) {

					if(player == CellState.P1) {
						node.most_proving = node.minChild(PROOF);
						node.setProofAndDisproof(node.most_proving.n[PROOF], node.sumChildren(DISPROOF));
					} else {
						node.most_proving = node.minChild(DISPROOF);
						node.setProofAndDisproof(node.sumChildren(PROOF), node.most_proving.n[DISPROOF]);
					}
				}

				// if proved, manage TT
				if(node.isProved()) {
					
					// debug
					log += "setProof: node proved;node==root:" + (node==root) + "; move: " + ((node == root) ? -1 : node.lastMoveFromFirstParent()) + " ;hash:" + board.hash_proved + "\n";
					if(DEBUG_PROVED) file_proved.write("setProof: node proved;node==root:" + (node==root) + "; move: " + ((node == root) ? -1 : node.lastMoveFromFirstParent()) + " ;hash:" + board.hash_proved + "\tmost proving:"+node.most_proving.hash_dag+"\n");
					
					node.prove(node.n[DISPROOF] > 0);
				 	entry_proved = addEntryProved(board.hash_proved, node.depth, node.depth, node.n[DISPROOF] > 0, TTElementProved.COL_NULL);
					proved.add(node);

					setDeepestProvedDescendant(entry_proved, board.hash_proved, player);
				}
				
				// debug
				log += "setProof node expanded end\n";
			}
			else {
				initProofAndDisproofNumbers(node, offset);
			}

			
		}

		/**
		 * Complexity: O(h + 4X), where h is the length of the path calling_node-most_proving_descndant
		 * @param node
		 * @return
		 */
		private PnNode selectMostProving(PnNode node) {
			
			log += "selectMostProving\t" + board.hash_proved + "\n";

			if(!node.isExpanded()) return node;
			else {
				markCheck(node.lastMoveForChild(node.most_proving));
				/* move `node` as first parent for most_proving (implementation detail useful when going back up in the tree,
				using `updateAncestors`) */
				node.most_proving.parents.remove(node);
				node.most_proving.parents.addFirst(node);

				return selectMostProving(node.most_proving);
			}
			// node.most_proving should always be != null

		}

		/**
		 * Complexity: 
		 * 		for alpha: O(2DbSearch + 13N**2)
		 * @param node
		 */
		private void developNode(PnNode node, HashSet<PnNode> nodes_proved, byte player) throws IOException {

			// debug
			log += "developNode\n";
			time_last_a[0] = System.currentTimeMillis() - timer_start;
			
			if(evaluate(node, board.game_state, player))
				return;

			// debug
			time_last_a[1] = System.currentTimeMillis() - timer_start;
			
			// if the game is still open
			generateAllChildren(node, nodes_proved, player);

			// debug
			time_last_a[2] = System.currentTimeMillis() - timer_start;

		}

		
		/**
		 * Complexity: O(DbSearch + getThreatCounts + children_generation + sorting + shuffle )
		 *	<p>	= O(DbSearch + 12N**2 + N 4X + N**2 + N )
		 *	<p>	= O(DbSearch + 13N**2)
		 *	<p>		note: setProofAndDisproofNumbers can't go in expanded case from here, so it's O(1)
		 * @param node
		 * @param threat_scores_by_col
		 */
		public void generateAllChildren(PnNode node, HashSet<PnNode> nodes_proved, byte player) throws IOException {

			// debug
			log += "generateChildren\n";
			if(DEBUG_PROVED) file_proved.write("genChildren for " + board.hash_proved + "\t,level " + node.depth + "\n");

			/* Heuristic: implicit threat.
			 * Only inspect moves in an implicit threat, i.e. a sequence by which the opponent could win
			 * if the current player was to make a "null move".
			 * In fact, the opponent could apply such winning sequence, if the current player was to 
			 * make a move outside it, thus not changing his plans.
			 * 
			 * Actually, no moves are removed, but these results are only used to sort them.
			 * 
			 * Applied to CXGame: columns where the opponent has an immediate attacking move - which leads to a win for him -,
			 * i.e. where the attacker's move corresponds to the first free cell in the column, are for sure
			 * the most interesting (in fact, you would lose not facing them); however, other columns involved in the sequence are
			 * not ignored, since they could block the win too, and also to simplify the calculations by approximation.
			 * 
			 * note: related_cols should already contain only available, not full, columns.
			 */

			// DbSearchResult res_db = dbSearch.selectColumn(board, node, timer_start + timer_duration - System.currentTimeMillis(), Auxiliary.opponent(player), Operators.MAX_TIER);

			/* Heuristic: sorting moves (previously selected from iterated related squares) by number/scores of own threats in them
			 * (i.e., for columns, the sum of the scores in the whole column).
			 */

			int		available_cols_n = board.N;
			int[]	col_scores = new int[board.N],
					threats = dbSearch.getThreatCounts(board, player);
			int current_child, j, k;

			// get for each column the score from db, and check if they are free
			boolean won = (player != MY_PLAYER);
			for(j = 0; j < board.N; j++) {
				//if(res_db != null && res_db.related_squares_by_col[j] > 0) col_scores[j] = res_db.related_squares_by_col[j];
				if( board.freeCol(j) ) {
					TTElementProved entry = getEntryProved(board.hash_proved, j, node.depth, player);
					if(entry == null)
						col_scores[j] = 1;
					else {
						available_cols_n--;
						if(entry.won() == (player == MY_PLAYER)) {
							won = (player == MY_PLAYER);
							available_cols_n = 0;
							break;
						}
					}
				}
				else available_cols_n--;
			}

			/* Check if the found child nodes can prove this node.
			Otherwise, note that the found nodes won't contribute to this node's numbers. */
			if(available_cols_n == 0) {
				// debug
				if(DEBUG_PROVED) file_proved.write("genChildren proved " + board.hash_proved + " " + ((node==root)?-1:node.lastMoveFromFirstParent()) +"\n");

				addEntryProved(board.hash_proved, node.depth, node.depth, won, TTElementProved.COL_NULL);
				node.prove(won);
				return;
			}
			
			node.expand(available_cols_n);

			/* Fill children with such columns, and sort by threat_scores at the same time.
			 * This is made in two steps: first fill and shuffle moves, then assign parent.children related to those shuffled moves.
			 */
			for(j = 0, current_child = 0; j < board.N && current_child < available_cols_n; j++) {
				if(col_scores[j] > 0)
				{
					node.cols[current_child++] = (byte)j;
					// move back the new child in the right place
					for(k = current_child - 1; (k > 0) && (threats[node.cols[k - 1]] > threats[j]); k--)
					Auxiliary.swapByte(node.cols, k, k - 1);
				}
			}

			// shuffle children with same priority
			int start, end;
			for(start = 0; start < available_cols_n; start++) {
				for(end = start + 1;
					end < available_cols_n && threats[node.cols[end]] == threats[node.cols[start]];
					end++
				) ;
				Auxiliary.shuffleArrayRangeByte(node.cols, start, end);
				start = end;
			}
				
			// now create the children
			for(current_child = 0; current_child < available_cols_n; current_child++)
			{
				j = node.cols[current_child];
				markCheck(j);

				
				TTElementNode entry = TTdag.get(TTElementNode.setKey(TTdag_key, board.hash_dag, node.depth + 1));
				if(entry == null) {
					node.createChild(current_child, j, board.hash_dag);
					/* Heuristic initialization: nodes without any threat should be considered less (or not at all).
					*/
					setProofAndDisproofNumbers(node.children[current_child], nodes_proved, Auxiliary.opponent(player), board.game_state, (threats[j] == 0) ? (board.N * 2 + 1) : (board.N + 1));
					if(!node.children[current_child].isProved()) {
						TTdag.insert(board.hash_dag, new TTElementNode(board.hash_dag, node.depth + 1, node.children[current_child]));

						// debug
						dag_n++;
					}
				} else {
					// debug
					if(DEBUG_PROVED) file_proved.write("found in dag:" + board.hash_proved + "\t" + entry.node.n[0] + "\t" + entry.node.n[1] + "\t" + entry.node.depth + "\t" + entry.node.parents.size() + "\t" + ((entry.node.children == null)?-1:entry.node.children.length) + "\n");
					
					node.addChild(current_child, entry.node, j);
				}

				// debug
				if(DEBUG_PROVED) file_proved.write("child " + node.children[current_child].hash_dag + "\n");

				unmark(j);
			}

		}

		/**
		 * Complexity:
		 * 		worst: O(2Nh)
		 * 			note: setProofAndDisproofNumbers always is called in expanded case, in intermediate nodes.
		 * 		avg: O(Nh) ?
		 * @param node
		 * @param game_state init to `board.game_state`
		 * @return
		 */
		public PnNode updateAncestorsWhileChanged(PnNode node, HashSet<PnNode> nodes_proved, byte player, byte game_state, PnNode caller) throws IOException {
			
			log += "updateAncestors-loop: node with col " + ((node == root) ? -1 : node.lastMoveFromFirstParent()) + ", numbers " + node.n[PROOF] + " " + node.n[DISPROOF] + ", isroot " + (node==root) + ";;level=" + getRelativeDepth(node) + "\n";
			
			int old_proof = node.n[PROOF], old_disproof = node.n[DISPROOF];
			setProofAndDisproofNumbers(node, nodes_proved, player, game_state, 0);		// offset useless, node always expanded here 

			// debug
			if(DEBUG_PROVED) file_proved.write("updateAncestors-loop: node " + board.hash_proved +"\twith col " + ((node == root) ? -1 : node.lastMoveFromFirstParent()) + "\t,numbers " + node.n[0]+" "+node.n[1]+" old:"+old_proof+" "+old_disproof+ ", isroot " + (node==root) + ";;level=" + getRelativeDepth(node) + "\n");

			// if changed
			if(old_proof != node.n[PROOF] || old_disproof != node.n[DISPROOF] || node.isProved()) {
				
				if(getRelativeDepth(node) > 0)
				{
					int marked_col;
					PnNode last_changed = null;
					for(PnNode parent : node.parents) {
						marked_col = parent.lastMoveForChild(node);
						unmark(marked_col);
						/* the last parent used to go down the tree was put as first parent;
						* however other parents and their ancestors still need to be updated.
						*/
						if(parent == node.parents.getFirst()) last_changed = updateAncestorsWhileChanged(parent, nodes_proved, Auxiliary.opponent(player), GameState.OPEN, node);
						else updateAncestorsWhileChanged(parent, nodes_proved, Auxiliary.opponent(player), GameState.OPEN, node);
						markOnly(marked_col);
					}

					if(last_changed != null) return last_changed;
					else return node;
				}
				else
					return node;
			}
			else
				return null;
		}

		/**
		 * Complexity: O(h)
		 * @param current
		 * @param base where to reset to
		 */
		private void resetBoard(PnNode current, PnNode base) {
			while(current != base) {

				// debug
				log += "resetBoard, depth " + getRelativeDepth(current) + "\n";
				
				unmark(current.lastMoveFromFirstParent());
				// any path goes back to root
				current = current.parents.getFirst();
			}
		}

		/**
		 * Remove any reference to `node`, and also to its descendants if they only had one parent.
		 * @param node
		 * @param clear_parents init to true
		 */
		private void _clearProvedTree(PnNode node, boolean clear_parents) throws IOException {

			// debug
			//log += "_clearProved: col ";
			//if(node.parents != null && node.parents.size() > 0)
			//	log += node.lastMoveFromFirstParent();
			//log += ", isroot: " + (node == root) + "parents=null,children=null" + (node.parents == null) + " " + (node.children == null) + "\n";
			if(TTdag.get(TTElementNode.setKey(TTdag_key, node.hash_dag, node.depth)) != null)
				dag_n--;

			TTdag.remove(TTElementNode.setKey(TTdag_key, node.hash_dag, node.depth));
			if(clear_parents && (node.parents != null) ) {
				for(PnNode parent : node.parents) {
					if(DEBUG_PROVED) {
						file_proved.write("clearProvedTree: node " + node.lastMoveFromFirstParent() + ",parent " + ((node == root) ? -1 : parent.lastMoveFromFirstParent()) + ",depth node:" + node.depth +"\n");
						file_proved.write("parent children:" + ((parent.children == null) ? -1 : parent.children.length) + ",numbers:" + node.n[0] + " "+node.n[1]  + ";"+parent.n[0]+" "+parent.n[1] +"\n");
						file_proved.write("hash: node=" + node.hash_dag +"\tparent=" + parent.hash_dag +"\n");
					}
					parent.removeChild(node);
				}
				node.parents.clear();
			}
			if(node.children != null) {
				for(PnNode child : node.children) {
					child.parents.remove(node);
					if(child.parents.size() == 0)
						_clearProvedTree(child, false);
				}
				node.children = null;	// to avoid repeating iteration, called by `clearProvedNodes()`
			}
		}
		/**
		 * 
		 * @param nodes_proved
		 */
		private void clearProvedNodes(HashSet<PnNode> nodes_proved) throws IOException {

			for(PnNode node : nodes_proved) {
				_clearProvedTree(node, true);
			}
			nodes_proved.clear();
		}


	//#endregion PN-SEARCH


	//#region AUXILIARY

		/**
		 * Init proof numbers to offset + current level in game tree.
		 * <p>
		 * Complexity: O(1)
		 * @param node
		 */
		protected void initProofAndDisproofNumbers(PnNode node, int offset) {
			int number = offset + getRelativeDepth(node) / 2 + 1;		// never less than 1
			node.setProofAndDisproof(number, number);
		}

		private void setDeepestProvedDescendant(TTElementProved entry, long hash, byte player) throws IOException {

			// debug
			if(DEBUG_PROVED) file_proved.write("setDeepest for " + hash + " " + entry.won() + "\tplayer " + player + "\n");
			String s = "";
			for(int j = 0; j < board.N; j++) {
				if(board.freeCol(j)) {
					TTElementProved entry_child = getEntryProved(hash, j, entry.depth_cur, player);
					if(entry_child != null) {
						s +=  "won " + entry_child.won() + " col " + entry_child.col() + " depth " + entry_child.depth_cur + " depth_max " + entry_child.depth_reachable +  "\n";
						if(DEBUG_PROVED) file_proved.write("setDeepest: won " + entry_child.won() + "\t,col " + entry_child.col() + "\t,depth " + entry_child.depth_cur + " depth_max " + entry_child.depth_reachable +  "\n");
					} else {
						if(DEBUG_PROVED) file_proved.write("setDeepest: " + TTproved.getHash(hash, board.free[j], j, Auxiliary.getPlayerBit(player)) + "\t" + board.free[j] + " " + j  + "\n");

					}
				}
			}
			if(!s.equals(""))
				if(DEBUG_PROVED) file_proved.write( "setDeepestProved, for " + hash + " " + entry.won() + " " + entry.col() + " " + entry.depth_reachable + " " + entry.depth_cur +  "\n" + s + "\n");
			
			int best_col = entry.col(), best_depth = entry.depth_reachable;
			TTElementProved entry_child;
			for(int j = 0; j < board.N; j++) {
				if(board.freeCol(j)) {
					entry_child = getEntryProved(hash, j, entry.depth_cur, player);
					if(entry_child != null && entry_child.won() == entry.won()
						&& (best_col == TTElementProved.COL_NULL || (entry_child.depth_reachable < best_depth == (entry.won() == (player == MY_PLAYER)) ) )
					) {
						// search deepest if winning for current `player`, otherwise least deep
						best_col = j;
						best_depth = entry_child.depth_reachable;
					}
				}
			}
			if(best_col != TTElementProved.COL_NULL) {
				entry.setCol(best_col);
				entry.depth_reachable = (short)best_depth;
			}
		}

		/**
		 * mark/unmark and update current_player.
		 * <p>  Complexity: O(4X)
		 * @param col : col to mark/unmark
		 * @return new game_state
		 */
		protected byte markCheck(int col) {
			byte res = board.markCheck(col, current_player);
			current_player = (byte)Constants.opponent(current_player);
			return res;
		}
		/**
		 * mark, no check.
		 * <p> Complexity: O(1)
		 * @param col
		 * @return
		 */
		protected void markOnly(int col) {
			board.mark(col, current_player);
			current_player = (byte)Constants.opponent(current_player);
		}
		/**
		 * Complexity: O(1)
		 * @param col
		 */
		protected void unmark(int col) {
			board.unmark(col);
			current_player = (byte)Constants.opponent(current_player);
		}
		
		/**
		 * Complexity: worst: O(N)
		 * @return
		 */
		protected PnNode bestNode() {

			if(root.children == null)
				return null;

			// child with min proof/disproof ratio
			PnNode best = null;
			for(PnNode child : root.children) {
				if(child.n[DISPROOF] != 0 && (best == null || (float)child.n[PROOF] / child.n[DISPROOF] < (float)best.n[PROOF] / best.n[DISPROOF]) )
					best = child;
			}

			return best;
		}
		/**
		 * get the deepest descendant node from root (init node to root) (need another paramter to work with
		 * other nodes and return `move_to_deepest`).
		 * @param node
		 * @return the deepest node, and set `move_to_deepest` with the first move to get there
		 */
		private PnNode deepestNode(PnNode node) {
			if(!node.isExpanded())
				return node;
			else {
				PnNode deepest = node, childs_deepest;
				int max_depth = node.depth;
				for(int i = 0; i < node.children.length; i++) {
					childs_deepest = deepestNode(node.children[i]);
					if(deepest == null || childs_deepest.depth > max_depth) {
						deepest = childs_deepest;
						if(node == root) move_to_deepest = node.cols[i];
					}
				}
				return deepest;
			}
		}

		/**
		 * Unlink from parents/children all nodes with different tag from `depth_root`,
		 * and remove from TT; unless proved (or evaluated in db).
		 * @param node
		 * @param hash
		 */
		private void removeUnmarkedTree(PnNode node, long hash, byte player) {
			if(node.tag == root.tag)
				return;
			else {
				TTdag.remove(TTElementNode.setKey(TTdag_key, hash, node.depth));
				/* each node should unlink each child, because of dag structure;
				however it's not necessary to unlink parents: if a node was to have a marked parent,
				then such node would be marked too. */
				if(node.children != null) {
					for(int i = 0; i < node.children.length; i++) {
						node.children[i].parents.remove(node);
						lastIt_freeCols[node.cols[i]]++;
						removeUnmarkedTree(node.children[i], TTdag.getHash(hash, lastIt_freeCols[node.cols[i]] - 1, node.cols[i], Auxiliary.getPlayerBit(player)), Auxiliary.opponent(player));
						lastIt_freeCols[node.cols[i]]--;
					}
					node.children = null;
				}
			}
		}
		
		/**
		 * Calculate player from node depth.
		 * @param depth
		 * @return
		 */
		private byte getPlayerFromDepth(int depth) {
			return ((depth % 2 == 0) == first) ? MY_PLAYER : YOUR_PLAYER;
		}
		private int getRelativeDepth(PnNode node) {
			return node.depth - root.depth;
		}
		/**
		 * Get entry from TTproved.
		 * @param hash node's hash
		 * @param col set to col for node's child if want entry for child, else TTElementProved.COL_NULL
		 * @param depth node's depth
		 * @param player current player at node (in case, who's making move col)
		 * @return entry
		 */
		private TTElementProved getEntryProved(long hash, int col, int depth, byte player) {
			if(col == TTElementProved.COL_NULL)
				return TTproved.get(TTElementProved.setKey(TTproved_key, hash, depth));
			else
				return TTproved.get(TTElementProved.setKey(TTproved_key, TTproved.getHash(hash, board.free[col], col, Auxiliary.getPlayerBit(player)), depth + 1));
		}
		/**
		 * Insert entry in TTproved.
		 * @param hash
		 * @param col
		 * @param depth
		 * @param player
		 */
		private TTElementProved addEntryProved(long hash, int depth_cur, int depth_reachable, boolean won, int col) {
			// debug
			proved_n++;
			
			TTElementProved entry = new TTElementProved(hash, depth_cur, depth_reachable, won, col);
			TTproved.insert(hash, entry);
			return entry;
		}
		
		/**
		 * Complexity: O(1)
		 * @return true if it's time to end the turn
		 */
		private boolean isTimeEnded() {
			return (System.currentTimeMillis() - timer_start) >= timer_duration;
		}

		/**
		 * @return true if available memory is less than a small percentage of max memory
		 */
		protected boolean isMemoryEnded() {
			// max memory useable by jvm - (allocatedMemory = memory actually allocated by system for jvm - free memory in totalMemory)
			long freeMemory = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
			return freeMemory < runtime.maxMemory() * (5 / 100);
		}

		protected void printTime() {
			ms = System.currentTimeMillis() - ms;
			if(ms > 0) {
				System.out.println("pn, turn " + visit_loops_n + ", time select most proving: " + ms);
				System.out.println("..." + log + "\n...");
			}
			ms = System.currentTimeMillis();
		}

		private void debugVisit(int idx, PnNode most_proving_node) {
			if(DEBUG_TIME) printTime();
			ms = System.currentTimeMillis();
			time_last[idx] = System.currentTimeMillis() - timer_start;
			mem_last[idx].set(runtime.maxMemory(), runtime.totalMemory(), runtime.freeMemory(), Auxiliary.freeMemory(runtime));
		}

	//#endregion AUXILIARY

}