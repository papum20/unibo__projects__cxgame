package connectx.pndb;


import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXCellState;
import connectx.CXGameState;
import connectx.CXPlayer;
import connectx.pndb.PnNode.Value;

import java.util.ArrayList;



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
public class PnSearch implements CXPlayer {

	//#region CONSTANTS
	private static final byte PROOF		= PnNode.PROOF;
	private static final byte DISPROOF	= PnNode.DISPROOF;

	private static final short OFFSET_MULTIPLIER = 3;

	private CXCellState MY_CX_WIN = CXCellState.P1;

	//#endregion CONSTANTS

	// board
	public BoardBit board;				// public for debug
	protected TranspositionTable TT;
	public byte current_player;			// public for debug
	private short level;				// current tree level (height)
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

				boolean my_turn = true;
				
				root.setProofAndDisproof(Constants.SHORT_1, Constants.SHORT_1);

				/* improvement: keep track of current node (next to develop), instead of 
				* looking for it at each iteration, restarting from root.
				*/
				PnNode currentNode = root;
				log = "before while";
				while(!root.isProved() && !isTimeEnded()) {

					//System.out.println("currentNode move: " + currentNode.col + ", mostProving move: " + ((currentNode.most_proving == null)? "null" : currentNode.most_proving.col) );

					PnNode mostProvingNode = null;
					mostProvingNode = selectMostProving(currentNode, current_player);
					log = "after selectMostProving";

					//System.out.println("most proving: " + mostProvingNode.col);
					
					developNode(mostProvingNode, my_turn, current_player);
					log = "after develop";

					//System.out.println("after develop\nroot numbers: " + root.n[0] + ", " + root.n[1]);
					//System.out.println("root children:");
					//for(PnNode child : root.children) {
					//	System.out.println(child.col + ":" + child.n[PROOF] + "," + child.n[DISPROOF]);
					//}

					currentNode = updateAncestorsWhileChanged(mostProvingNode);
					log = "after updateAncestors";

					//System.out.println("after ancestors\nroot numbers: " + root.n[0] + ", " + root.n[1]);
					//System.out.println("root children:");
					//for(PnNode child : root.children) {
					//	System.out.println(child.col + ":" + child.n[PROOF] + "," + child.n[DISPROOF]);
					//}
				}

				resetBoard(currentNode, root);
			}
			catch(Exception e) {
				System.out.println(log + "\n\tturn: " + current_player);
				throw e;
			}
		}

		/**
		 * 
		 * @param node
		 * @param game_state
		 * @param player who has to move in node's board.
		 * @return	a list of moves containing a threat, ordered according to db evaluations;
		 * 			otherwise null if game ended.
		 */
		private ArrayList<Integer> evaluate(PnNode node, byte game_state, byte player) {
		
			if(game_state == GameState.OPEN)
				return evaluateDb(node, player);
			else {
				if(game_state == GameState.P1)				// my win
					node.prove(true, true);		// root cant be ended, or the game would be
				else										// your win or draw
					node.prove(false, node != root);

				return null;
			}
		}

		/**
		 * Evaluate `node` according to a DbSearch.
		 * @param node
		 * @param player
		 * @return
		 */
		private ArrayList<Integer> evaluateDb(PnNode node, byte player) {

			String log = "evaliuate: \n";
			try {
				
				DbSearchResult res_db = dbSearch.selectColumn(board, node, timer_end - System.currentTimeMillis(), player);
		
				if(!res_db.won)
					return res_db.moves_ordered;

				/* if a win is found without expanding, need to save the winning move somewhere (in a child)
				* (especially for the root, or you wouldn't know the correct move)
				*/
				if(node != root)
					node.prove(true, false);
					
				// debug
				log += "db found win: " + res_db.moves_ordered.get(0) + " player: " + player + "\n\n";
				
				// root is only evaluated once, before expanding
				node.expand(1);
				node.children[0] = new PnNode(res_db.moves_ordered.get(0), node);							// can overwrite a child
				node.children[0].prove(true, false);
				
				return null;
				
			} catch (Exception e) {
				System.out.println(log);
				throw e;
			}
		}


		/**
		 * set proof and disproof; update node.mostProving.
		 * assuming value=unknown, as nodes are evaluated in developNode.
		 * @param node
		 * @param my_turn
		 * @param offset offset for initProofAndDisproofNumbers, in case of `node` open and not expanded.
		 */
		private void setProofAndDisproofNumbers(PnNode node, boolean my_turn, short offset) {

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
				}
				else if(board.game_state == GameState.OPEN)
					initProofAndDisproofNumbers(node, offset);
				else 
					node.prove(board.game_state == GameState.P1, node != root);

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
		private void developNode(PnNode node, boolean my_turn, byte player) {

			ArrayList<Integer> threat_cols = evaluate(node, board.game_state, player);

			if(threat_cols == null)
				return;

			generateAllChildren(node, threat_cols, my_turn);
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
			
			String log = "start";
			try {
				PnNode last_changed = node;
				boolean changed = true;
				// do at least one iteration, not to skip root's setProofAndDisproof (first turn)
				do {
					int old_proof = node.n[PROOF], old_disproof = node.n[DISPROOF];
					log = "before set";
					setProofAndDisproofNumbers(node, isMyTurn(), Constants.SHORT_0);		// offset useless, node always expanded here
					log = "after set";
					changed = (old_proof != node.n[PROOF] || old_disproof != node.n[DISPROOF]);
						
					if(node.isProved() && node != root)
						node.prove(node.n[PROOF] == 0 ? true : false, node != root);
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

		/**
		 * 
		 * @param node
		 * @param threat_cols
		 */
		public void generateAllChildren(PnNode node, ArrayList<Integer> threat_cols, boolean my_turn) {

			boolean[] used_cols = new boolean[board.N];
			short i = 0;
			
			node.expand(board.free_n);
			
			// create children for threat cells
			for(Integer col : threat_cols) {
				node.children[i] = new PnNode(col, node);
				node.children[i++].setProofAndDisproof(i, i);
				used_cols[col] = true;
			}

			// create other, secondary, children
			for(int j = 0; j < board.N; j++) {
				if(!used_cols[j] && board.freeCol(j)) {
					node.children[i] = new PnNode(j, node);
					mark(j);
					setProofAndDisproofNumbers(node.children[i], my_turn, (short)threat_cols.size());
					unmark(j);
					i++;
				}
			}

			// debug
			//System.out.println("in developNode: node col=" + node.col + ", node.chidren len=" + node.children.length);
		}


	//#endregion PN-SEARCH


	//#region AUXILIARY

		/**
		 * Init proof numbers to (offset * OFFSET_MULTIPLIER) + current level in game tree.
		 * @param node
		 */
		private void initProofAndDisproofNumbers(PnNode node, short offset) {
			short number = (short)(offset * OFFSET_MULTIPLIER + level);
			node.setProofAndDisproof(number, number);
		}
		/**
		 * 
		 */
		private byte mark(int col) {
			byte res = board.mark(col, current_player);
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

	//#endregion AUXILIARY

}