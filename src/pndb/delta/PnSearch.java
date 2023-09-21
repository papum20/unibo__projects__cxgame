package pndb.delta;

import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXPlayer;
import pndb.constants.Auxiliary;
import pndb.constants.CellState;
import pndb.constants.Constants;
import pndb.constants.GameState;
import pndb.old.Board;
import pndb.tt.TranspositionElementEntry;
import pndb.tt.TranspositionTable;



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
 * 
 * TODO;
 * (pensa a quella cosa dell'altro operatore, difese, db etc.)
 * implementa quella cosa (se non richede troppo tempo)
 * .no prune, ricorda per prox it (prima test memoria con virtualsalcazzi)
 * .new findAls, con empty cells, pure per vertical
 * .rimuovi tt.doppia entry? (se alla fine se non serve)...
 * ...gestire nodi a cui si arriva in vari modi (perché magari ci si arriva la seconda volta prima di averlo risolto la prima) (dag)
 * (magari va bene, vuol dire che è più probabile e ha senso dargli più possibilità di esplorarlo; o magari no e bisogni farlo una sola volta)
 * 
 * last test not well (findALsInDIr)
 * 
 */
public class PnSearch implements CXPlayer {

	//#region CONSTANTS
	protected static final byte PROOF		= PnNode.PROOF;
	protected static final byte DISPROOF	= PnNode.DISPROOF;

	protected byte MY_WIN = CellState.P1;

	//#endregion CONSTANTS

	// board
	public BoardBit board;				// public for debug
	protected TranspositionTable TT;
	public byte current_player;			// public for debug
	protected short current_level;		// current tree level (height)
	protected DbSearch dbSearch;

	// nodes
	protected PnNode root;

	// time / memory
	protected long timer_start;			//turn start (milliseconds)
	protected long timer_duration;		//time (millisecs) at which to stop timer
	protected Runtime runtime;

	private short	level_root;			// tree depth starting from empty board
	private int		deepest_level;		// absolute (from empty board)
	private PnNode	deepest_node;

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

		board = new BoardBit(M, N, X);
		dbSearch = new DbSearch();		
		TT = new TranspositionTable(M, N);

		BoardBit.TT = TT;

		if(first)	current_player = CellState.P1;
		else		current_player = CellState.P2;
		
		timer_duration = (timeout_in_secs - 1) * 1000;
		level_root = first ? Constants.SHORT_0 : Constants.SHORT_1;
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
			
			deepest_level	= -1;	// any level is > -1
			deepest_node	= null;
			level_root++;			// at each visit, it indicates root's level
			
			// timer
			timer_start = System.currentTimeMillis();
			// update own board
			CXCell last_move = B.getLastMove();
			if(last_move != null)
				mark(last_move.j);
				
			// debug
			System.out.println("---\n" + playerName());
			if(last_move != null) System.out.println("Opponent: " + last_move.j);
			else System.out.println("Opponent: " + last_move);
			board.print();

			// visit
			if(last_move == null) root = new PnNode(Board.COL_NULL, null);
			else root = new PnNode(last_move.j, null);

			current_level = 1;
			visit();

			// return
			PnNode best = bestNode();
			if(best == null) best = root.most_proving;
			int move = best.col;

			mark(move);

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
		private void visit() {

			// debug
			visit_loops_n = 0;
			ms = System.currentTimeMillis();

			root.setProofAndDisproof(Constants.SHORT_1, Constants.SHORT_1);

			/* improvement: keep track of current node (next to develop), instead of 
			* looking for it at each iteration, restarting from root.
			*/
			PnNode currentNode = root;
			while(!root.isProved() && !isTimeEnded()) {

				// debug
				log = "visit no. " + visit_loops_n + " for player " + current_player + "\n";
				ms = System.currentTimeMillis();
				//System.out.println("currentNode move: " + currentNode.col + ", mostProving move: " + ((currentNode.most_proving == null)? "null" : currentNode.most_proving.col) );

				PnNode mostProvingNode = selectMostProving(currentNode);

				// debug
				if(DEBUG_TIME) printTime();
				if(DEBUG_ON) {
					System.out.println("most proving: " + mostProvingNode.col);
					board.print();
				}
				
				developNode(mostProvingNode, current_player);

				// debug
				if(DEBUG_TIME) printTime();
				//System.out.println("after develop\nroot numbers: " + root.n[0] + ", " + root.n[1] + "\nroot children");
				//for(PnNode child : root.children) System.out.println(child.col + ":" + child.n[PROOF] + "," + child.n[DISPROOF]);
				
				currentNode = updateAncestorsWhileChanged(mostProvingNode);
				
				// debug
				if(DEBUG_TIME) printTime();
				if(DEBUG_ON) {
					root.debug(root);
					board.print();
					//if(loops_n > 20) break;
				}
				visit_loops_n++;
				
			}

			resetBoard(currentNode, root);
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
				TranspositionElementEntry entry = TT.getState(board.hash);

				if(entry == null || entry.state[Auxiliary.getPlayerBit(player)] == GameState.NULL)
					return evaluateDb(node, player);

				// update max depth
				short entry_depth = TT.getFinalDepth(board.hash);
				if(entry_depth > deepest_level) {
					deepest_level	= entry_depth; 
					deepest_node	= node;
				}

				node.prove( entry.state[Auxiliary.getPlayerBit(player)] == MY_WIN, false);
				return true;
			}
			else {
				node.prove(game_state == GameState.WINP1, true);		// root cant be ended, or the game would be ended
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

			DbSearchResult res_db = dbSearch.selectColumn(board, node, timer_start + timer_duration - System.currentTimeMillis(), player, Operators.MAX_TIER);
	
			if(res_db == null)
				return false;

			TT.setStateOrInsert(player, Auxiliary.cellState2winState(player), Auxiliary.getPlayerBit(player));
				
			/* note: probably, prune is useless now, as evaluate is only called when node hasn't been expanded yet.
			*/
			
			/* if a win is found without expanding, need to save the winning move somewhere (in a child)
			* (especially for the root, or you wouldn't know the correct move)
			*/
			node.prove(player == CellState.P1, false);
			
			// root is only evaluated once, before expanding
			node.expand(1);
			node.children[0] = new PnNode(res_db.winning_col, node);							// can overwrite a child
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

			// Update deepest node.
			if(level_root + current_level + (res_db.threats_n * 2 + 1) > deepest_level) {
				deepest_level	= level_root + current_level + (res_db.threats_n * 2 + 1);	// each threat counts as a move per each, except the last, winning move 
				deepest_node	= node;
			}
			
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
					if(node.n[PROOF] == 0) {
						node.prove(true, node != root);
						TT.setStateOrInsert(board.hash, GameState.WINP1, Auxiliary.getPlayerBit(player));
					} else {
						node.prove(false, node != root);
						TT.setStateOrInsert(board.hash, GameState.WINP2, Auxiliary.getPlayerBit(player));
					}
				}
			}
			// game states
			else if(board.game_state == GameState.OPEN)
				initProofAndDisproofNumbers(node, offset);
			else 
				node.prove(board.game_state == GameState.WINP1, node != root);


			/* In case, update deepest node.
			 * No need to check node's value: if it's winning, the deepest_node won't be used;
			 * otherwise, it's not winning and it will be used.
			 * Also, this won't overwrite calculations in evaluateDb, as that adds moves.
			 */
			if(board.game_state == GameState.DRAW) {
				deepest_level	= level_root + current_level + 1;		// max depth + 1
				deepest_node	= node;
			} else if(node.isProved() && (level_root + current_level > deepest_level)) {
				deepest_level	= level_root + current_level;
				deepest_node	= node;
			}

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
				mark(node.most_proving.col);
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

			/* Heuristic: nodes without any implicit threat should be considered less (or not at all),
			 * especially after a few moves (as, probably, after a few moves it's "guaranteed" there are always some).
			 * For now, not considered.
			 */
			if(res_db != null)
				related_cols = res_db.related_squares_by_col;
			else {
				related_cols = new int[board.N];
				for(j = 0; j < board.N; j++)
					if(board.freeCol(j)) related_cols[j] = 1;
			}
			
			// count the columns, i.e. the number of new children
			for(int moves_n : related_cols)
				if(moves_n > 0) related_cols_n++;

			node.expand(related_cols_n);
			current_child = 0;

			// fill children with such columns, and sort by threat_scores at the same time
			for(j = 0; j < board.N && current_child < related_cols_n; j++) {
				if(related_cols[j] > 0) {
					// then insert
					node.children[current_child++] = new PnNode(j, node);

					// set proof numbers
					/* Heuristic: nodes without any threat should be considered less (or not at all).
					 */
					mark(j);
					setProofAndDisproofNumbers(node.children[current_child - 1], current_player, (threats[j] == 0) ? 0 : (short)(board.N + 1) );
					unmark(j);

					// move back the new child in the right place
					for(k = current_child - 1; (k > 0) && (threats[node.children[k - 1].col] > threats[j]); k--)
						Auxiliary.swap(node.children, k, k - 1);
				}
			}

			// shuffle children with same priority
			int start, end;
			for(start = 0; start < related_cols_n; start++) {
				for(end = start + 1;
					end < related_cols_n && threats[node.children[end].col] == threats[node.children[start].col];
					end++
				) ;
				Auxiliary.shuffleArrayRange(node.children, start, end);
				start = end;
			}

		}

		/**
		 * Complexity: O(h)
		 * @param current
		 * @param root
		 */
		private void resetBoard(PnNode current, PnNode root) {
			while(current != root) {
				unmark(current.col);
				current = current.parent;
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
		public PnNode updateAncestorsWhileChanged(PnNode node) {
			
			log += "updateAncestors\n";

			PnNode last_changed;
			boolean changed = true;
			// do at least one iteration, not to skip root's setProofAndDisproof (first turn)
			do {
				int old_proof = node.n[PROOF], old_disproof = node.n[DISPROOF];
				setProofAndDisproofNumbers(node, current_player, Constants.SHORT_0);		// offset useless, node always expanded here

				changed = (old_proof != node.n[PROOF] || old_disproof != node.n[DISPROOF]) || node.isProved();
				
				last_changed = node;
				node = node.parent;
				if(changed && last_changed != root) unmark(last_changed.col);
			}
			while(changed && last_changed != root && node != null);

			return last_changed;
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
			short number = (short)(offset + current_level / 2 + 1);		// never less than 1, as level init to 1
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
			current_level++;
			return res;
		}
		/**
		 * Complexity: O(1)
		 * @param col
		 */
		protected void unmark(int col) {
			board.unmark(col);
			current_player = (byte)Constants.opponent(current_player);
			current_level--;
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

			// if all moves are lost, get the first move to the deepest node.
			if(best == null) {
				PnNode p = deepest_node;
				while(p != root) {
					best = p;
					p = p.parent;
				}
			}

			return best;
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