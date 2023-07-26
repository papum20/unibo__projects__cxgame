package connectx.pndb;


import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXPlayer;
import connectx.pndb.PnNode.Value;

import java.util.ArrayList;




public class PnSearch implements CXPlayer {

	//#region CONSTANTS
	private static final byte PROOF = PnNode.PROOF;
	private static final byte DISPROOF = PnNode.DISPROOF;

	//#endregion CONSTANTS

	// board
	BoardBit board;
	byte current_player;

	// nodes
	PnNode root;

	// time / memory
	protected long timer_start;			//turn start (milliseconds)
	protected long timer_end;			//time (millisecs) at which to stop timer
	protected Runtime runtime;


	
	
	
	@Override
	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
		board = new BoardBit(M, N, X);
		if(first) current_player = CellState.ME;
		else current_player = CellState.YOU;

		timer_end = (timeout_in_secs - 1) * 1000;
		runtime = Runtime.getRuntime();
	}

	@Override
	public int selectColumn(CXBoard B) {
		// timer
		timer_start = System.currentTimeMillis();
		// update own board
		CXCell last_move = B.getLastMove();
		if(last_move != null) {
			mark(last_move.j);
			System.out.println("Opponent: " + last_move.j);
		}
		else System.out.println("Opponent: " + last_move);
		board.print();
			
		// visit
		if(last_move == null) root = new PnNode(Board.COL_NULL, null);
		else root = new PnNode(last_move.j, null);
		visit();

		System.out.println("root numbers: " + root.n[0] + ", " + root.n[1]);
		System.out.println("root children:");
		for(PnNode child : root.children) {
			System.out.println(child.col + ":" + child.n[PROOF] + "," + child.n[DISPROOF]);
		}
		
		// return
		PnNode best = bestNode();
		if(best == null) best = root.most_proving;
		int move = best.col;

		mark(move);
		System.out.println("My move: " + move);
		board.print();

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
			String log = "start";
			try {
				evaluate(root, GameState.OPEN);
				setProofAndDisproofNumbers(root, true);

				/* improvement: keep track of current node (next to develop), instead of 
				* looking for it at each iteration, restarting from root.
				*/
				PnNode currentNode = root;
				log = "before while";
				while(root.value() == Value.UNKNOWN && !isTimeEnded()) {
					
					//System.out.println("currentNode move: " + currentNode.col + ", mostProving move: " + ((currentNode.most_proving == null)? "null" : currentNode.most_proving.col) );

					PnNode mostProvingNode = null;
					mostProvingNode = selectMostProving(currentNode, current_player);
					log = "after selectMostProving";
					
					developNode(mostProvingNode, true, current_player);
					log = "after develop";
					currentNode = updateAncestorsUpTo(mostProvingNode);
					log = "after updateAncestors";

				}

				resetBoard(currentNode, root);
			}
			catch(Exception e) {
				//System.out.println(log + "\n\tturn: " + current_player);
				throw e;
			}
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
			String log = "start set proof";
			try {
				if(node.isExpanded()) {
					PnNode most_proving;
					if(my_turn) {
						log = "my turn start";
						most_proving = node.minChild(PROOF);
						node.setProofAndDisproof(most_proving.n[PROOF], node.sumChildren(DISPROOF));
					} else {
						log = "not my turn start";
						most_proving = node.minChild(DISPROOF);
						log = "not my turn, before setProof";
						//System.out.println("in setProof: node col: " + node.col + ", most_proving col: " + most_proving.col);
						//System.out.println(most_proving.children == null);
						node.setProofAndDisproof(node.sumChildren(PROOF), most_proving.n[DISPROOF]);
					}
					log = "not my turn, after setProof";
					node.most_proving = most_proving;
				} else if(node.value() == Value.UNKNOWN) initProofAndDisproofNumbers(node);
				// else if value not unknown: numbers already set (implementation) (by evaluate)
			} catch (Exception e)  {
				System.out.println(log + "\n\tturn : " + current_player);
				throw e;
			}
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
		private void developNode(PnNode node, boolean my_turn, int player) {
			ArrayList<Integer> free_cols = board.freeCols();
			node.expand(free_cols.size());
			node.generateAllChildren(free_cols);

			//System.out.println("in developNode: node col=" + node.col + ", node.chidren len=" + node.children.length);

			// evaluate children
			for(PnNode child : node.children) {
				byte game_state = mark(child.col);
				evaluate(child, game_state);
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
			String log = "start";
			try {
				PnNode last_changed = node;
				boolean changed = true;
				// do at least one iteration, not to skip root's setProofAndDisproof (first turn)
				do {
					int old_proof = node.n[PROOF], old_disproof = node.n[DISPROOF];
					log = "before set";
					setProofAndDisproofNumbers(node, isMyTurn());
					log = "after set";
					changed = (old_proof != node.n[PROOF] || old_disproof != node.n[DISPROOF]);
						
					if(node.isProved() && node != root)
							node.prove(node.n[PROOF] == 0 ? true : false);
						log = "after prove";
						
						last_changed = node;
						node = node.parent;
						if(changed && last_changed != root) unmark(last_changed.col);
						log = "after unmark";
				}
				while(changed && last_changed != root && last_changed != null);

				return last_changed;
			}
			catch (Exception e) {
				System.out.println(log);
				throw e;
			}
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
			return current_player == CellState.ME;
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
				if(child.n[DISPROOF] != 0 && (best == null || child.n[PROOF] / child.n[DISPROOF] < best.n[PROOF] / best.n[DISPROOF]) )
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

	//#endregion AUXILIARY

}