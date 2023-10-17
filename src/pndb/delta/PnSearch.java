package pndb.delta;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;

import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXPlayer;
import pndb.delta.constants.Auxiliary;
import pndb.delta.constants.CellState;
import pndb.delta.constants.GameState;
import pndb.delta.tt.TranspositionTable;
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
 * <p>	15.	(delta) PnTTnode: more parents (dag)
 * <p>	16. (delta) no prune, keep nodes for next visits (using tt for each node)
 * <p>	17.	M*N < 2**15, because of short type for numbers
 * <p>	18.	dbSearch: note about wins by mistake, with threats of tier > 1 (intersecting with tier 1 ones)
 * <p>	19. why deleting previous nodes: i guess if you managed to prove a node from a previous position, which was
 * 			more generical, you'll probably be able to re-do it now, with less nodes.
 * <p>	20.	TT.remove at start of each selectColumn, to remove entries from previous rounds
 * <p>	21.	each node is inserted in dag at creation time
 * <p>	22.	in list nodes to remove, same node could be put more times: so use set
 * <p>	23.	PnTTnodes need children as array/list, bc of dag (each node has its own parents/children)
 * <p>	24.	updateAncestors could start updating proved nodes unreachable from root
 * <p>	25.	a node, if existing, is always either in tt dag or proved.
 * <p>	26. had to remove current_node enhancement: with dag it's very expensive keeping track of last updated, while selectMostProving isn't actually very expensive.
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
 * .metti `age` in TTproved, così se ti serve deepestNode alla fine di selectCol (se perso), se un child ha age vecchia vuol dire che potresti dover
 * ricalcolare il deepest, ricorsivamente.
 * .depth_reachable in pnttnode
 * .make proved db node add proved child? so when other parents find it in dag, its proved already
 * 
 * .errorr cono
 * .remove dag clean
 * 
 * check there are not problems with def.length=0 with new operator
 * 
 */
public class PnSearch implements CXPlayer {

	//#region CONSTANTS
	protected static final byte PROOF		= PnTTnode.PROOF;
	protected static final byte DISPROOF	= PnTTnode.DISPROOF;

	protected byte MY_PLAYER	= CellState.P1;
	protected byte YOUR_PLAYER	= CellState.P2;
	protected byte MY_WIN		= GameState.WINP1;
	protected byte YOUR_WIN		= GameState.WINP2;

	//#endregion CONSTANTS

	// board
	public BoardBitPn board;			// public for debug
	protected TranspositionTable<PnTTnode, PnTTnode.KeyDepth> TTdag;
	protected TranspositionTable<TTElementProved, TTElementProved.KeyDepth> TTproved;
	protected DbSearch dbSearch;
	
	// nodes
	protected PnTTnode root;
	
	// time / memory
	protected long timer_start;			//turn start (milliseconds)
	protected long timer_duration;		//time (millisecs) at which to stop timer
	protected Runtime runtime;
	
	// implementation
	private PnTTnode.KeyDepth key_dag;
	private KeyDepth TTproved_key;
	protected PnTTnode		lastIt_root;
	protected BoardBitPn	lastIt_board;

	// debug
	private final boolean DEBUG_PROVED	= true;
	private final boolean DEBUG_TIME	= false;
	private final boolean DEBUG_LOG		= false;
	protected String log;
	private long ms;
	private int visit_loops_n;
	private long depth_last;
	private long[] time_last = new long[5];
	private testerMemoryStruct[] mem_last = new testerMemoryStruct[5];
	private long[] time_last_a = new long[3];
	private PnTTnode node_last_db;
	private int file_rand;
	private FileWriter file_proved;
	private int created_n;

	
	

	public PnSearch() {}
	
	/**
	 * Complexity: O(5N + 4MN + 2**16 + (5MN + 3M+4N + 2**16) ) = O(9MN + 3M+9N + 2**17)
	 */
	@Override
	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {

		BoardBit.M = (byte)M;
		BoardBit.N = (byte)N;
		BoardBit.X = (byte)X;
		board		= new BoardBitPn(first ? MY_PLAYER : YOUR_PLAYER);
		dbSearch	= new DbSearch();		
		TTdag		= new TranspositionTable<PnTTnode, PnTTnode.KeyDepth>(M, N, PnTTnode.getTable());
		TTproved	= new TranspositionTable<TTElementProved, KeyDepth>(M, N, TTElementProved.getTable());

		BoardBitPn.TTdag 	= TTdag;
		BoardBitPn.TTproved	= TTproved;
		key_dag				= new PnTTnode.KeyDepth();
		TTproved_key		= new TTElementProved.KeyDepth();
		PnTTnode.board		= board;
		PnTTnode.TTdag		= TTdag;
		PnTTnode.TTproved	= TTproved;

		timer_duration = (timeout_in_secs - 1) * 1000;
		runtime = Runtime.getRuntime();
		lastIt_board = new BoardBitPn(first ? MY_PLAYER : YOUR_PLAYER);

		// dbSearch instantiated by subclass
		dbSearch.init(M, N, X, first);

		// debug
		for(int i = 0; i < mem_last.length; i++) mem_last[i] = new testerMemoryStruct(0,0,0,0);
		System.out.println("\n-_-\nSTART GAME\n-_-\n");
		file_rand = new Random().nextInt();
		System.out.println("RAND HASH:"+file_rand);
		created_n = 0;
		
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
				board.markCheck(B.getLastMove().j);

			// debug
			if(DEBUG_PROVED) file_proved = new FileWriter("debug/pn/"+ file_rand +"proved" + MC.length + ".txt");
			System.out.println("---\n" + playerName());
			System.out.println("Opponent: " + ((B.getLastMove() == null) ? null : B.getLastMove().j) );
			System.out.println("root existed in dag: " + (TTdag.get(PnTTnode.setKey(key_dag, board.hash, MC.length)) != null) );
				
			// see if new root was already visited, otherwise create it
			root = TTdag.get(PnTTnode.setKey(key_dag, board.hash, MC.length));
			if(root == null) {
				root = new PnTTnode(board.hash, (short)MC.length, (MC.length / 2) % 2 );
				root.setProofAndDisproof(1, 1);

				created_n++;
			}

			// debug
			System.out.println("root hash:" + board.hash + "\tdepth " + root.depth );
			board.print();

			// remove unreachable nodes from previous rounds
			root.setTag((root.depth / 2) % 2);		// unique tag for each round
			if(lastIt_root != null) {
				// debug
				System.out.println("time before tagTree: " + (System.currentTimeMillis() - timer_start) );
				
				root.tagTree();
				// debug
				System.out.println("time before removeUnmarkedTree: " + (System.currentTimeMillis() - timer_start) );

				PnTTnode.board = lastIt_board;
				lastIt_root.removeUnmarkedTree(root.getTag());
				PnTTnode.board = board;
			}

			// debug
			testerMemoryStruct mem = new testerMemoryStruct(runtime.maxMemory(), runtime.totalMemory(), runtime.freeMemory(), Auxiliary.freeMemory(runtime));
			System.out.println("time,mem before gc: " + (System.currentTimeMillis() - timer_start) + "\t" + mem);
			
			runtime.gc();
			
			// debug
			mem.set(runtime.maxMemory(), runtime.totalMemory(), runtime.freeMemory(), Auxiliary.freeMemory(runtime));
			System.out.println("time,mem before start visit: " + (System.currentTimeMillis() - timer_start) + "\t" + mem);
			
			// visit
			TTElementProved root_eval = getEntryProved(board.hash, TTElementProved.COL_NULL, root.depth, board.player);
			if(root_eval == null) {
				visit();
				root_eval = getEntryProved(board.hash, TTElementProved.COL_NULL, root.depth, board.player);
			}

			
			// debug
			if(DEBUG_PROVED) file_proved.close();
			log += "ended visit\nproved_n = " + TTdag.count + "\tdag_n = " + TTproved.count + "\tcreated_n = " + created_n + "\n";
			log += "root_eval=null:" + (root_eval==null) + "\n";
			if(root_eval != null) {
				log += "depths " + root.depth + " " + root_eval.depth_cur + "\n";
				log += root_eval.col() + " "+root_eval.won()+" " +root_eval.depth_reachable + "\n";
			}
			System.out.println("TIME before lastIt_root/board: " + (System.currentTimeMillis() - timer_start) );
			
			
			lastIt_root	 = root;
			lastIt_board.copy(board);
			
			// debug
			System.out.println("TIME before select move: " + (System.currentTimeMillis() - timer_start) );

			int move;
			if(root_eval != null)
				move = root_eval.col();
			else {
				move = root.getMoveToBestChild();
			}
			board.markCheck(move);

			// debug
			System.out.println("TIME before return: " + (System.currentTimeMillis() - timer_start) );
			log += "before debug&return\n";
			log += root.debugString(root);
			log += "\nMy move: " + move + "\n";
			log += board.printString(0);
			log += "time,mem before return (last): " + (System.currentTimeMillis() - timer_start) + " " + Auxiliary.freeMemory(runtime) + "\n";
			System.out.println("\nLOG:\n" + log);
			System.out.println("TIME before return: " + (System.currentTimeMillis() - timer_start) );
			
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

			LinkedList<Integer> marked				= new LinkedList<Integer>();
			LinkedList<BoardBitPn> boards_to_prune	= new LinkedList<BoardBitPn>();

			/* enhancement: keep track of current node (next to develop), instead of 
			* looking for it at each iteration, restarting from root.
			*/
			PnTTnode most_proving_node;
			while(!root.isProved() && !isTimeEnded()) {

				// debug
				log = "visit no. " + visit_loops_n + " for player " + board.player + "\n";
				debugVisit(0, null);

				most_proving_node = selectMostProving(root, marked);

				// debug
				log += "most proving level " + most_proving_node.depth + ", isroot: " + (most_proving_node == root) + "\n";
				debugVisit(1, most_proving_node);
				
				developNode(most_proving_node);
				
				// debug
				debugVisit(2, most_proving_node);
				
				updateAncestorsWhileChanged(most_proving_node.depth, boards_to_prune, null, true);

				// debug
				log += "update ancestors end; now resetBoard\n";
				depth_last = getRelativeDepth(most_proving_node);
				debugVisit(3, most_proving_node);
				if(isTimeEnded()) {
					System.out.println("last most_proving:");
					board.print();
				}
				
				resetBoard(marked);

				// debug
				log += "resetBoard end; before prune\n";

				pruneTrees(boards_to_prune);
				
				// debug
				log += "pruning end\n";
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
			
			resetBoard(marked);

			// debug
			System.out.println("TIME at end of visit: " + (System.currentTimeMillis() - timer_start) );
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
		protected boolean evaluate(PnTTnode node) throws IOException {

			// debug
			log += "evaluate\n";
		
			if(board.game_state == GameState.OPEN) {
				TTElementProved evaluation = getEntryProved(board.hash, TTElementProved.COL_NULL, node.depth, board.player);

				if(evaluation != null)
					return true;
				else
					return evaluateDb(node, board.player);
			}
			else {
				// debug
				log += "eval: game_state!=open\n";
				if(DEBUG_PROVED) file_proved.write("state " + board.game_state + "\n");
				
				node.prove(board.game_state == MY_WIN);		// root cant be ended, or the game would be ended
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
		protected boolean evaluateDb(PnTTnode node, byte player) throws IOException {

			log += "evaluateDb\n";

			DbSearchResult eval = dbSearch.selectColumn(board, node, timer_start + timer_duration - System.currentTimeMillis(), player, Operators.MAX_TIER);
			
			if(eval == null)
				return false;

			node.prove(player == MY_PLAYER, (short)(node.depth + (eval.threats_n * 2 - 1)), eval.winning_col);

			// debug
			if(DEBUG_PROVED) file_proved.write("proved db\n");

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
		protected void setProofAndDisproofNumbers(PnTTnode node, int offset) throws IOException {
			/* TTproved management:
				if game_state != OPEN: just put depth, col=last move, won=game_state
				if my turn: if won choose a winning node (depth = its depth, col = col to get there), else search the deepest one
				else: if (I) won it means any moves makes me win, so could choose any (or maybe the deepest in the opponent's interest, but i'd still win);
				for algo purpose, don't leave empty and choose any; otherwise, choose the least deep which wins for him (so tries not to choose a draw)
			*/

			if(DEBUG_LOG) log += "setProof for " + board.hash + "\t " + node.depth + "\tn " + node.n[0] + "\t" + node.n[1] + "\n";
			
			if(board.game_state != GameState.OPEN) {
				// debug
				if(DEBUG_LOG) log += "state " + board.game_state + "\n";
				if(DEBUG_PROVED) file_proved.write("setProof for " + board.hash + "\t" + node.depth + "\tn " + node.n[0] + "\t" + node.n[1] + "\tstate " + board.game_state + "\n");

				node.prove(board.game_state == MY_WIN);
			}
			// if node has children, set numbers according to children numbers.
			else if(node.isExpanded())
			{
				// debug
				if(DEBUG_LOG) log += "expanded " + node.n[0] + "\t" + node.n[1] + "\tmost proving " +((node.most_proving_col==-1)?-1:TTdag.getHash(board.hash, board.free[node.most_proving_col], node.most_proving_col, Auxiliary.getPlayerBit(board.player))) + "\n";

				TTElementProved entry = node.updateProofAndDisproof(board.player == MY_PLAYER ? PROOF : DISPROOF);

				// debug
				if(DEBUG_LOG) log += "updated " + node.n[0] + "\t" + node.n[1] + "\tmost proving " +((node.most_proving_col==-1)?-1:TTdag.getHash(board.hash, board.free[node.most_proving_col], node.most_proving_col, Auxiliary.getPlayerBit(board.player))) + "\n";

				if(entry != null) {
					
					updateProved(entry);
					
					// debug
					if(DEBUG_LOG) log += "proved " + entry.won() + " " + entry.depth_reachable + " " + entry.col() + "\n";
					if(DEBUG_PROVED) file_proved.write("proved " + entry.won() + " " + entry.depth_reachable + " " + entry.col() + "\n");
				}
				
				// debug
				if(DEBUG_LOG) log += "setProof node expanded end\n";
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
		private PnTTnode selectMostProving(PnTTnode node, LinkedList<Integer> marked) throws IOException {
			
			log += "selectMostProving\t" + board.hash + "\n";
			if(DEBUG_PROVED) file_proved.write("selectMostProving\t" + board.hash);
			if(DEBUG_PROVED) file_proved.write("\tlevel " + node.depth + "\n");

			if(!node.isExpanded()) return node;
			else {
				board.markCheck(node.most_proving_col);
				/* move `node` as first parent for most_proving (implementation detail useful when going back up in the tree,
				using `updateAncestors`) */
				marked.push(Integer.valueOf(node.most_proving_col));

				return selectMostProving(getEntry(board.hash, TTElementProved.COL_NULL, node.depth + 1, board.player), marked);
			}
			// node.most_proving should always be != null

		}

		/**
		 * Complexity: 
		 * 		for alpha: O(2DbSearch + 13N**2)
		 * @param node
		 */
		private void developNode(PnTTnode node) throws IOException {

			// debug
			log += "develop " + board.hash + "\t" + node.depth + "\n";
			if(DEBUG_PROVED) file_proved.write("develop " + board.hash + "\t" + node.depth + "\n");
			time_last_a[0] = System.currentTimeMillis() - timer_start;
			
			if(evaluate(node))
				return;

			// debug
			time_last_a[1] = System.currentTimeMillis() - timer_start;
			
			// if the game is still open
			generateAllChildren(node);

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
		public void generateAllChildren(PnTTnode node) throws IOException {

			// debug
			log += "genChildren\n";
			if(DEBUG_PROVED) file_proved.write("genChildren\n");

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

			int		available_cols_n = BoardBit.N;
			int[]	col_scores = new int[BoardBit.N],
					threats = dbSearch.getThreatCounts(board, board.player);
			int current_child, j, k;

			// get for each column the score from db, and check if they are free
			boolean won = (board.player != MY_PLAYER);
			for(j = 0; j < BoardBit.N; j++) {
				//if(res_db != null && res_db.related_squares_by_col[j] > 0) col_scores[j] = res_db.related_squares_by_col[j];
				if( board.freeCol(j) ) {
					TTElementProved entry = getEntryProved(board.hash, j, node.depth, board.player);

					if(entry == null)
						col_scores[j] = 1;
					else {
						// debug
						if(DEBUG_PROVED) file_proved.write("found proved child " + TTdag.getHash(board.hash, board.free[j], j, Auxiliary.getPlayerBit(board.player)) + "\t" + entry.won() + "\n");
						
						available_cols_n--;
						if(entry.won() == (board.player == MY_PLAYER)) {
							won = (board.player == MY_PLAYER);
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
				log += "node proved " + won + "\n";
				if(DEBUG_PROVED) file_proved.write("node proved " + won + "\n");

				node.prove(won);
				return;
			}

			node.setExpanded();
			byte[] children_cols = new byte[available_cols_n];
			
			/* Fill children with such columns, and sort by threat_scores at the same time.
			 * This is made in two steps: first fill and shuffle moves, then assign parent.children related to those shuffled moves.
			 */
			for(j = 0, current_child = 0; j < BoardBit.N && current_child < available_cols_n; j++) {
				if(col_scores[j] > 0)
				{
					children_cols[current_child++] = (byte)j;
					// move back the new child in the right place
					for(k = current_child - 1; (k > 0) && (threats[children_cols[k - 1]] > threats[j]); k--)
						Auxiliary.swapByte(children_cols, k, k - 1);
				}
			}

			// shuffle children with same priority
			int start, end;
			for(start = 0; start < available_cols_n; start++) {
				for(end = start + 1;
					end < available_cols_n && threats[children_cols[end]] == threats[children_cols[start]];
					end++
				) ;
				Auxiliary.shuffleArrayRangeByte(children_cols, start, end);
				start = end;
			}
	
			// now create the children
			for(current_child = 0; current_child < available_cols_n; current_child++)
			{
				j = children_cols[current_child];
				board.markCheck(j);
				
				PnTTnode child = TTdag.get(PnTTnode.setKey(key_dag, board.hash, node.depth + 1));
				if(child == null) {
					child = node.createChild();
					/* Heuristic initialization: nodes without any threat should be considered less (or not at all).
					 * Proof init offset: if threats=0, BoardBit.N+1 should give enough space to prioritize other moves;
					 * otherwise, use current_child, so its an incremental number also respecting the random shuffle.
					 */
					setProofAndDisproofNumbers(child, (threats[j] == 0) ? (BoardBit.N + 1) : (current_child) );
					
					// debug
					if(DEBUG_PROVED) file_proved.write("created at col " + j + "\t" + board.hash + "\tn " + child.n[0] + "\t" + child.n[1] + "\n");

					// debug
					if(!child.isProved()) {
						created_n++;
					}
					
				} else {
					// debug
					if(DEBUG_PROVED) file_proved.write("found in dag\t" + board.hash + "\t" + child.n[0] + "\t" + child.n[1] + "\n");
				}

				board.unmark(j);
			}

		}

		/**
		 * Complexity:
		 * 		worst: O(2Nh)
		 * 			note: setProofAndDisproofNumbers always is called in expanded case, in intermediate nodes.
		 * 		avg: O(Nh) ?
		 * @param node
		 * @param game_state init to `board.game_state`
		 * @param most_proving true if it's the most_proving node, i.e. only first it
		 * @return
		 */
		public void updateAncestorsWhileChanged(int depth, LinkedList<BoardBitPn> boards_to_prune, TTElementProved caller, boolean most_proving) throws IOException {
			
			// debug
			if(DEBUG_LOG) log += "updateAncestors for " + board.hash + "\t " + depth + "\n";
			
			PnTTnode node = getEntry(board.hash, TTElementProved.COL_NULL, depth, board.player);
			TTElementProved entry = getEntryProved(board.hash, TTElementProved.COL_NULL, depth, board.player);
			
			
			if(entry != null) {
				// just need to update deepest move (using caller), so can save time
				
				// debug
				if(DEBUG_PROVED) file_proved.write("updateAncestors for " + board.hash +"\t" + depth + " already proved\n");

				if(caller != null) {
					TTElementProved best_child = getEntryProved(board.hash, entry.col(), entry.depth_cur, board.player);
					if(isBetterChild(entry, best_child, caller))
						entry.set(getColFromEntryProved(caller, depth), caller.depth_reachable);
					else
						return;
				} else if(most_proving) {
					updateProved(entry);
					boards_to_prune.add(new BoardBitPn(board));
				} else
					return;
			} else if(node != null) {
				// debug
				long old_most_proving = (node.most_proving_col==-1)?-1:TTdag.getHash(board.hash, board.free[node.most_proving_col], node.most_proving_col, Auxiliary.getPlayerBit(board.player));
				
				int old_proof = node.n[PROOF], old_disproof = node.n[DISPROOF];
				setProofAndDisproofNumbers(node, 0);		// offset useless, node always expanded here 
				
				if(old_proof == node.n[PROOF] && old_disproof == node.n[DISPROOF])
					return;
				else if(node.isProved()) {
					
					// debug
					if(DEBUG_PROVED) file_proved.write("setProof for " + board.hash + "\t" + node.depth + "\tn " + old_proof + "\t" + old_disproof + "most proving " + old_most_proving + "\n\t->" 
						+ node.n[0] + "\t" + node.n[1] + "\t" +((node.most_proving_col==-1)?-1:TTdag.getHash(board.hash, board.free[node.most_proving_col], node.most_proving_col, Auxiliary.getPlayerBit(board.player))) + "\n");
					
					boards_to_prune.add(new BoardBitPn(board));
				}
			} else {
				return;
			}

			if(depth == root.depth)	// no recursion on root's parents
				return;
			
			// if changed
			for(int j = 0; j < BoardBit.N; j++) {
				if(board.cellState(j) == Auxiliary.opponent(board.player)) {
					board.unmark(j);
					updateAncestorsWhileChanged(depth - 1, boards_to_prune, entry, false);	// marked=null, so don't push/pop when not on most_proving path
					board.mark(j);
				}
			}

		}

		/**
		 * Complexity: O(h)
		 * @param depth where to reset to
		 * @param marked
		 */
		private void resetBoard(LinkedList<Integer> marked) {
			while(marked.size() > 0) {

				// debug
				if(DEBUG_LOG) log += "resetBoard, depth " + (root.depth + marked.size()) + "\n";
				
				board.unmark(marked.pop());
			}
		}

		/**
		 * Remove any reference to `node`, and also to its descendants if they only had one parent.
		 * @param isroot true for tree root
		 */
		private void pruneTree(BoardBitPn board, int depth, boolean isroot) throws IOException {

			// debug
			//log += "_clearProved: col ";
			//if(node.parents != null && node.parents.size() > 0)
			//      log += node.lastMoveFromFirstParent();
			//log += ", isroot: " + (node == root) + "parents=null,children=null" + (node.parents == null) + " " + (node.children == null) + "\n";

			// (if exists)
			
			PnTTnode node = getEntry(board.hash, TTElementProved.COL_NULL, depth, board.player);
			
			if (isroot || ( node != null && !node.hasParents() )) {

				if(node != null) {
					TTdag.remove(PnTTnode.setKey(key_dag, board.hash, depth));
					TTdag.count--;
				}
				
				// debug
				if(DEBUG_PROVED) file_proved.write(board.hash + "\t" + depth + "\n");

				for(int j = 0; j < BoardBit.N; j++) {
					if(board.freeCol(j)) {
						board.mark(j);
						pruneTree(board, depth + 1, false);
						board.unmark(j);
					}
				}
			}
		}
		/**
		 * 
		 * @param nodes_proved
		 */
		private void pruneTrees(LinkedList<BoardBitPn> boards) throws IOException {

			ListIterator<BoardBitPn> it_board = boards.listIterator();
			while(it_board.hasNext()) {

				// debug
				if(DEBUG_PROVED) file_proved.write("prune\n");

				PnTTnode.board = it_board.next();
				pruneTree(PnTTnode.board, PnTTnode.board.getDepth(), true);
			}
			boards.clear();
			PnTTnode.board = board;
		}


	//#endregion PN-SEARCH


	//#region AUXILIARY

		/**
		 * Init proof numbers to offset + current level in game tree.
		 * <p>
		 * Complexity: O(1)
		 * @param node
		 */
		protected void initProofAndDisproofNumbers(PnTTnode node, int offset) {
			int number = offset + getRelativeDepth(node) / 2 + 1;		// never less than 1
			node.setProofAndDisproof(number, number);
		}

		private void setDeepestProvedDescendant(TTElementProved entry, long hash, byte player) throws IOException {

			// debug
			if(DEBUG_PROVED) file_proved.write("setDeepest for " + hash + " " + entry.won() + "\tplayer " + player + "\n");
			String s = "";
			for(int j = 0; j < BoardBit.N; j++) {
				if(board.freeCol(j)) {
					TTElementProved entry_child = getEntryProved(hash, j, entry.depth_cur, player);
					if(entry_child != null) {
						s +=  "won " + entry_child.won() + " col " + entry_child.col() + " depth " + entry_child.depth_cur + " depth_max " + entry_child.depth_reachable +  "\n";
						if(DEBUG_PROVED) file_proved.write("setDeepest: won " + entry_child.won() + "\t,col " + entry_child.col() + "\t,depth " + entry_child.depth_cur + " depth_max " + entry_child.depth_reachable +  "\n");
					} else {
						if(DEBUG_PROVED) file_proved.write("setDeepest: " + TTdag.getHash(hash, board.free[j], j, Auxiliary.getPlayerBit(player)) + "\t" + board.free[j] + " " + j  + "\n");

					}
				}
			}
			if(!s.equals(""))
				if(DEBUG_PROVED) file_proved.write( "setDeepestProved, for " + hash + " " + entry.won() + " " + entry.col() + " " + entry.depth_reachable + " " + entry.depth_cur +  "\n" + s + "\n");
			
			int best_col = entry.col(), best_depth = entry.depth_reachable;
			TTElementProved entry_child;
			for(int j = 0; j < BoardBit.N; j++) {
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
				entry.set(best_col, (short)best_depth);
			}
		}
		/**
		 * check all proved children, to find the best and deeepst/least deep path.
		 * @param entry
		 */
		private void updateProved(TTElementProved entry) {

			TTElementProved best_child = null;
			for(int j = 0; j < BoardBit.N; j++) {
				if(board.freeCol(j)) {
					TTElementProved child = getEntryProved(board.hash, j, entry.depth_cur, board.player);
					if(isBetterChild(entry, best_child, child)) {
						best_child = child;
						entry.set(j, best_child.depth_reachable);
					}
				}
			}
		}
		/**
		 * 
		 * @param current
		 * @param test
		 * @return true if should replace current with test.
		 */
		private boolean isBetterChild(TTElementProved parent, TTElementProved current, TTElementProved test) {
			return test != null &&
				(current == null || ( parent.won() != current.won() && parent.won() == test.won() )	// set anyway
				|| current.won() == test.won() && ( test.depth_reachable > current.depth_reachable == (parent.won() == current.won()) )	// set if deeper
			);
		}
		
		private int getRelativeDepth(PnTTnode node) {
			return node.depth - root.depth;
		}
		/**
		 * Get entry from TTdag.
		 * @param hash node's hash
		 * @param col set to col for node's child if want entry for child, else TTElementProved.COL_NULL
		 * @param depth node's depth
		 * @param player current player at node (in case, who's making move col)
		 * @return entry
		 */
		private PnTTnode getEntry(long hash, int col, int depth, byte player) {
			if(col == TTElementProved.COL_NULL)
				return TTdag.get(PnTTnode.setKey(key_dag, hash, depth));
			else
				return TTdag.get(PnTTnode.setKey(key_dag, TTdag.getHash(hash, board.free[col], col, Auxiliary.getPlayerBit(player)), depth + 1));
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
				return TTproved.get(TTElementProved.setKey(TTproved_key, TTdag.getHash(hash, board.free[col], col, Auxiliary.getPlayerBit(player)), depth + 1));
		}
		private int getColFromEntryProved(TTElementProved child, int depth) {
			for(int j = 0; j < BoardBit.N; j++) {
				if(board.freeCol(j) && getEntryProved(board.hash, j, depth, board.player) == child)
					return j;
			}
			return -1;
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
			TTproved.count++;
			
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

		private void debugVisit(int idx, PnTTnode most_proving_node) {
			if(DEBUG_TIME) printTime();
			ms = System.currentTimeMillis();
			time_last[idx] = System.currentTimeMillis() - timer_start;
			mem_last[idx].set(runtime.maxMemory(), runtime.totalMemory(), runtime.freeMemory(), Auxiliary.freeMemory(runtime));
		}

	//#endregion AUXILIARY

}