package pndb.delta;

import java.util.LinkedList;
import java.util.ListIterator;

import pndb.delta.Operators.ThreatsByRank;
import pndb.delta.threats.ThreatApplied;
import pndb.delta.threats.ThreatCells;
import pndb.delta.threats.ThreatCells.USE;
import pndb.delta.constants.Auxiliary;
import pndb.delta.constants.CellState;
import pndb.delta.constants.GameState;
import pndb.delta.constants.MovePair;
import pndb.delta.constants.Constants.BoardsRelation;
import pndb.delta.structs.DbSearchResult;
import pndb.delta.tt.TranspositionTable;
import pndb.delta.tt.TranspositionTable.Element.Key;
import pndb.delta.tt.TTElementBool;



/**
 * note:
 * -	never unmarks, as always creates a copy of a board.
 * -	TT: always used with MY_PLAYER = 0, YOUR = 1 (for state).
 * -	Combination stage uses TT with open states, in order to only search for already done combinations once.
 * 		However, being this specific for the current dbSearch, the TT entries are then removed, unless proved to another state.
 * -	For the previous point, boards with a state not open, in case, are added to TT as open, so they are not mistaken
 * 		in the combination stage (for implementation, because of the return type, boolean, of the functions).
 * 
 */
public class DbSearch {
	
	//#region CONSTANTS
		protected byte MY_PLAYER;
		private static final int MAX_THREAT_SEQUENCES = 10;
	//#endregion CONSTANTS

	public BoardBitDb board;

	// time / memory
	protected long timer_start;						//turn start (milliseconds)
	protected long timer_duration;					//time (millisecs) at which to stop timer
	private static final float TIMER_RATIO = 0.9f;	// see isTimeEnded() implementation
	private Runtime runtime;

	// VARIABLES FOR A DB-SEARCH EXECUTION
	protected int found_win_sequences;
	protected DbNode win_node;
	protected boolean[][] GOAL_SQUARES;		// used for defensive search.


	public DbSearch() {

		runtime = Runtime.getRuntime();
	}
	

	public void init(int M, int N, int X, boolean first) {
		
		MY_PLAYER	= CellState.P1;
		BoardBitDb.MY_PLAYER = MY_PLAYER;
		
		BoardBitDb.TT	= new TranspositionTable<TTElementBool, Key>(M, N, TTElementBool.getTable());
		
		GOAL_SQUARES = new boolean[M][N];	// initialized to false
	}

	

	/**
	 * 
	 * @param B
	 * @param root_pn
	 * @param time_remaining
	 * @return a DbSearchResult structure, filled as follows:  
	 * 1.	if found a winning sequence, winning_col is the first winning move,
	 * 		and related_squares_by_col contains, for each column j, the number of squares related to the winning sequence, in column j;
	 * 2.	otherwise, it's null.
	 */
	public DbSearchResult selectColumn(BoardBit B, TTPnNode root_pn, long time_remaining, byte player, byte max_tier) {
		
		DbNode root = null;;

		// timer
		timer_start	= System.currentTimeMillis();
		timer_duration	= timer_start + time_remaining;
		
		// update own board instance
		board = new BoardBitDb(B, player);
		
		board.findAllAlignments(Operators.MAX_TIER, true, "selCol_");
		
		// db init
		root = createRoot(board);
		win_node 	= null;
		found_win_sequences = 0;
		
		// recursive call for each possible move
		visit(root, true, max_tier);
		root = null;

		if(foundWin())
			return getReturnValue(player);

		return null;
	}
	
	/**
	 * Complexity: O(board.getThreatCounts)
	 * <p>	= O(12N**2)
	 */
	public int[] getThreatCounts(BoardBit B, byte player) {

		board = new BoardBitDb(B, player);
		return board.getThreatCounts(player);
	}

	//#region ALGORITHM

		/**
		 * Complexity: 
		 * 		iteration: O(addDependencyStage + addCombinationStage + lastDependency.clear + lastCombination.clear)
		 * 				= O(addDependencyStage + addCombinationStage + lastDependency.length + lastCombination.length)
		 * 
		 * 			case rooot.applicable_threats==0:
		 * 				= O( 6XN + lastDependency.length + lastCombination.length)
		 * 
		 * 			case rooot.applicable_threats>0:
		 * 				= O(addDependencyStage + addCombinationStage)
		 * 				= O(addDependencyStage + addCombinationStage)
		 * 
		 *		  		case attacking (betha):
		 * 					O(	lastCombination.length *
		 * 							* {
		 * 								[P(!end_game)* node.applicable_threats_n**2 * 27X**2] +
		 * 								+ P(endgame) * O(visitGlobalDefenses(node))
		 * 							} 			-- [for each node in lastCombination]
		 * 						+ lastDependency.length *
		 * 							* (A.marked_threats.length + 9N + 4 B.marked_threats.length(X + A.avg_threats_per_dir_per_line) + 8X A.added_threats.length
		 * 							)			-- [for each A,B in lastDependency]
		 * 					)
		 * 			case !attacking (betha):
		 * 					O(	lastCombination.length *
		 * 							* [
		 * 								P(!end_game) * node.applicable_threats_n**2 * (27X**2 )
		 * 							]			-- [for each node in lastCombination]
		 * 						+ lastDependency.length *
		 * 							* (A.marked_threats.length + 9N + 4 B.marked_threats.length(X + A.avg_threats_per_dir_per_line) + 8X A.added_threats.length
		 * 							)			-- [for each A,B in lastDependency]
		 * 					)
		 * 
		 * 
		 *
		 * 					notes: combinationStage is not executed if lastCombination.length==0, i.e. if (end_game || applicable_threats==0)
		 * 
		 * @param root
		 * @param attacker
		 * @param attacking
		 * @param max_tier
		 * @return true if the visit was successful, i.e. reached a goal state for the attacker.
		 */
		protected boolean visit(DbNode root, boolean attacking, int max_tier) {

			// init dependency and combination lists
			LinkedList<DbNode> lastDependency = new LinkedList<DbNode>(), lastCombination = new LinkedList<DbNode>();
			initLastCombination(root, lastCombination);

			/* Heuristic: only for attacker, stop after visiting a certain number of possible winning sequences.
			*/
			boolean found_goal_state = false;
			while(	!isTimeEnded() && isTreeChanged(lastCombination)
					&& !found_goal_state
					&& (!attacking || found_win_sequences < MAX_THREAT_SEQUENCES)
			) {
				// start dependency stage
				lastDependency.clear();
				
				// HEURISTIC: only for attacker, only search for threats of tier < max tier found in defenses
				int max_tier_t = attacking? max_tier : root.getMaxTier();
				if(addDependencyStage(lastDependency, lastCombination, root, attacking, max_tier_t))	//uses lastCombination, fills lastDependency
					found_goal_state = true;
					
				// START COMBINATIO STAGE
				if((attacking && !foundWin()) || (!attacking && !found_goal_state))
				{
					lastCombination.clear();
					
					addCombinationStage(lastDependency, lastCombination, root, attacking);				//uses lasdtDependency, fills lastCombination
				}
			}

			return found_goal_state;

		}

		/**
		 * @param root
		 * @param attacker
		 * @return true if found a defense (i.e. threat sequence was not winning)
		 */
		private boolean visitGlobalDefense(DbNode possible_win, DbNode root) {

			//add each combination of attacker's made threats to each dependency node
			DbNode new_root			= createDefensiveRoot(possible_win.board.getMarkedThreats(), root);
			int first_threat_tier	= new_root.getMaxTier();

			//visit for defender
			markGoalSquares(possible_win.board.getMarkedThreats(), true);
			//won for defender (=draw or win for defender)
			boolean defended = visit(new_root, false, first_threat_tier);
			markGoalSquares(possible_win.board.getMarkedThreats(), false);

			if(!defended)
				win_node = possible_win;

			return defended;
		}

		/**
		 * Complexity:
		 * 			O(addDependentChildren * lastCombination.length)
		 * 			= lastCombination.length *
		 * 				( P(!end_game) * {[P(applicable_threats_n==0) * O(6XN)] + [P(applicable_threats_n>0) * O( node.applicable_threats_n**2 * (27X**2 ))]} +
		 * 				+ {P(endgame) * P(attacking) * O(visitGlobalDefenses)}
		 * 				)
		 * 		case attacking:
		 * 				O(lastCombination.length *
		 * 				( P(!end_game) * {[P(applicable_threats_n==0) * O(6XN)] + [P(applicable_threats_n>0) * O( node.applicable_threats_n**2 * (27X**2 ))]} +
		 * 				+ {P(endgame) P(attacking) * O(visitGlobalDefenses)}
		 * 				))
		 * 		case !attacking:
		 * 				O(lastCombination.length *
		 * 				P(!end_game) * {[P(applicable_threats_n==0) * O(6XN)] + [P(applicable_threats_n>0) * O( node.applicable_threats_n**2 * (27X**2 ))]}
		 * 				)
		 * 				note: with !attacking, goal_squares and max_tier could reduce iterations number
		 * 
		 * @param attacker
		 * @param attacking
		 * @param lastDependency
		 * @param lastCombination
		 * @param root
		 * @param max_tier
		 * @return true if reached a goal state for the current player.
		 */
		private boolean addDependencyStage(LinkedList<DbNode> lastDependency, LinkedList<DbNode> lastCombination, DbNode root, boolean attacking, int max_tier) {

			boolean found_sequence = false;
			ListIterator<DbNode> it = lastCombination.listIterator();
			
			while (
				!isTimeEnded() && it.hasNext()
				&& (!attacking || found_win_sequences < MAX_THREAT_SEQUENCES)
				&& ((attacking && !foundWin()) || (!attacking && !found_sequence))
			) {
				DbNode node = it.next();

				found_sequence = addDependentChildren(lastDependency, node, root, attacking, max_tier);
			}
			return found_sequence;
				
		}
		
		/**
		 * (see notes for findAllCombinationNodes() and class notes)
		 * Complexity: 
		 * 		= lastDependency.length * O(findAllCombinationNodes) + O(removeCombinationTTEntries)
		 * 		= lastDependency.length * O( A.marked_threats.length + N**2 + 4 B.marked_threats.length(X + A.avg_threats_per_dir_per_line) + 432X**2 A.added_threats.length ) + O(lastCombination.length)
		 * 		= lastDependency.length * O( A.marked_threats.length + N**2 + 4 B.marked_threats.length(X + A.avg_threats_per_dir_per_line) + 432X**2 A.added_threats.length )
		 * 		= O(lastDependency.length * (A.marked_threats.length + N**2 + 4 B.marked_threats.length(X + A.avg_threats_per_dir_per_line) + 432X**2 A.added_threats.length )
		 * 
		 * -	betha:
		 * 			= O(lastDependency.length * (A.marked_threats.length + 9N + 4X B.marked_threats.length + 4 B.marked_threats.length A.avg_threats_per_dir_per_line + 8X A.added_threats.length )
		 * 
		 * @param root
		 * @param attacker
		 * @param attacking
		 * @param lastDependency
		 * @param lastCombination
		 * @return only return true if the dbSearch should end, because a checked and won node was found in the TT.
		 */
		private void addCombinationStage(LinkedList<DbNode> lastDependency, LinkedList<DbNode> lastCombination, DbNode root, boolean attacking) {
			
			ListIterator<DbNode> it = lastDependency.listIterator();
			while (
				!isTimeEnded() && it.hasNext()
				&& (!attacking || found_win_sequences < MAX_THREAT_SEQUENCES)
			) {
				DbNode node = it.next();
				
				findAllCombinationNodes(node, root, attacking, lastCombination, root);
			}

			removeCombinationTTEntries(lastCombination);
		}

		/**
		 * Complexity:
		 * 		iteration: P(!end_game) * [O(getApplicableOperators) + O(applicable_threats_n * addDependantChild)] + P(endgame) * O(visitGlobalDefensive)
		 * 				= P(!end_game) * {[P(applicable_threats_n==0) * O(6XN)] + [P(applicable_threats_n>0) * O( node.applicable_threats_n**2 * (27X**2 ))]} +
		 * 				+ {P(endgame) P(attacking) * O(visitGlobalDefensive) + P(!attacking) * O(1) }
		 * 				= P(!end_game) * {[P(applicable_threats_n==0) * O(6XN)] + [P(applicable_threats_n>0) * O( node.applicable_threats_n**2 * (27X**2 ))]} +
		 * 				+ {P(endgame) P(attacking) * O(visitGlobalDefensive)}
		 * 		case !end_game: O( 3X(M+N)	+ node.applicable_threats_n*(27X**2 + 4X + 64 + newChild.applicable_threats_n) )
		 * 						= O( 3X2N	+ node.applicable_threats_n*(27X**2 + 4X + 64 + newChild.children_n) )
		 * 						= O( 6XN	+ node.applicable_threats_n*(27X**2 + 4X + 64 + newChild.children_n) )
		 * 			case applicable_threats_n=0: O(6XN)
		 * 			case applicable_threats_n>0: O( node.applicable_threats_n*(27X**2 + 4X + 64 + newChild.applicable_threats_n) )
		 * 										= O( node.applicable_threats_n**2 * (27X**2 ))
		 * 										= O( node.applicable_threats_n**2 * (27X**2 ))
		 * 										, assuming similar number of applicable threats.
		 * 		case end_game: O(visitGlobalDefensive)
		 * 						if attacking, else O(1)
		 * 
		 * 		, with P(X)=probability of X
		 * @param lastDependency
		 * @param node
		 * @param root
		 * @param attacker
		 * @param attacking
		 * @param max_tier
		 * @return true if reached a goal state for the current player.
		 */
		private boolean addDependentChildren(LinkedList<DbNode> lastDependency, DbNode node, DbNode root, boolean attacking, int max_tier) {

			byte state = node.board.gameState();
			if(state == GameState.OPEN)
			{
				boolean found_sequence = false;
				//LinkedList<CXCell[]> applicableOperators = getApplicableOperators(node, MAX_CHILDREN, my_attacker);
				ThreatsByRank applicableOperators = node.board.getApplicableOperators(max_tier);

				for(LinkedList<ThreatCells> tier : applicableOperators) {
					if(tier != null)
					{
						for(ThreatCells threat : tier) {

							int atk_index = 0;
							//stops either after checking all threats, or if found a win/defense (for defended it is just any possible winning sequence)
							while( ((attacking && !foundWin()) || (!attacking && !found_sequence))
								&& ((atk_index = threat.nextAtk(atk_index)) != -1)
							) {
								if(isTimeEnded())
									return found_sequence;

								//if a goal square is marked, returns true, as goal squares are only used for defensive search, where only score matters
								MovePair atk_cell = threat.related[atk_index];

								if(GOAL_SQUARES[atk_cell.i][atk_cell.j]) {
									addDependentChild(node, threat, atk_index, lastDependency);
									// don't add to TT, as it's only for defense
									return true;
								}
								else {
									DbNode newChild = addDependentChild(node, threat, atk_index, lastDependency);

									if(addDependentChildren(lastDependency, newChild, root, attacking, max_tier))
										found_sequence = true;

									if(foundWin() || (attacking && found_win_sequences >= MAX_THREAT_SEQUENCES)) return found_sequence;
									else atk_index++;
								}
							}
						}
					}
				}
				return found_sequence;
			}
			else {
				// consider p2's wins first (e.g. in case a threat creates a win for both)
				if(state != Auxiliary.cellState2winState(root.board.attacker)) {
					return false;
				}
				else {
					if(attacking) {
						found_win_sequences++;
						visitGlobalDefense(node, root);
					}
					return true;
				}
			}

		}

		/**
		 * Fill the `lastCombination` list. It will be checked for defenses (rare cases) in the next dependency stage.
		 * Note that checking foundWin() wouldn't work for defensive visits
		 * (where of course you haven't found a win yet, but you are proving one, and a defense is not a win).
		 * 
		 * Complexity:
		 * 		iteration: O(validCombinationWith) + O(addCombinationChild)
		 * 			no mc:
		 * 				O( N + 4X B.marked_threats.length + 4 B.marked_threats.length A.avg_threats_per_dir_per_line + 432 X**2 added_threats.length )
		 * 
		 * 			betha:
		 * 				O( 9N + 4X B.marked_threats.length + 4 B.marked_threats.length A.avg_threats_per_dir_per_line + 8X A.added_threats.length )
		 * @param partner fixed node for combination
		 * @param node iterating node for combination
		 * @param attacking
		 * @param lastCombination
		 * @param root
		 */
		private void findAllCombinationNodes(DbNode partner, DbNode node, boolean attacking, LinkedList<DbNode> lastCombination, DbNode root) {
			
			if(
				// interrupt db
				node == null || (attacking && found_win_sequences >= MAX_THREAT_SEQUENCES) || isTimeEnded()
				/* Partner's and node's state is always open (or it would have been checked earlier, when created in dependency stage).
				* However a combined child could be not in open state: then it would be checked in the next dependency stage for defenses.
				*/
				|| node.board.gameState() != GameState.OPEN
				|| partner.validCombinationWith(node) != BoardsRelation.USEFUL
				//doesn't check if isDependencyNode() : also combinations of combination nodes could result in alignments
			)
				return;

			//create combination with A's board (copied)
			addCombinationChild(partner, node, lastCombination, root, attacking);
			findAllCombinationNodes(partner, node.getFirstChild(), attacking, lastCombination, root);
			findAllCombinationNodes(partner, node.getSibling(), attacking, lastCombination, root);
		}
	
	//#endregion ALGORITHM


	//#region CREATE

		
		protected DbNode createRoot(BoardBitDb B) {
			return DbNode.copy(B, true, Operators.MAX_TIER, true);
		}
	
		/**
		 * Complexity: O(DbNode.copy )
		 * 
		 * 	 * 		with mc: O(3M + 10N + B.marked_threats.length + MN) = O(B.marked_threats.length + N**2 + 13N)
	 * 		no mc: O(3M + 10N + B.marked_threats.length) = O(B.marked_threats.length + 13N)
		 * @param root
		 * @param athreats
		 * @param attacker
		 * @return
		 */
		private DbNode createDefensiveRoot(LinkedList<ThreatApplied> athreats, DbNode root) {

			ListIterator<ThreatApplied> it = athreats.listIterator();
			ThreatApplied athreat = null, athreat_prev = null;

			//create defenisve root copying current root, using opponent as player and marking only the move made by the current attacker in the first threat
			byte max_tier	= (byte)(Operators.tier_from_code(athreats.getFirst().threat.type) - 1);		// only look for threats better than mine
			byte attacker	= root.board.attacker;
			DbNode def_root	= DbNode.copy(root.board, true, max_tier, false);
			def_root.board.setAttacker(Auxiliary.opponent(attacker));
			def_root.board.findAllAlignments(max_tier, true, "defRoot_");

			//add a node for each threat, each node child/dependant from the previous one
			DbNode prev, node = def_root;
			while(it.hasNext()) {

				athreat = it.next();
				prev = node;
				prev.board.mark(athreat.threat.related[athreat.related_index], attacker);

				/* in all cases, except for the last threat, which is winning.
				Note that even a threat of tier > 1 could win by mistake: in such case, the problem is that it also has defensive moves;
				however these won't be added thanks to this condition, but will be ignored.
				*/
				if(it.hasNext()) {
					
					// the last node doesn't have any defensive squares, as it's a win.
					if(athreat_prev != null && athreat_prev.threat.related.length > 1)
						prev.board.checkAlignments(athreat_prev.threat.getDefensive(athreat_prev.related_index), prev.getMaxTier(), "createDefRoot");
					
					athreat_prev = athreat;
					
					BoardBitDb node_board = prev.board.getDependant(athreat.threat, athreat.related_index, USE.DEF, prev.getMaxTier(), false);
					node = new DbNode(node_board, true, prev.getMaxTier());
					prev.addChild(node);
				}
			}
			
			return def_root;
		}

		/**
		 * Complexity:  O(27X**2 + 4X + 64) + O(node.applicable_threats_n)
		 * sets child's game_state if entry exists in TT
		 */
				/**
		 * sets child's game_state if entry exists in TT.
		 * Complexity: 
		 * 		O(node.getDependant) + O(node.children_n)
		 * 		= O(64 + 4X + CheckAlignments) + O(node.applicable_threats_n)
		 * 		= O(64 + 4X + 18(2X+second-first)**2 ) + O(node.applicable_threats_n)
		 * 		= O(64 + 4X + 18(2X+X)**2 ) + O(node.applicable_threats_n)
		 * 		= O(64 + 4X + 27X**2 ) + O(node.applicable_threats_n)
		 * 		= O(27X**2 + 4X + 64) + O(node.applicable_threats_n)
		 * @param first
		 */
		protected DbNode addDependentChild(DbNode node, ThreatCells threat, int atk, LinkedList<DbNode> lastDependency) {
			
			BoardBitDb new_board	= node.board.getDependant(threat, atk, USE.BTH, node.getMaxTier(), true);
			DbNode newChild 		= new DbNode(new_board, false, node.getMaxTier());

			node.addChild(newChild);
			lastDependency.add(newChild);

			return newChild;
		}
		/**
		 * Adding the child to both parents is useless, and also would create problems with infinite recursion/repetitions.
		 * Only creates the child if the board wasn't already obtained by another combination in the same stage, of the same dbSearch (using TT, see class notes).
		 * 
		 * Complexity:
		 * 		if !added: O(A.getCombined(B))
		 *				= O(N**2 + 9N + 4X B.marked_threats.length + 4 B.marked_threats.length avg_threats_per_dir_per_line + 288X**2 added_threats.length )
		 * 		if added: O(A.getCombined(B)) + O(hasAlignments) + O(A.addChild)
		 *				= O(N**2 + 9N + 4X B.marked_threats.length + 4 B.marked_threats.length avg_threats_per_dir_per_line + 288X**2 added_threats.length + 6N + threats )
		 *				= O(N**2 + 15N + 4X B.marked_threats.length + 4 B.marked_threats.length avg_threats_per_dir_per_line + 288X**2 added_threats.length )
		 * 
		 * 		anyway: 
		 *				= O(N**2 + N + 4X B.marked_threats.length	+ 4 B.marked_threats.length avg_threats_per_dir_per_line + 288X**2 added_threats.length )
		 *			no mc (worst case):
		 *				= O(		N + 4X B.marked_threats.length	+ 4 B.marked_threats.length avg_threats_per_dir_per_line + 288X**2 added_threats.length )
		 *
		 *		betha:
		 *		 		= O(	   9N + 4X B.marked_threats.length	+ 4 B.marked_threats.length avg_threats_per_dir_per_line + 8X A.added_threats.length ) 
		 * 		
		 * 		note: usually few children.
		 * 
		 * = O(marked_threats.length + N**2) + O(B.marked_threats.length * (16X * avg_threats_per_dir_per_line) )
		 * 
		 */
		private void addCombinationChild(DbNode A, DbNode B, LinkedList<DbNode> lastCombination, DbNode root, boolean attacking) {

			int max_threat					= Math.min(A.getMaxTier(), B.getMaxTier());
			BoardBitDb new_board			= A.board.getCombined(B.board, max_threat);
			DbNode new_child				= null;
			TTElementBool entry = new_board.getEntry();

			// if already analyzed and saved in TT
			if(entry != null)
				return;
				
			//only create node if has threats (to continue visit)
			if(new_board.hasAlignments()) {
				new_child = new DbNode(new_board, true, (byte)max_threat);
				//only add child to tree and list if doesn't already exist, has threats and is not in ended state
				A.addChild(new_child);
				// only add to one parent, not to visit twice (and make the tree grow exponentially)
				lastCombination.add(new_child);

				// if not present in TT for attacker
				new_board.addEntry(new TTElementBool(new_board.hash, 1));
			}
		}


		/**
		 * Add to lastCombination all node's descendants.
		 * Complexity: O(children_n)
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

		/**
		 * Complexity: O(lastCombination.length)
		 * @param lastCombination
		 * @param attacker
		 */
		private void removeCombinationTTEntries(LinkedList<DbNode> lastCombination) {

			/* remmove TT "open" entries from lastCombination
			*/
			ListIterator<DbNode> it = lastCombination.listIterator();

			while(it.hasNext()) {
				BoardBitDb board = it.next().board;
				TTElementBool entry = board.getEntry();
				
				if(entry != null && entry.val == 1)
					board.removeEntry();
			}
		}
	
		/**
		 * Complexity:
		 * 		worst: O(4*athreats.length),
		 * 		with 4 max threat length.
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

		/**
		 * Complexity: O(1)
		 * @return
		 */
		protected boolean foundWin() {
			return win_node != null;
		}
	
		/**
		 * Complexity: O(N)
		 */
		protected DbSearchResult getReturnValue(byte player) {

			// the winning move is the player's move in the first threat in the sequence
			ThreatApplied winning_threat = win_node.board.markedThreats.getFirst();
			int winning_col = winning_threat.threat.related[winning_threat.related_index].j;
			int[]	related_squares_by_col;
			
			/* fill the related_squares_by_column with the number of newly made moves for each column
			*/
			related_squares_by_col = new int[BoardBitDb.N];
			for(int j = 0; j < BoardBit.N; j++)
				related_squares_by_col[j] = win_node.board.free[j] - board.free[j];
			
			return new DbSearchResult(winning_col, related_squares_by_col, win_node.board.markedThreats.size());
		}
		
		/**
		 * Complexity: O(1)
		 * @return true if it's time to end the turn
		 */
		private boolean isTimeEnded() {
			return (System.currentTimeMillis() - timer_start) >= timer_duration * TIMER_RATIO;
		}

		/**
		 * the tree is changed if either lastdCombination o lastDependency are not empty;
		 * however, dependency nodes are created from other dependency nodes only in the same level,
		 * so such iteration would be useless.
		 * Complexity: O(1)
		*/
		private boolean isTreeChanged(LinkedList<DbNode> lastCombination) {
			return lastCombination.size() > 0;
		}

	//#endregion HELPER

}
