package connectx.pndb;


import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXPlayer;
import connectx.pndb.PnNode.Value;

import java.util.ArrayList;




public class PnSearch implements CXPlayer {


	// board
	Board board;
	byte current_player;

	// nodes
	PnNode root;

	// time / memory
	protected long timer_start;			//turn start (milliseconds)
	protected long timer_end;			//time (millisecs) at which to stop timer
	protected Runtime runtime;


	
	
	
	@Override
	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
		board = new Board(M, N, X);

		timer_end = timeout_in_secs - 1000;
		runtime = Runtime.getRuntime();
	}

	@Override
	public int selectColumn(CXBoard B) {
		// timer
		timer_start = System.currentTimeMillis();
		// update own board
		CXCell last_move = B.getLastMove();
		board.mark(last_move.j, CellState.YOU);

		// visit
		if(last_move == null) root = new PnNode(Board.COL_NULL, null);
		else root = new PnNode(last_move.j, null);
		current_player = CellState.ME;
		visit();

		// return
		int move = bestMove();
		return move;
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
			evaluate(root, root.col);
			setProofAndDisproofNumbers(root, true);

			/* improvement: keep track of current node (next to develop), instead of 
			* looking for it at each iteration, restarting from root.
			*/
			PnNode currentNode = root;
			while(root.value() == Value.UNKNOWN && !isTimeEnded()) {

				PnNode mostProvingNode = selectMostProving(currentNode, current_player);

				developNode(mostProvingNode, true, current_player);
				currentNode = updateAncestorsUpTo(mostProvingNode);

			}

			resetBoadrd(currentNode, root);
		}

		/**
		 * 
		 * @param node
		 */
		private void evaluate(PnNode node, byte game_state) {
			// my win
			if(game_state == GameState.P1) node.prove(true);
			// open: heuristic
			else if(game_state == GameState.OPEN) node.setProofAndDisproof(Constants.SHORT_1, Constants.SHORT_1);
			// your win or draw
			else node.prove(false);
		}

		/**
		 * set proof and disproof; update node.mostProving.
		 * assuming value=unknown, as nodes are evaluated in developNode.
		 */
		private void setProofAndDisproofNumbers(PnNode node, boolean my_turn) {
			if(node.isExpanded()) {
				PnNode most_proving;
				if(my_turn) {
					most_proving = node.minChild(PnNode.PROOF);
					node.setProofAndDisproof(most_proving.n[PnNode.PROOF], node.sumChildren(PnNode.DISPROOF));
				} else {
					most_proving = node.minChild(PnNode.DISPROOF);
					node.setProofAndDisproof(most_proving.sumChildren(PnNode.PROOF), most_proving.n[PnNode.DISPROOF]);
				}
				node.most_proving = most_proving;
			} else if(node.value() == Value.UNKNOWN) initProofAndDisproofNumbers(node);
			// else if value not unknown: numbers already set (implementation) (by evaluate)
		}

		/**
		 * 
		 * @param node
		 * @return
		 */
		private PnNode selectMostProving(PnNode node, int player) {
			if(!node.isExpanded()) return node;
			else {
				board.mark(node.most_proving.col, player);
				return selectMostProving(node.most_proving, Constants.opponent(player));
			}
			// node.most_proving should always be != null
		}

		/**
		 * 
		 * @param node
		 */
		private void developNode(PnNode node, boolean my_turn, int player) {
			ArrayList<Integer> free_cols = board.freeCols();
			node.expand(free_cols.size());
			node.generateAllChildren(free_cols);
			// evaluate children
			for(PnNode child : node.children) {
				byte game_state = board.mark(child.col, Constants.opponent(player));
				evaluate(node, game_state);
				setProofAndDisproofNumbers(node, my_turn);
				unmark(child.col);
			}
			

		}

		/**
		 * 
		 * @param current
		 * @param root
		 */
		private void resetBoadrd(PnNode current, PnNode root) {

		}

		/**
		 * 
		 * @param node
		 * @return
		 */
		public PnNode updateAncestorsUpTo(PnNode node) {
			PnNode last_changed = node;
			boolean changed = true;
			while(changed && last_changed != root) {
				int old_proof = node.n[PnNode.PROOF], old_disproof = node.n[PnNode.DISPROOF];
				setProofAndDisproofNumbers(node, isMyTurn());
				changed = (old_proof != node.n[PnNode.PROOF] || old_disproof != node.n[PnNode.DISPROOF]);

				if(node.isProved() && node != root)
					node.prove(node.n[PnNode.PROOF] == 0 ? true : false);

				last_changed = node;
				node = node.parent;
				if(changed && last_changed != root) unmark(last_changed.col);
			}

			return last_changed;
		}


	//#endregion PN-SEARCH


	//#region AUXILIARY

		/**
		 * 
		 * @param node
		 */
		private void initProofAndDisproofNumbers(PnNode node) {
			node.setProofAndDisproof(Constants.SHORT_1, Constants.SHORT_1);
		}
		/**
		 * unmark and update current_player.
		 * @param col : col to unmark
		 */
		private void unmark(int col) {
			board.unmark(col);
			current_player = (byte)Constants.opponent(current_player);
		}
		/**
		 * 
		 * @return
		 */
		private boolean isMyTurn() {
			return current_player == CellState.ME;
		}
		/**
		 * 
		 * @return
		 */
		private int bestMove() {
			// take child with min proof
			int move = root.minChild(PnNode.PROOF).col;
			return move;
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

	//#endregion AUXILIARY

}