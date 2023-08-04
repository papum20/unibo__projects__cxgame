package pndb.nocel.nonmc;

import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXGameState;
import connectx.CXPlayer;
import pndb.alpha.BoardBit;
import pndb.alpha.DbSearchResult;
import pndb.alpha.IDbSearch;
import pndb.alpha.PnNode;
import pndb.constants.Auxiliary;
import pndb.constants.CellState;
import pndb.constants.Constants;
import pndb.constants.GameState;
import pndb.old.Board;
import pndb.tt.TranspositionElementEntry;
import pndb.tt.TranspositionTable;



/**
 * notes:
 * -	i'm always CellState.ME, GameState.P1.
 * -	TT: always used with MY_PLAYER = 0, YOUR = 1 (for state).
 * -	TT is used for positions evaluated and verified by db, so it contains certain values.
 * 
 * enhancements to do:
 * -	note: should do something when found loss for root, like try to win something
 * -	TT
 */
public class PnSearch<DB extends IDbSearch> implements CXPlayer {

	//#region CONSTANTS
	private static final byte PROOF		= PnNode.PROOF;
	private static final byte DISPROOF	= PnNode.DISPROOF;

	private byte MY_WIN = CellState.P1;

	//#endregion CONSTANTS

	// board
	public BoardBit board;			// public for debug
	private TranspositionTable TT;
	public byte current_player;			// public for debug
	private short level;				// current tree level (height)
	protected DB dbSearch;

	// nodes
	private PnNode root;

	// time / memory
	protected long timer_start;			//turn start (milliseconds)
	protected long timer_end;			//time (millisecs) at which to stop timer
	protected Runtime runtime;

	// debug
	private final boolean DEBUG_ON = false;
	private final boolean DEBUG_TIME = false;
	private String log;
	private long ms;
	private int visit_loops_n;

	
	
	
	@Override
	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {

		board = new BoardBit(M, N, X);
		TT = new TranspositionTable(M, N);

		BoardBit.TT = TT;

		if(first)	current_player = CellState.P1;
		else		current_player = CellState.P2;

		timer_end = (timeout_in_secs - 1) * 1000;
		runtime = Runtime.getRuntime();

		// dbSearch instantiated by subclass
		dbSearch.init(M, N, X, first);
	}

	@Override
	public int selectColumn(CXBoard B) {

		// debug
		try {
			
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

			level = 1;
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
			System.out.println(log);
			throw e;
		}

	}

	@Override
	public String playerName() {
		return "PnDb";
	}


	//#region PN-SEARCH

		/**
		 * iterative loop for visit
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

				PnNode mostProvingNode = selectMostProving(currentNode, current_player);

				// debug
				if(DEBUG_TIME) printTime();
				if(DEBUG_ON) {
					System.out.println("most proving: " + mostProvingNode.col);
					board.print();
				}
				
				developNode(mostProvingNode, current_player);
				
				// debug
				if(DEBUG_TIME) printTime();
				//System.out.println("after develop\nroot numbers: " + root.n[0] + ", " + root.n[1]);
				//System.out.println("root children:");
				//for(PnNode child : root.children) {
					//	System.out.println(child.col + ":" + child.n[PROOF] + "," + child.n[DISPROOF]);
					//}
				
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
		 * 
		 * @param node
		 * @param game_state
		 * @param player who has to move in node's board.
		 * @return true if evaluated the node, i.e. it's an ended state or db found a win sequence.
		 */
		private boolean evaluate(PnNode node, byte game_state, byte player) {

			// debug
			log += "evaluate\n";
		
			if(game_state == GameState.OPEN) {
				TranspositionElementEntry entry = TT.getState(board.hash);

				if(entry == null || entry.state[Auxiliary.getPlayerBit(player)] == null)
					return evaluateDb(node, player);

				node.prove( Auxiliary.CX2gameState(entry.state[Auxiliary.getPlayerBit(player)]) == MY_WIN, false);
				return true;
			}
			else {
				if(game_state == GameState.P1)				// my win
					node.prove(true, true);		// root cant be ended, or the game would be
				else										// your win or draw
					node.prove(false, true);

				return true;
			}
		}

		/**
		 * Evaluate `node` according to a DbSearch.
		 * @param node
		 * @param player
		 * @return true if found a win.
		 */
		private boolean evaluateDb(PnNode node, byte player) {

			log += "evaluateDb\n";

			DbSearchResult res_db = dbSearch.selectColumn(board, node, timer_end - System.currentTimeMillis(), player);
	
			if(res_db == null)
				return false;

			TT.setStateOrInsert(player, Auxiliary.cellState2winStateCX(player), Auxiliary.getPlayerBit(player));
				
			/* note: probably, prune is useless now, as evaluate is only called when node hasn't been expanded yet.
				*/
			
			/* if a win is found without expanding, need to save the winning move somewhere (in a child)
			* (especially for the root, or you wouldn't know the correct move)
			*/
			if(node != root)
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
				* As an additional proof, if current player could create a new threat or avoid the opponent's one with 
				* such different move, then `s` wouldn't represent a winning sequence (Db also checks defenses).
				*/
			//filterChildren(node.parent, res_db.related_squares_by_col);
			
			return true;
		}


		/**
		 * set proof and disproof; update node.mostProving.
		 * assuming value=unknown, as nodes are evaluated in developNode.
		 * @param node
		 * @param my_turn
		 * @param offset offset for initProofAndDisproofNumbers, in case of `node` open and not expanded.
		 */
		private void setProofAndDisproofNumbers(PnNode node, boolean my_turn, short offset) {

			log += "setProof\n";

			// if proof or disproof reached 0, because all children were proved
			if(node.isProved())
			{
				if(node.n[PROOF] == 0) {
					node.prove(true, node != root);
					TT.setStateOrInsert(board.hash, CXGameState.WINP1, 0);
				} else {
					node.prove(node.n[PROOF] == 0 ? true : false, node != root);
					TT.setStateOrInsert(board.hash, CXGameState.WINP2, 1);
				}
			}
			// if node has children, set numbers according to children numbers
			else if(node.isExpanded())
			{
				PnNode most_proving;

				if(my_turn) {
					most_proving = node.minChild(PROOF);
					node.setProofAndDisproof(most_proving.n[PROOF], node.sumChildren(DISPROOF));
				} else {
					most_proving = node.minChild(DISPROOF);
					node.setProofAndDisproof(node.sumChildren(PROOF), most_proving.n[DISPROOF]);
				}

				node.most_proving = most_proving;
			}
			// game states
			else if(board.game_state == GameState.OPEN)
				initProofAndDisproofNumbers(node, offset);
			else 
				node.prove(board.game_state == GameState.P1, node != root);

		}

		/**
		 * 
		 * @param node
		 * @return
		 */
		private PnNode selectMostProving(PnNode node, byte player) {
			
			log += "selectMostProving\n";

			if(!node.isExpanded()) return node;
			else {
				mark(node.most_proving.col);
				return selectMostProving(node.most_proving, (byte)Constants.opponent(player));
			}
			// node.most_proving should always be != null

		}

		/**
		 * 
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
		 * 
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

			DbSearchResult res_db = dbSearch.selectColumn(board, node, timer_end - System.currentTimeMillis(), Auxiliary.opponent(player));

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
					setProofAndDisproofNumbers(node.children[current_child - 1], isMyTurn(), (threats[j] == 0) ? 0 : (short)(board.N + 1) );
					unmark(j);

					// move back the new child in the right place
					for(k = current_child - 1; (k > 0) && (threats[node.children[k - 1].col] > threats[j]); k--)
						Auxiliary.swap(node.children, k, k - 1);
				}
			}

		}


		/**
		 * 
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
		 * 
		 * @param node
		 * @return
		 */
		public PnNode updateAncestorsWhileChanged(PnNode node) {
			
			log += "updateAncestors\n";

			PnNode last_changed = node;
			boolean changed = true;
			// do at least one iteration, not to skip root's setProofAndDisproof (first turn)
			do {
				int old_proof = node.n[PROOF], old_disproof = node.n[DISPROOF];
				setProofAndDisproofNumbers(node, isMyTurn(), Constants.SHORT_0);		// offset useless, node always expanded here

				changed = (old_proof != node.n[PROOF] || old_disproof != node.n[DISPROOF]) || node.isProved();
				
				last_changed = node;
				node = node.parent;
				if(changed && last_changed != root) unmark(last_changed.col);
			}
			while(changed && last_changed != root && last_changed != null);

			return last_changed;
		}

		/**
		 * Remove node's children with associated move in column, if column is == 0 in filter.
		 * @param node
		 * @param filter
		 */
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


	//#endregion PN-SEARCH


	//#region AUXILIARY

		/**
		 * Init proof numbers to offset + current level in game tree.
		 * @param node
		 */
		protected void initProofAndDisproofNumbers(PnNode node, short offset) {
			short number = (short)(offset + level);		// never less than 1, as level init to 1
			node.setProofAndDisproof(number, number);
		}
		/**
		 * 
		 */
		protected byte mark(int col) {
			byte res = board.markCheck(col, current_player);
			current_player = (byte)Constants.opponent(current_player);
			level++;
			return res;
		}
		/**
		 * unmark and update current_player.
		 * @param col : col to unmark
		 */
		private void unmark(int col) {
			board.unmark(col);
			current_player = (byte)Constants.opponent(current_player);
			level--;
		}
		/**
		 * 
		 * @return
		 */
		private boolean isMyTurn() {
			return current_player == CellState.P1;
		}
		/**
		 * 
		 * @return
		 */
		private PnNode bestNode() {
			// take child with min proof
			// int move = root.minChild(PROOF).col;
			// child with min proof/disproof ratio
			PnNode best = null;
			for(PnNode child : root.children) {
				if(child.n[DISPROOF] != 0 && (best == null || (float)child.n[PROOF] / child.n[DISPROOF] < (float)best.n[PROOF] / best.n[DISPROOF]) )
					best = child;
			}
			return best;
		}
		
		/**
		 * 
		 * @return true if it's time to end the turn
		 */
		private boolean isTimeEnded() {
			return (System.currentTimeMillis() - timer_start) >= timer_end;
		}

		/**
		 * @return true if available memory is less than a small percentage of max memory
		 */
		protected boolean isMemoryEnded() {
			// max memory useable by jvm - (allocatedMemory = memory actually allocated by system for jvm - free memory in totalMemory)
			long freeMemory = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
			return freeMemory < runtime.maxMemory() * (5 / 100);
		}

		protected void printMemory() {
			long freeMemory = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
			System.out.println("memory: max=" + runtime.maxMemory() + " " + ", allocated=" + runtime.totalMemory() + ", free=" + runtime.freeMemory() + ", realFree=" + freeMemory);
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