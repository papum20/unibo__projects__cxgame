package pndbg.alpha;

import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXPlayer;
import pndbg.constants.Auxiliary;
import pndbg.constants.CellState;
import pndbg.constants.Constants;
import pndbg.constants.GameState;
import pndbg.old.Board;
import pndbg.tt.TranspositionElementEntry;
import pndbg.tt.TranspositionTable;



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
public abstract class _PnSearch<RES, DB extends IDbSearch<RES>> implements CXPlayer {

	//#region CONSTANTS
	protected static final byte PROOF		= PnNode.PROOF;
	protected static final byte DISPROOF	= PnNode.DISPROOF;

	protected byte MY_WIN = CellState.P1;

	//#endregion CONSTANTS

	// board
	public BoardBit board;				// public for debug
	protected TranspositionTable TT;
	protected final _Operators OPERATORS;
	public byte current_player;			// public for debug
	protected short current_level;		// current tree level (height)
	protected DB dbSearch;

	// nodes
	protected PnNode root;

	// time / memory
	protected long timer_start;			//turn start (milliseconds)
	protected long timer_duration;			//time (millisecs) at which to stop timer
	protected Runtime runtime;

	// debug
	private final boolean DEBUG_ON		= false;
	private final boolean DEBUG_TIME	= false;
	protected String log;
	private long ms;
	private int visit_loops_n;

	
	

	protected _PnSearch(_Operators operators) {
		OPERATORS = operators;
	}
	
	/**
	 * Complexity: O(5N + 4MN + 2**16 + (5MN + 3M+4N + 2**16) ) = O(9MN + 3M+9N + 2**17)
	 */
	@Override
	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {

		board = new BoardBit(M, N, X);
		TT = new TranspositionTable(M, N);

		BoardBit.TT = TT;

		if(first)	current_player = CellState.P1;
		else		current_player = CellState.P2;

		timer_duration = (timeout_in_secs - 1) * 1000;
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

				node.prove( entry.state[Auxiliary.getPlayerBit(player)] == MY_WIN, false);
				return true;
			}
			else {
				if(game_state == GameState.WINP1)				// my win
					node.prove(true, true);		// root cant be ended, or the game would be
				else										// your win or draw
					node.prove(false, true);

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
		protected abstract boolean evaluateDb(PnNode node, byte player);


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
		protected void setProofAndDisproofNumbers(PnNode node, boolean my_turn, short offset) {

			log += "setProof\n";

			// if proof or disproof reached 0, because all children were proved
			if(node.isProved())
			{
				if(node.n[PROOF] == 0) {
					node.prove(true, node != root);
					TT.setStateOrInsert(board.hash, GameState.WINP1, 0);
				} else {
					node.prove(node.n[PROOF] == 0 ? true : false, node != root);
					TT.setStateOrInsert(board.hash, GameState.WINP2, 1);
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
				node.prove(board.game_state == GameState.WINP1, node != root);

		}

		/**
		 * Complexity: O(h + 4X), where h is the length of the path calling_node-most_proving_descndant
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
		 * Complexity: 
		 * 		for alpha: O(2DbSearch + 2N**2 + (17+4X)N ) if M similar to N
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
		public abstract void generateAllChildren(PnNode node, byte player);


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
		 * Complexity: O(1)
		 * @param node
		 */
		protected void initProofAndDisproofNumbers(PnNode node, short offset) {
			short number = (short)(offset + current_level);		// never less than 1, as level init to 1
			node.setProofAndDisproof(number, number);
		}

		/**
		 * mark/unmark and update current_player.
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
		 * Complexity: O(1)
		 * @return
		 */
		protected boolean isMyTurn() {
			return current_player == CellState.P1;
		}
		
		/**
		 * Complexity: worst: O(N)
		 * @return
		 */
		protected PnNode bestNode() {
			// child with min proof/disproof ratio (note that if a child has proof zero, it will be chosen)
			PnNode best = null;
			for(PnNode child : root.children) {
				if(child.n[DISPROOF] != 0 && (best == null || (float)child.n[PROOF] / child.n[DISPROOF] < (float)best.n[PROOF] / best.n[DISPROOF]) )
					best = child;
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