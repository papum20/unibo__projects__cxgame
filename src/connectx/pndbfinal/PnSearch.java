package connectx.pndbfinal;


import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXCellState;
import connectx.CXPlayer;
import connectx.pndbfinal.PnNode.Value;

import java.util.ArrayList;



/**
 * note: i'm always CellState.ME, GameState.P1
 */
public class PnSearch implements CXPlayer {

	//#region CONSTANTS
	private static final byte PROOF = PnNode.PROOF;
	private static final byte DISPROOF = PnNode.DISPROOF;

	private CXCellState MY_CX_WIN = CXCellState.P1;

	//#endregion CONSTANTS

	// board
	private BoardBit board;
	private byte current_player;
	private DbSearch dbSearch;

	// nodes
	private PnNode root;

	// time / memory
	protected long timer_start;			//turn start (milliseconds)
	protected long timer_end;			//time (millisecs) at which to stop timer
	protected Runtime runtime;


	
	
	
	@Override
	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {

		board = new BoardBit(M, N, X);
		if(first) current_player = CellState.P1;
		else current_player = CellState.P2;

		timer_end = (timeout_in_secs - 1) * 1000;
		runtime = Runtime.getRuntime();

		dbSearch = new DbSearch();
		dbSearch.init(M, N, X, first);
	}

	@Override
	public int selectColumn(CXBoard B) {
		// timer
		timer_start = System.currentTimeMillis();
		// update own board
		CXCell last_move = B.getLastMove();
		if(last_move != null) {
			mark(last_move.j);
		}
			
		// visit
		if(last_move == null) root = new PnNode(Board.COL_NULL, null);
		else root = new PnNode(last_move.j, null);
		visit();

		// return
		PnNode best = bestNode();
		if(best == null) best = root.most_proving;
		int move = best.col;

		mark(move);

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

			boolean my_turn = true;
			
			evaluate(root, GameState.OPEN, current_player);
			setProofAndDisproofNumbers(root, my_turn);

			/* improvement: keep track of current node (next to develop), instead of 
			* looking for it at each iteration, restarting from root.
			*/
			PnNode currentNode = root;
			while(!root.isProved() && !isTimeEnded()) {

				PnNode mostProvingNode = null;
				mostProvingNode = selectMostProving(currentNode, current_player);
				
				developNode(mostProvingNode, my_turn, current_player);

				currentNode = updateAncestorsUpTo(mostProvingNode);
			}

				resetBoard(currentNode, root);
		}

		/**
		 * 
		 * @param node
		 * @param game_state
		 * @param player who has to move in node's board.
		 */
		private void evaluate(PnNode node, byte game_state, byte player) {
		
			CXCell res_db = dbSearch.selectColumn(board, node, timer_end - System.currentTimeMillis(), player);

			/* if a win is found without expanding, need to save the winning move somewhere (in a child)
			 * (especially for the root, or you wouldn't know the correct move)
			 */
			if(res_db != null)
			{
				if(node.children != null) {
					for(PnNode child : node.children) {
						if(child.col == res_db.j) {
							child.prove(res_db.state == MY_CX_WIN, true);
							return;
						}
					}
				} else
				node.expand(1);

				// also if didn't find child for column
				node.children[0] = new PnNode(res_db.j, node);							// can overwrite a child
				node.children[0].prove(res_db.state == MY_CX_WIN, false);
	
				/*
				* note: should do something when found loss for root, like try to win something
				 */
			}
			// my win
			else if(game_state == GameState.P1) node.prove(true, node != root);
			// open: heuristic
			else if(game_state == GameState.OPEN) node.setProofAndDisproof(Constants.SHORT_1, Constants.SHORT_1);
			// your win or draw
			else node.prove(false, node != root);
		}

		/**
		 * set proof and disproof; update node.mostProving.
		 * assuming value=unknown, as nodes are evaluated in developNode.
		 */
		private void setProofAndDisproofNumbers(PnNode node, boolean my_turn) {

			if(node.isExpanded()) {
				PnNode most_proving;
				if(my_turn) {
					most_proving = node.minChild(PROOF);
					node.setProofAndDisproof(most_proving.n[PROOF], node.sumChildren(DISPROOF));
				} else {
					most_proving = node.minChild(DISPROOF);
					node.setProofAndDisproof(node.sumChildren(PROOF), most_proving.n[DISPROOF]);
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
		private PnNode selectMostProving(PnNode node, byte player) {

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
		private void developNode(PnNode node, boolean my_turn, byte player) {

			ArrayList<Integer> free_cols = board.freeCols();
			node.expand(free_cols.size());
			node.generateAllChildren(free_cols);

			//System.out.println("in developNode: node col=" + node.col + ", node.chidren len=" + node.children.length);

			// evaluate children
			for(PnNode child : node.children) {
				byte game_state = mark(child.col);
				evaluate(child, game_state, Auxiliary.opponent(player));
				setProofAndDisproofNumbers(child, my_turn);
				unmark(child.col);
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
		public PnNode updateAncestorsUpTo(PnNode node) {
			
			PnNode last_changed = node;
			boolean changed = true;
			// do at least one iteration, not to skip root's setProofAndDisproof (first turn)
			do {
				int old_proof = node.n[PROOF], old_disproof = node.n[DISPROOF];
				setProofAndDisproofNumbers(node, isMyTurn());
				changed = (old_proof != node.n[PROOF] || old_disproof != node.n[DISPROOF]);
					
				if(node.isProved() && node != root)
					node.prove(node.n[PROOF] == 0 ? true : false, node != root);
				
				last_changed = node;
				node = node.parent;
				if(changed && last_changed != root) unmark(last_changed.col);
			}
			while(changed && last_changed != root && last_changed != null);

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
		 * 
		 */
		private byte mark(int col) {
			byte res = board.mark(col, current_player);
			current_player = (byte)Constants.opponent(current_player);
			return res;
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

	//#endregion AUXILIARY

}