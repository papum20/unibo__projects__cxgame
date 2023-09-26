package pndb.delt2;

import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXPlayer;
import pndb.constants.Auxiliary;
import pndb.constants.CellState;
import pndb.constants.Constants;
import pndb.constants.GameState;
import pndb.delt2.tt.TranspositionTable;
import pndb.delt2.tt.TranspositionTableNode;



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
 * 
 * TODO;
 * .new findAls, con empty cells, pure per vertical
 * .rimuovi tt.doppia entry? (se alla fine se non serve)...
 * .db corretto: db ricorsivo su tier3 (con più risposte)
 * ..oppure: provale tutte, con accortezze: 1. usa tutte minacce di 1atk 1def (saranno molte combinazioni in più);
 * 2. a questo punto in combine, stai attento che da ognuno si aggiunga l'intera minaccia (1atk e 1def, non solo atk) (se no 
 * rischi di aggiungere più atk che def e la situazione non sarebbe raggiungibile)
 * .stacked 13 dubious (are these new operators well made?)
 * .save best move in TT? for now, root cant use it
 * .if TT remains so, simplify for db (and reduce dimensions)
 * .note: tt.depth isn't used now
 * .tt could be bigger, or remove unused pnNodes (note: doesn't take so much memory)
 * TT.remove at start of each selectColumn, to remove entries from previous rounds
 * 
 * check there are not problems with def.length=0 with new operator
 * 
 */
public class PnSearch implements CXPlayer {

	//#region CONSTANTS
	protected static final byte PROOF		= PnNode.PROOF;
	protected static final byte DISPROOF	= PnNode.DISPROOF;

	protected byte MY_PLAYER	= CellState.P1;
	protected byte MY_WIN		= GameState.WINP1;
	protected byte YOUR_WIN		= GameState.WINP2;

	//#endregion CONSTANTS

	// board
	public BoardBit board;				// public for debug
	protected TranspositionTableNode TT;
	public byte current_player;			// public for debug
	protected DbSearch dbSearch;
	
	// nodes
	protected PnNode root;

	// time / memory
	protected long timer_start;			//turn start (milliseconds)
	protected long timer_duration;		//time (millisecs) at which to stop timer
	protected Runtime runtime;
	
	private short	depth_root;			// tree depth starting from empty board (absolute)
	protected short depth_current;		// current tree level (height), relative (=absolute-depth_root)

	private int		move_to_deepest;	// auxiliary to getDeepestNode

	// debug
	private final boolean DEBUG_ON		= false;
	private final boolean DEBUG_TIME	= false;
	protected String log;
	private long ms;
	private int visit_loops_n;

	
	

	public PnSearch() {}
	
	/**
	 * Complexity: O(5N + 4MN + 2**16 + (5MN + 3M+4N + 2**16) ) = O(9MN + 3M+9N + 2**17)
	 */
	@Override
	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {

		board		= new BoardBit(M, N, X);
		dbSearch	= new DbSearch();		
		TT			= new TranspositionTableNode(M, N);

		BoardBit.TT = new TranspositionTable(M, N);

		if(first)	current_player = CellState.P1;
		else		current_player = CellState.P2;
		
		timer_duration = (timeout_in_secs - 1) * 1000;
		depth_root = first ? Constants.SHORT_0 : Constants.SHORT_1;
		runtime = Runtime.getRuntime();

		// dbSearch instantiated by subclass
		dbSearch.init(M, N, X, first);

		// debug
		System.out.println("\n-_-\nSTART GAME\n-_-\n");
		
	}

	/**
	 * Complexity: O(4X + )
	 */
	@Override
	public int selectColumn(CXBoard B) {

		// debug
		try {
			
			depth_root++;			// at each visit, it indicates root's level
			
			// timer
			timer_start = System.currentTimeMillis();
			
			// update own board
			CXCell[] MC = B.getMarkedCells();
			if(MC.length == 0)
				root = new PnNode();
			else {
				mark(B.getLastMove().j);
				// see if new root was already visited, otherwise create it
				root = TT.getNode(board.hash);
				if(root == null) root = new PnNode();
			}

			// debug
			System.out.println("---\n" + playerName());
			if(B.getLastMove() != null) System.out.println("Opponent: " + B.getLastMove().j);
			else System.out.println("Opponent: " + B.getLastMove());
			board.print();
			
			// visit
			depth_current = 0;
			visit();

			// debug
			log += "ended visit\n";
			
			// return
			int move;
			PnNode best = bestNode();
			if(best == null && deepestNode(root) != root) {
				// if all moves are lost, get the first move to the deepest node.
				move = move_to_deepest;
			} else {
				 if(best == null) best = root.most_proving;
				move = root.lastMoveForChild(best);
			}
				
			mark(move);
			depth_root++;

			// debug
			log += "before debug&return\n";

			// debug
			root.debug(root);
			System.out.println("My move: " + move);
			board.print();

			return move;

		} catch (Exception e) {
			System.out.println("log pn: " + log + " error: " + e);
			throw e;
		}

	}

	@Override
	public String playerName() {
		return "PnDb delt2";
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
		private void visit() {

			// debug
			visit_loops_n = 0;
			ms = System.currentTimeMillis();

			root.setProofAndDisproof(Constants.SHORT_1, Constants.SHORT_1);

			/* improvement: keep track of current node (next to develop), instead of 
			* looking for it at each iteration, restarting from root.
			*/
			PnNode current_node = root, most_proving_node;
			while(!root.isProved() && !isTimeEnded()) {

				// debug
				log = "visit no. " + visit_loops_n + " for player " + current_player + "\n";
				ms = System.currentTimeMillis();
				//System.out.println("currentNode move: " + currentNode.col + ", mostProving move: " + ((currentNode.most_proving == null)? "null" : currentNode.most_proving.col) );

				most_proving_node = selectMostProving(current_node);

				// debug
				log += "most_proving col: " + ((most_proving_node == root) ? -1 : most_proving_node.lastMoveFromFirstParent()) + ", isroot: " + (most_proving_node == root) + "\n";
				if(DEBUG_TIME) printTime();
				if(DEBUG_ON) {
					System.out.println("most proving: " + ((most_proving_node == root) ? -1 : most_proving_node.lastMoveFromFirstParent()));
					board.print();
				}
				
				developNode(most_proving_node, current_player);

				// debug
				if(DEBUG_TIME) printTime();
				//System.out.println("after develop\nroot numbers: " + root.n[0] + ", " + root.n[1] + "\nroot children");
				//for(PnNode child : root.children) System.out.println(child.col + ":" + child.n[PROOF] + "," + child.n[DISPROOF]);
				
				current_node = updateAncestorsWhileChanged(most_proving_node, current_player);
				// debug
				log += "update ancestors end; now resetBoard, current_node==null?" + (current_node == null) + "\n";

				/* at this point of the loop, the most_proving_node was not-expanded, and now just got expanded,
				so its numbers always change in `updateAcestors`, except when, by chance, initialization numbers were the
				same as the newly calculated ones. */
				if(current_node == null) current_node = most_proving_node;
				resetBoard(most_proving_node, current_node);
				
				// debug
				log += "resetBoard end\n";
				log += root.debugString(root) + "\n";
				if(DEBUG_TIME) printTime();
				if(DEBUG_ON) {
					root.debug(root);
					board.print();
					//if(loops_n > 20) break;
				}
				visit_loops_n++;
				
			}
			
			// debug
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
		protected boolean evaluate(PnNode node, byte game_state, byte player) {

			// debug
			log += "evaluate\n";
		
			if(game_state == GameState.OPEN) {
				PnNode entry = TT.getNode(board.hash);

				if(node == root || entry == null)
					// if an entry exist, it was already analyzed with db
					return evaluateDb(node, player);

				return entry.isProved();
			}
			else {
				node.prove(game_state == GameState.WINP1, false);		// root cant be ended, or the game would be ended
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
		protected boolean evaluateDb(PnNode node, byte player) {

			log += "evaluateDb\n";

			TT.insert(board.hash, node, (short)(depth_root + depth_current));
			DbSearchResult res_db = dbSearch.selectColumn(board, node, timer_start + timer_duration - System.currentTimeMillis(), player, Operators.MAX_TIER);
	
			if(res_db == null)
				return false;

			/* note: probably, prune is useless now, as evaluate is only called when node hasn't been expanded yet.
			*/
			
			/* if a win is found without expanding, need to save the winning move somewhere (in a child)
			* (especially for the root, or you wouldn't know the correct move)
			*/
			node.prove(player == MY_PLAYER, false);
			
			// root is only evaluated once, before expanding
			node.expand(1);
			node.createChild(0, res_db.winning_col);			// can overwrite a child
			node.children[0].prove(player == CellState.P1, false);

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
			//filterChildren(node.parent, res_db.related_squares_by_col);

			// Update depth with the depth of the threat found
			TT.setNode(board.hash, node, (short)(depth_root + depth_current + (res_db.threats_n * 2 + 1)) );

			return true;
		}


		/**
		 * set proof and disproof; update node.mostProving.
		 * assuming value=unknown, as nodes are evaluated in developNode.
		 * Complexity: 
		 * 	-	case proved: O(1)
		 * 	-	case expanded: O(2N)
		 * 	-	else: O(1)
		 * @param node
		 * @param my_turn
		 * @param offset offset for initProofAndDisproofNumbers, in case of `node` open and not expanded.
		 */
		protected void setProofAndDisproofNumbers(PnNode node, byte player, short offset) {

			log += "setProof\n";

			// if node has children, set numbers according to children numbers
			if(node.isExpanded())
			{
				// debug
				log += "setProof: node expanded;node==root:" + (node==root) + "; move: " + ((node == root) ? -1 : node.lastMoveFromFirstParent()) + "\n";
				
				PnNode most_proving;

				if(player == CellState.P1) {
					most_proving = node.minChild(PROOF);
					node.setProofAndDisproof(most_proving.n[PROOF], node.sumChildren(DISPROOF));
				} else {
					most_proving = node.minChild(DISPROOF);
					node.setProofAndDisproof(node.sumChildren(PROOF), most_proving.n[DISPROOF]);
				}
				
				
				node.most_proving = most_proving;
				
				// if proof or disproof reached 0, because all children were proved
				if(node.isProved())
				{
					// debug
					log += "setProof: node proved;node==root:" + (node==root) + "; move: " + ((node == root) ? -1 : node.lastMoveFromFirstParent()) + "\n";
					
					if(node.n[PROOF] == 0) {
						node.prove(true, false);
					} else {
						node.prove(false, false);
					}
				}
			}
			// game states
			else if(board.game_state == GameState.OPEN)
			initProofAndDisproofNumbers(node, offset);
			else 
			node.prove(board.game_state == GameState.WINP1, false);
			
			// debug
			log += "setProof node expanded end\n";
			
		}

		/**
		 * Complexity: O(h + 4X), where h is the length of the path calling_node-most_proving_descndant
		 * @param node
		 * @return
		 */
		private PnNode selectMostProving(PnNode node) {
			
			log += "selectMostProving\n";

			if(!node.isExpanded()) return node;
			else {
				mark(node.lastMoveForChild(node.most_proving));
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
		private void developNode(PnNode node, byte player) {

			// debug
			log += "developNode\n";

			if(evaluate(node, board.game_state, player))
				return;

			// if the game is still open
			generateAllChildren(node, player);

		}

		
		/**
		 * Complexity: O(DbSearch + getThreatCounts + children_generation + sorting + shuffle )
		 *	<p>	= O(DbSearch + 12N**2 + N 4X + N**2 + N )
		 *	<p>	= O(DbSearch + 13N**2)
		 *	<p>		note: setProofAndDisproofNumbers can't go in expanded case from here, so it's O(1)
		 * @param node
		 * @param threat_scores_by_col
		 */
		public void generateAllChildren(PnNode node, byte player) {

			// debug
			log += "generateChildren\n";

			/* Heuristic: implicit threat.
			 * Only inspect moves in an implicit threat, i.e. a sequence by which the opponent could win
			 * if the current player was to make a "null move".
			 * In fact, the opponent could apply such winning sequence, if the current player was to 
			 * make a move outside it, thus not changing his plans.
			 * Applied to CXGame: columns where the opponent has an immediate attacking move - which leads to a win for him -,
			 * i.e. where the attacker's move corresponds to the first free cell in the column, are for sure
			 * the most interesting (in fact, you would lose not facing them); however, other columns involved in the sequence are
			 * not ignored, since they could block the win too, and also to simplify the calculations by approximation.
			 */
			/* note: related_cols should already contain only available, not full, columns.
			 */

			DbSearchResult res_db = dbSearch.selectColumn(board, node, timer_start + timer_duration - System.currentTimeMillis(), Auxiliary.opponent(player), Operators.MAX_TIER);

			/* Heuristic: sorting moves (previously selected from iterated related squares) by number/scores of own threats in them
			 * (i.e., for columns, the sum of the scores in the whole column).
			 */

			int		related_cols_n	= 0;
			int[]	related_cols,
					threats			= dbSearch.getThreatCounts(board, player);
			int current_child, j, k;
			PnNode	entry;

			/* Heuristic: nodes without any implicit threat should be considered less (or not at all),
			 * especially after a few moves (as, probably, after a few moves it's "guaranteed" there are always some).
			 * For now, used implicit threats number to give priorities.
			 */
			related_cols = new int[board.N];
			for(j = 0; j < board.N; j++) {
				if(res_db != null && res_db.related_squares_by_col[j] > 0) related_cols[j] = res_db.related_squares_by_col[j];
				else if(board.freeCol(j)) related_cols[j] = 1;
			}
			
			// count the columns, i.e. the number of new children
			for(int moves_n : related_cols)
				if(moves_n > 0) related_cols_n++;

			node.expand(related_cols_n);
			current_child = 0;

			/* Fill children with such columns, and sort by threat_scores at the same time.
			 * First fill and shuffle moves, then assign parent.children related to those shuffled moves.
			 */
			for(j = 0; j < board.N && current_child < related_cols_n; j++) {
				if(related_cols[j] > 0)
				{
					// only assign the move, for now
					node.cols[current_child++] = (byte)j;
					
					// move back the new child in the right place
					for(k = current_child - 1; (k > 0) && (threats[node.cols[k - 1]] > threats[j]); k--)
					Auxiliary.swapByte(node.cols, k, k - 1);
				}
			}

			// shuffle children with same priority
			int start, end;
			for(start = 0; start < related_cols_n; start++) {
				for(end = start + 1;
					end < related_cols_n && threats[node.cols[end]] == threats[node.cols[start]];
					end++
				) ;
				Auxiliary.shuffleArrayRangeByte(node.cols, start, end);
				start = end;
			}
				
			/* now create the children
			*/
			for(current_child = 0; current_child < related_cols_n; current_child++)
			{
				j = node.cols[current_child];

				mark(j);

				entry = TT.getNode(board.hash);
				if(entry == null) {
					node.createChild(current_child, j);
					// set proof numbers
					/* Heuristic: nodes without any threat should be considered less (or not at all).
					*/
					setProofAndDisproofNumbers(node.children[current_child], current_player, (threats[j] == 0) ? 0 : (short)(board.N + 1) );

					/* In case, update deepest node.
					* No need to check node's value: if it's winning, the deepest_node won't be used;
					* otherwise, it's not winning and it will be used.
					* Also, this won't overwrite calculations in evaluateDb, as that adds moves.
					*/
					if(board.game_state == GameState.DRAW)
						node.children[current_child].depth = (short)(depth_root + depth_current + 1);

					TT.insert(board.hash, node.children[current_child]);
				} else {
					node.addChild(current_child, entry, j);
				}

				unmark(j);
			}

		}

		/**
		 * Complexity:
		 * 		worst: O(2Nh)
		 * 			note: setProofAndDisproofNumbers always is called in expanded case, in intermediate nodes.
		 * 		avg: O(Nh) ?
		 * @param node
		 * @return
		 */
		public PnNode updateAncestorsWhileChanged(PnNode node, byte player) {
			
			log += "updateAncestors-loop: node with col " + ((node == root) ? -1 : node.lastMoveFromFirstParent()) + ", numbers " + node.n[PROOF] + " " + node.n[DISPROOF] + ", isroot " + (node==root) + ";;level=" + depth_current + "\n";
			
			int old_proof = node.n[PROOF], old_disproof = node.n[DISPROOF];
			setProofAndDisproofNumbers(node, player, Constants.SHORT_0);		// offset useless, node always expanded here

			// if changed
			if(old_proof != node.n[PROOF] || old_disproof != node.n[DISPROOF] || node.isProved()) {
				
				if(depth_current > 0)
				{
					PnNode last_changed = null;
					depth_current--;
					for(PnNode parent : node.parents) {
						/* the last parent used to go down the tree was put as first parent;
						 * however other parents and their ancestors still need to be updated.
						 */
						if(parent == node.parents.getFirst()) last_changed = updateAncestorsWhileChanged(parent, Auxiliary.opponent(player));
						else updateAncestorsWhileChanged(parent, Auxiliary.opponent(player));
					}
					depth_current++;

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
				log += "resetBoard, depth " + depth_current + "\n";
				
				unmark(current.lastMoveFromFirstParent());
				// any path goes back to root
				current = current.parents.getFirst();
			}
		}

		/**
		 * Remove node's children with associated move in column, if column is == 0 in filter.
		 * @param node
		 * @param filter
		 */
		/*
		private void filterChildren(PnNode node, int[] filter) {

			if(node == null)
				return;

			int to_delete = 0, i = 0;

			// count children to delete
			for(PnNode child : node.children)
				if(filter[child.col] == 0) to_delete++;
				
			// create new array
			PnNode[] new_children = new PnNode[node.children.length - to_delete];
			for(PnNode child : node.children)
				if(filter[child.col] != 0) new_children[i++] = child;
			
			node.children = new_children;
		}
		*/

	//#endregion PN-SEARCH


	//#region AUXILIARY

		/**
		 * Init proof numbers to offset + current level in game tree.
		 * <p>
		 * Complexity: O(1)
		 * @param node
		 */
		protected void initProofAndDisproofNumbers(PnNode node, short offset) {
			short number = (short)(offset + depth_current / 2 + 1);		// never less than 1
			node.setProofAndDisproof(number, number);
		}

		/**
		 * mark/unmark and update current_player.
		 * <p>
		 * Complexity: O(4X)
		 * @param col : col to mark/unmark
		 * @return new game_state
		 */
		protected byte mark(int col) {
			byte res = board.markCheck(col, current_player);
			current_player = (byte)Constants.opponent(current_player);
			depth_current++;
			return res;
		}
		/**
		 * Complexity: O(1)
		 * @param col
		 */
		protected void unmark(int col) {
			board.unmark(col);
			current_player = (byte)Constants.opponent(current_player);
			depth_current--;
		}
		
		/**
		 * Complexity: worst: O(N)
		 * @return
		 */
		protected PnNode bestNode() {

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


	//#endregion AUXILIARY

}