package connectx.pndbfinal;

import java.util.LinkedList;
import java.util.ListIterator;

import connectx.CXCell;
import connectx.CXGameState;
import connectx.pndbfinal.BiList.BiNode;
import connectx.pndbfinal.DbNode.BoardsRelation;
import connectx.pndbfinal.Operators.ThreatsByRank;
import connectx.pndbfinal.Operators.ThreatCells;
import connectx.pndbfinal.Operators.USE;



/**
 * note:
 * -	never unmarks, as always creates a copy of a board.
 */
public class DbSearch {
	
	//#region CONSTANTS
	private final int MAX_THREAT_SEQUENCES = 10;

	//#endregion CONSTANTS

	// time / memory
	private long timer_start;						//turn start (milliseconds)
	private long timer_end;							//time (millisecs) at which to stop timer
	private static final float TIMER_RATIO = 0.9f;	// see isTimeEnded() implementation
	protected Runtime runtime;

	private BoardBitDb board;
	protected TranspositionTable TT;

	// VARIABLES FOR A DB-SEARCH EXECUTION
	private int found_win_sequences;
	private DbNode win_node;
	private boolean[][] GOAL_SQUARES;
	


	public DbSearch() {
		runtime = Runtime.getRuntime();
	}
	

	public void init(int M, int N, int X, boolean first) {

		board = new BoardBitDb(M, N, X);
		TT = new TranspositionTable(M, N);
		BoardBitDb.TT = TT;
		
		GOAL_SQUARES = new boolean[M][N];
		for(int i = 0; i < M; i++)
			for(int j = 0; j < N; j++) GOAL_SQUARES[i][j] = false;

	}

	
	/**
	 * 
	 * @param board_pn
	 * @param root_pn
	 * @param time_remaining
	 * @return a CXCell, containing the state of the winning player, on null if didn't find a sequence.
	 */
	public CXCell selectColumn(BoardBit board_pn, PnNode root_pn, long time_remaining, byte player) {

		// timer
		timer_start	= System.currentTimeMillis();
		timer_end	= timer_start + time_remaining;

		// update own board instance
		board = new BoardBitDb(board_pn);
		board.setPlayer(player);

		board.findAllAlignments(player, Operators.TIER_MAX, "selCol_");
		//board.findAllAlignments(CellState.P2, Operators.TIER_MAX, "selCol_");
		//board.updateAlignments(last_move_pair, last_move.state);
		
		// db init
		DbNode root = createRoot(board);
		win_node = null;
		boolean found_sequence = false;
		found_win_sequences = 0;
		
		// recursive call for each possible move
		found_sequence = visit(root, player, true, Operators.TIER_MAX);
		root = null;

		// debug
		if(foundWin()) {
			win_node.board.print();
			return getBestMove(player);
		}

		// best move
		return null;

	}


		//#region ALGORITHM

		/**
		 * 
		 * @param root
		 * @param attacker
		 * @param attacking
		 * @param max_tier
		 * @return
		 */
		protected boolean visit(DbNode root, byte attacker, boolean attacking, int max_tier) {

			short level = 1;
			if(!attacking) level = 8;

			// init dependency and combination lists
			LinkedList<DbNode> lastDependency = new LinkedList<DbNode>(), lastCombination = new LinkedList<DbNode>();
			initLastCombination(root, lastCombination);

			// loop
			boolean found_sequence = false;
			while(	!isTimeEnded() && isTreeChanged(lastCombination) &&
					( (attacking && !foundWin() && found_win_sequences < MAX_THREAT_SEQUENCES) ||	//if attacker's visit: stop when found win
					(!attacking && !found_sequence) )												//if defender's visit: stop when found defense (any threat sequence)
			) {
				
				// start dependency stage
				lastDependency.clear();
				
				// HEURISTIC: only for attacker, only search for threats of tier < max tier found in defenses
				int max_tier_t = attacking? max_tier : root.getMaxTier();
				if(addDependencyStage(attacker, attacking, lastDependency, lastCombination, root, max_tier_t))			//uses lastCombination, fills lastDependency
				found_sequence = true;
					
				// START COMBINATIO STAGE
				if((attacking && !foundWin()) || (!attacking && !found_sequence)) {
					lastCombination.clear();
					
					if(addCombinationStage(root, attacker, attacking, lastDependency, lastCombination))		//uses lasdtDependency, fills lastCombination
					found_sequence = true;
				}
				
				// RE-CHECK AFTER COMBINATION
				level++;
			}

			return found_sequence;
		}

		/**
		 * @param root
		 * @param attacker
		 * @return true if found a defense (i.e. threat sequence was not wining)
		 */
		private boolean visitGlobalDefense(DbNode possible_win, DbNode root, byte attacker) {

			//add each combination of attacker's made threats to each dependency node
			DbNode new_root			= createDefensiveRoot(root, possible_win.board.markedThreats, attacker);
			int first_threat_tier	= new_root.getMaxTier();
			
			//visit for defender
			markGoalSquares(possible_win.board.getMarkedThreats(), true);
			//won for defender (=draw or win for defender)
			boolean defended = visit(new_root, Auxiliary.opponent(attacker), false, first_threat_tier);
			markGoalSquares(possible_win.board.getMarkedThreats(), false);

			if(!defended) win_node = possible_win;
			return defended;
		}

		/** (for now) assumptions:
		 * - the game ends only after a dependency stage is added (almost certain about proof)
		 * 	actually not true for mnk game (if you put 3 lined in a board, other 2 in another one, then merge the boards...)
		 */
		protected boolean addDependencyStage(byte attacker, boolean attacking, LinkedList<DbNode> lastDependency, LinkedList<DbNode> lastCombination, DbNode root, int max_tier) {

			boolean found_sequence = false;
			ListIterator<DbNode> it = lastCombination.listIterator();
			while(!isTimeEnded() && it.hasNext() && !found_sequence) {
				DbNode node = it.next();
				found_sequence = addDependentChildren(node, attacker, attacking, 1, lastDependency, root, max_tier);
			}
			return found_sequence;
		}

		protected boolean addCombinationStage(DbNode root, byte attacker, boolean attacking, LinkedList<DbNode> lastDependency, LinkedList<DbNode> lastCombination) {

			boolean found_sequence = false;
			ListIterator<DbNode> it = lastDependency.listIterator();
			while(!isTimeEnded() && it.hasNext() && !found_sequence) {
				DbNode node = it.next();
				found_sequence = findAllCombinationNodes(node, root, attacker, attacking, lastCombination, root);
			}
			return found_sequence;
		}

		/**
		 * 
		 * @param node
		 * @param attacker
		 * @param attacking
		 * @param lev
		 * @param lastDependency
		 * @param root
		 * @param max_tier
		 * @return true if found at least one winning sequence
		 */
		protected boolean addDependentChildren(DbNode node, byte attacker, boolean attacking, int lev, LinkedList<DbNode> lastDependency, DbNode root, int max_tier) {

			byte state = node.board.gameState();
			if(state == GameState.OPEN)
			{
				boolean found_sequence = false;
				//LinkedList<CXCell[]> applicableOperators = getApplicableOperators(node, MAX_CHILDREN, my_attacker);
				ThreatsByRank applicableOperators = getApplicableOperators(node.board, attacker, max_tier);

				for(LinkedList<ThreatCells> tier : applicableOperators) {
					if(tier != null)
					{
						for(ThreatCells threat : tier) {

							int atk_index = 0;
							//stops either after checking all threats, or if found a win/defense (for defended it is just any possible winning sequence)
							while( ((attacking && !foundWin()) || (!attacking && !found_sequence)) &&
							((atk_index = threat.nextAtk(atk_index)) != -1)
							) {
								if(isTimeEnded())
									return found_sequence;

								//if a goal square is marked, returns true, as goal squares are only used for defensive search, where only score matters
								MovePair atk_cell = threat.related[atk_index];
								if(GOAL_SQUARES[atk_cell.i][atk_cell.j]) {
									return true;
								}
								else {
									DbNode newChild = addDependentChild(node, threat, atk_index, lastDependency, lev);

									if(addDependentChildren(newChild, attacker, attacking, lev+1, lastDependency, root, max_tier))
										found_sequence = true;
									if(foundWin() || found_win_sequences >= MAX_THREAT_SEQUENCES) return found_sequence;
									else atk_index++;
								}
							}
						}
					}
				}
				return found_sequence;
			}
			else {
				int attacker_i = attacking? 0:1;
				TT.setStateOrInsert(node.board.hash, Auxiliary.gameState2CX(state), attacker_i);
				
				if(state == GameState.DRAW) return !attacking;
				else if(state == Auxiliary.cellState2winState(attacker)) {
					if(attacking) {
						found_win_sequences++;
						visitGlobalDefense(node, root, attacker);
					}
					return true;
				}
				else return false;	//in case of loss or draw
			}
		}

		/**
		 * @param partner : fixed node for combination
		 * @param node : iterating node for combination
		 */
		protected boolean findAllCombinationNodes(DbNode partner, DbNode node, byte attacker, boolean attacking, LinkedList<DbNode> lastCombination, DbNode root) {
			
			if(node == null || found_win_sequences >= MAX_THREAT_SEQUENCES) return false;
			else {
				byte state = node.board.gameState();
				
				if(state == GameState.OPEN) {
					boolean found_sequence = false;

					//doesn't check if isDependencyNode() : also combinations of combination nodes could result in alignments
					DbNode.BoardsRelation relation = partner.validCombinationWith(node, attacker);

					if(relation != BoardsRelation.CONFLICT) {
						if(relation == BoardsRelation.USEFUL) {

							//create combination with A's board (copied)
							if(addCombinationChild(partner, node, lastCombination, root, attacker, attacking))
								found_sequence = true;

							if(foundWin())
								return true;
						}
						
						if(!isTimeEnded() && findAllCombinationNodes(partner, node.getFirstChild(), attacker, attacking, lastCombination, root)) {
							if(foundWin()) return true;
							else found_sequence = true;
						}
					}

					if(!isTimeEnded() && findAllCombinationNodes(partner, node.getSibling(), attacker, attacking, lastCombination, root))
						found_sequence = true;
					return found_sequence;
				}
				// GAME STATE CASES
				else if(state == GameState.DRAW) return !attacking;
				else return (state == Auxiliary.cellState2winState(attacker));
			}
		}
	
	//#endregion ALGORITHM


	//#region CREATE

		protected DbNode createRoot(BoardBitDb B) {

			DbNode root = DbNode.copy(board, true, Operators.TIER_MAX, true);
			return root;
		}

		private DbNode createDefensiveRoot(DbNode root, LinkedList<ThreatApplied> athreats, byte attacker) {

			ListIterator<ThreatApplied> it = athreats.listIterator();
			ThreatApplied athreat = null;

			//create defenisve root copying current root, using opponent as player and marking only the move made by the current attacker in the first threat
			byte max_tier	= (byte)(Operators.tier(athreats.getFirst().threat.type) - 1);		// only look for threats better than mine
			DbNode def_root	= DbNode.copy(root.board, true, max_tier, false);
			def_root.board.setPlayer(Auxiliary.opponent(attacker));
			def_root.board.findAllAlignments(Auxiliary.opponent(attacker), max_tier, "defRoot_");

			//add a node for each threat, each node child/dependant from the previous one
			DbNode prev, node = def_root;
			while(it.hasNext()) {
				athreat = it.next();
				prev = node;
				prev.board.mark(athreat.threat.related[athreat.related_index], attacker);

				// related > 1 means there is at least 1 defensive move (bc there's always an attacker one)
				if(it.hasNext() || athreat.threat.related.length > 1) {
					BoardBitDb node_board = prev.board.getDependant(athreat.threat, athreat.related_index, USE.DEF, prev.getMaxTier(), true);
					node = new DbNode(node_board, true, prev.getMaxTier());
					prev.addChild(node);

					// now included in getDependant()
					//node.board.markCells(threat.def, YOUR_MNK_PLAYER);
					// for future enhancements?
					//node.board.addThreat(threat);
				}
				//the new node doesn't check alignments
			}
			
			return def_root;
		}

		/**
		 * sets child's game_state if entry exists in TT
		 */
		protected DbNode addDependentChild(DbNode node, ThreatCells threat, int atk, LinkedList<DbNode> lastDependency, int level) {
			
			BoardBitDb new_board	= node.board.getDependant(threat, atk, USE.BTH, node.getMaxTier(), true);

			int attacker_i			= new_board.currentPlayer;
			DbNode newChild			= new DbNode(new_board, false, node.getMaxTier());
			TranspositionElementEntry entry = TT.getState(new_board.hash);

			if(entry != null && entry.state[attacker_i] != null) {
				new_board.setGameState(entry.state[attacker_i]);
			}
			else {
				TT.insert(new_board.hash, CXGameState.OPEN, attacker_i);
				//only adds child to tree and list if doesn't already exist
				node.addChild(newChild);
				lastDependency.add(newChild);
			}
			return newChild;
		}

		/**
		 * (I hope) adding the child to both parents is useless, for now
		 * @return true if found any possible winning sequence
		 */
		protected boolean addCombinationChild(DbNode A, DbNode B, LinkedList<DbNode> lastCombination, DbNode root, byte attacker, boolean attacking) {

			int attacker_i			= attacking? 0:1;
			int max_threat			= Math.min(A.getMaxTier(), B.getMaxTier());
			BoardBitDb new_board	= A.board.getCombined(B.board, attacker, max_threat);
			DbNode new_child		= null;

			byte state = new_board.gameState();
			TranspositionElementEntry entry = TT.getState(new_board.hash);

			if(state != GameState.OPEN || (entry != null && entry.state[attacker_i] != null && entry.state[attacker_i] != CXGameState.OPEN) || new_board.hasAlignments(attacker)) {
				//only create node if winning/drawn (to check it) or if has threats (to continue visit)
				//if entry.state==OPEN: means node was already visited

				if(entry == null || entry.state[attacker_i] == null) {

					new_child = new DbNode(new_board, true, (byte)max_threat);

					// if open: continue visit
					if(state == GameState.OPEN) {		//state, state_TT = open, means the if's condition was validated by hasAlignments()
						// if no TT entry, update it...
						TT.insert(new_board.hash, Auxiliary.gameState2CX(state), attacker_i);
						//only add child to tree and list if doesn't already exist, has threats and is not in ended state
						A.addChild(new_child);
						//B.addChild(newChild);
						lastCombination.add(new_child);
					} else {							// if game ended: update TT
						TT.insert(new_board.hash, Auxiliary.gameState2CX(state), attacker_i);
						if(state == Auxiliary.cellState2winState(attacker)) {	// if won: check defenses
							if(attacking) {
								found_win_sequences++;
								visitGlobalDefense(new_child, root, attacker);
							}
						}
					}

				}
				else {
					//if TT has entry, update board's state (if OPEN, remains OPEN)
					new_board.setGameState(entry.state[attacker_i]);
				}
			}

			return (state == Auxiliary.cellState2winState(attacker));
		}


		/**
		 * add to lastCombination all node's descendants
		 * @param node
		 * @param lastCombination
		 */
		private void initLastCombination(DbNode node, LinkedList<DbNode> lastCombination) {
			if(node != null) {
				lastCombination.addLast(node);
				initLastCombination(node.getSibling(), lastCombination);
				initLastCombination(node.getFirstChild(), lastCombination);
			}
		}
		
	//#endregion CREATE

	//#region GET_SET

		protected ThreatsByRank getApplicableOperators(BoardBitDb board, byte attacker, int max_tier) {

			byte defender		= Auxiliary.opponent(attacker);
			ThreatsByRank res	= new ThreatsByRank();

			for(AlignmentsList alignments_by_row : board.alignments_by_direction) {
				for(BiList_ThreatPos alignments_in_row : alignments_by_row) {
					if(alignments_in_row != null) {
						BiNode<ThreatPosition> alignment = alignments_in_row.getFirst(attacker);
						if(alignment != null && Operators.tier(alignment.item.type) <= max_tier) {
							do {
								ThreatCells cell_threat_operator = Operators.applied(board, alignment.item, attacker, defender);

								if(cell_threat_operator != null) res.add(cell_threat_operator);
								alignment = alignment.next;
							} while(alignment != null);
						}
					}
				}
			}
			return res;
		}
	
		/**
		 * 
		 * @param threats : threats to mark as goal squares
		 * @param mark : true marks, false unmarks
		 */
		private void markGoalSquares(LinkedList<ThreatApplied> athreats, boolean mark) {
			for(ThreatApplied t : athreats) {
				for(MovePair cell : t.threat.related) GOAL_SQUARES[cell.i][cell.j] = mark;
			}
		}
		
	//#endregion GET_SET

	//#region HELPER

		protected boolean foundWin() {
			return win_node != null;
		}
	
		protected CXCell getBestMove(byte player) {
			int i = board.MC_n - 1;
			//return first player's move after initial state
			while(i < win_node.board.MC_n && Auxiliary.CX2cellState(win_node.board.getMarkedCell(i).state) != player)
				i++;
			return win_node.board.getMarkedCell(i);
		}
		
		/**
		 * 
		 * @return true if it's time to end the turn
		 */
		private boolean isTimeEnded() {
			return (System.currentTimeMillis() - timer_start) >= timer_end * TIMER_RATIO;
		}

		/* tree is changed if either lastdCombination o lastDependency are not empty;
			* however, dependency node are created from other dependency nodes only in the same level,
			* so such iteration would be useless
			*/
		protected boolean isTreeChanged(LinkedList<DbNode> lastCombination) {
			return lastCombination.size() > 0;
		}

		protected void printMemory() {
			long freeMemory = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
			System.out.println("memory: max=" + runtime.maxMemory() + " " + ", allocated=" + runtime.totalMemory() + ", free=" + runtime.freeMemory() + ", realFree=" + freeMemory);
		}
	
	//#endregion HELPER

}
