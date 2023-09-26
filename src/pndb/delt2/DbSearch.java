package pndb.delt2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;

import pndb.delt2.Operators.ThreatsByRank;
import pndb.delt2.threats.ThreatApplied;
import pndb.delt2.threats.ThreatCells;
import pndb.delt2.threats.ThreatCells.USE;
import pndb.constants.Auxiliary;
import pndb.constants.CellState;
import pndb.constants.GameState;
import pndb.constants.MovePair;
import pndb.constants.Constants.BoardsRelation;
import pndb.delt2.tt.TranspositionElementEntry;
import pndb.delt2.tt.TranspositionTable;



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
		private final int MAX_THREAT_SEQUENCES = 10;

	//#endregion CONSTANTS

	// time / memory
	protected long timer_start;						//turn start (milliseconds)
	protected long timer_end;						//time (millisecs) at which to stop timer
	private static final float TIMER_RATIO = 0.9f;	// see isTimeEnded() implementation
	private Runtime runtime;


	protected int M, N;
	public BoardBitDb board;
	protected TranspositionTable TT;

	// VARIABLES FOR A DB-SEARCH EXECUTION
	protected int found_win_sequences;
	protected DbNode win_node;
	protected boolean[][] GOAL_SQUARES;		// used for defensive search.
	
	// DEBUG
	protected final boolean DEBUG_ON			= false;
	private final boolean DEBUG_TIME			= false;
	protected final boolean DEBUG_PRINT			= false;
	private final boolean DEBUG_ONLY_FOUND_SEQ	= true;
	protected int counter			= 0;
	protected FileWriter file		= null;
	private int DEBUG_CODE_MAX		= 999999999;
	protected String log;
	private long ms;
	private int visit_loops_n;
	private String indent;




	public DbSearch() {

		runtime = Runtime.getRuntime();
	}
	

	public void init(int M, int N, int X, boolean first) {
		
		this.M = M;
		this.N = N;

		MY_PLAYER	= CellState.P1;
		BoardBitDb.MY_PLAYER = MY_PLAYER;
		
		board = new BoardBitDb(M, N, X);
		TT = new TranspositionTable(M, N);
		
		BoardBitDb.TT = TT;

		GOAL_SQUARES = new boolean[M][N];
		// initialized to false
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
	public DbSearchResult selectColumn(BoardBit B, PnNode root_pn, long time_remaining, byte player, byte max_tier) {
		
		// debug
		log = "__\ndbSearch\n";

		DbNode root = null;;

		try {
			
			// timer
			timer_start	= System.currentTimeMillis();
			timer_end	= timer_start + time_remaining;
			
			// update own board instance
			board = new BoardBitDb(B);
			board.setPlayer(player);
			
			board.findAllAlignments(player, Operators.MAX_TIER, true, "selCol_");
			
			// debug
			if(DEBUG_ON && board.hasAlignments(player)) {
				file = new FileWriter("debug/db1main/main" + (counter++) + "_" + debugRandomCode() + "_" + board.getMC_n() + ".txt");
				file.write("root board:\n" + board.printString(0) + board.printAlignmentsString(0));
				file.close();
			}
			
			// db init
			root = createRoot(board);
			win_node 	= null;
			found_win_sequences = 0;
			
			// recursive call for each possible move
			visit(root, player, true, max_tier);
			root = null;

			// debug
			if(DEBUG_ON) file.close();
			if(foundWin()) {
				if(DEBUG_PRINT)
					System.out.println("found win: " + foundWin() );
				log += "found win: " + foundWin() + "\n";
				log += "win node \n";
				log += win_node.board.printString(0);
			}

			if(foundWin())
				return getReturnValue(player);

			return null;

		} catch (IOException io) {
			return null;
		} catch (ArrayIndexOutOfBoundsException e) {
			if(root == null) System.out.println("root is null");
			else {
				root.board.print();
				root.board.printAlignments();
			}
			System.out.println(log + "\nout of bounds in db\n");
			if(DEBUG_ON) try {file.close();} catch(IOException io) {}
			throw e;
		} catch (Exception e) {
			if(root == null) System.out.println("root is null");
			else {
				root.board.print();
				root.board.printAlignments();
			}
			System.out.println(log + "\nany error in db\n");
			if(DEBUG_ON) try {file.close();} catch(IOException io) {}
			throw e;
		}

	}
	
	/**
	 * Complexity: O(board.getThreatCounts)
	 * <p>	= O(12N**2)
	 */
	public int[] getThreatCounts(BoardBit B, byte player) {

		board = new BoardBitDb(B);
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
		protected boolean visit(DbNode root, byte attacker, boolean attacking, int max_tier) throws IOException {

			// debug
			log = "dbVisit\n";
			String filename_current = "";
			boolean found_something = false;
			ms = System.currentTimeMillis();
			short level = attacking? (short)1 : (short)8;
			indent = "";
			for(int i = 0; i < level; i++) indent += "\t";
			if(DEBUG_ON) {
				if(!attacking) file.write("\t\t\t\t--------\tSTART OF DEFENSE\t--------\n");
				else {
					filename_current = "debug/db1/db" + (counter++) + "_" + debugRandomCode() + "_" + root.board.getMC_n() + "-" + (root.board.getMC_n() > 0 ? root.board.getMarkedCell(root.board.getMC_n()-1) : "_") + "-" + level + ".txt";
					file = new FileWriter(filename_current);
					file.write("attacker, attacking, maxtier = " + attacker + " " + attacking + " " + max_tier + "\n");

				}
			}

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
				
				// debug
				log += "dbVisit for " + attacker + ", attacking: " + attacking + "\n";
				if(DEBUG_ON) file.write(indent + "-------\tDEPENDENCY\t--------\n");
				if(DEBUG_TIME) printTime();
				
				// HEURISTIC: only for attacker, only search for threats of tier < max tier found in defenses
				int max_tier_t = attacking? max_tier : root.getMaxTier();
				if(addDependencyStage(attacker, attacking, lastDependency, lastCombination, root, max_tier_t))	//uses lastCombination, fills lastDependency
					found_goal_state = true;
					
				// debug
				if(DEBUG_TIME) printTime();
				log += "added dependency\n";
				if(!lastDependency.isEmpty()) found_something = true;
				if(DEBUG_ON) file.write(indent + "--------\tCOMBINATION\t--------\n");
				
				// START COMBINATIO STAGE
				if((attacking && !foundWin()) || (!attacking && !found_goal_state))
				{
					lastCombination.clear();
					
					// debug
					if(DEBUG_TIME) printTime();

					if(addCombinationStage(root, attacker, attacking, lastDependency, lastCombination))				//uses lasdtDependency, fills lastCombination
						found_goal_state = true;
					
					// debug
					if(DEBUG_TIME) printTime();
					if(!lastCombination.isEmpty()) found_something = true;
					if(DEBUG_ON) file.write(indent + "--------\tEND OF COMBINATION\t--------\n");
					
				}
				
				// DEBUG
				level++;
				visit_loops_n++;
				if(DEBUG_ON) {
					file.write("ATTACKING: " + (attacking? "ATTACKER":"DEFENDER") + "\n");
					file.write("FOUND SEQUENCE: " + found_goal_state + "\n");
					file.write("VISIT WON: " + foundWin() + "\n");
				}
			}

			// DEBUG
			log += "end of loop\n";
			if(DEBUG_ON) {
				if(!attacking) {
					file.write("\t\t\t\t--------\tEND OF DEFENSE\t--------\n");
				} else {
					file.close();
					if(!found_something) {
						File todel = new File(filename_current);
						if(DEBUG_ONLY_FOUND_SEQ && !found_goal_state)
							todel.delete();
					}
				}
			}
			
			return found_goal_state;

		}

		/**
		 * @param root
		 * @param attacker
		 * @return true if found a defense (i.e. threat sequence was not winning)
		 */
		private boolean visitGlobalDefense(DbNode possible_win, DbNode root, byte attacker) throws IOException {

			//DEBUG
			if(DEBUG_ON) file.write("\t\t\t\tWIN:\n" + possible_win.board.printString(1) + "\t\t\t\t-----\n");
			
			//add each combination of attacker's made threats to each dependency node
			DbNode new_root			= createDefensiveRoot(root, possible_win.board.getMarkedThreats(), attacker);
			int first_threat_tier	= new_root.getMaxTier();

			//visit for defender
			markGoalSquares(possible_win.board.getMarkedThreats(), true);
			//won for defender (=draw or win for defender)
			boolean defended = visit(new_root, Auxiliary.opponent(attacker), false, first_threat_tier);
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
		private boolean addDependencyStage(byte attacker, boolean attacking, LinkedList<DbNode> lastDependency, LinkedList<DbNode> lastCombination, DbNode root, int max_tier) throws IOException {

			// debug
			log += "depStage\n";

			boolean found_sequence = false;
			ListIterator<DbNode> it = lastCombination.listIterator();
			
			while (
				!isTimeEnded() && it.hasNext()
				&& (!attacking || found_win_sequences < MAX_THREAT_SEQUENCES)
				&& ((attacking && !foundWin()) || (!attacking && !found_sequence))
			) {
				DbNode node = it.next();

				// debug
				if(DEBUG_ON) file.write(indent + "DEPENDENCY: parent: \n" + node.board.printString(node.board.getMC_n()) + node.board.printAlignmentsString(node.board.getMC_n()) + indent + "children: \n" +
								((node.board.getMC_n() == 0 || node.board.getMarkedCell(node.board.getMC_n()-1) == null) ? "no MC\n" :
									(node.board.getMarkedCell(node.board.getMC_n()-1).i + " " + node.board.getMarkedCell(node.board.getMC_n()-1).j + " " + node.board.getMarkedCell(node.board.getMC_n()-1).state + "\n")));
				
				found_sequence = addDependentChildren(node, attacker, attacking, 1, lastDependency, root, max_tier);
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
		private boolean addCombinationStage(DbNode root, byte attacker, boolean attacking, LinkedList<DbNode> lastDependency, LinkedList<DbNode> lastCombination) throws IOException {
			
			// debug
			log += "combStage\n";

			ListIterator<DbNode> it = lastDependency.listIterator();
			boolean found_win = false;

			while (
				!isTimeEnded() && it.hasNext()
				&& (!attacking || found_win_sequences < MAX_THREAT_SEQUENCES)
				&& !found_win
			) {
				DbNode node = it.next();
				
				// debug
				if(DEBUG_ON) file.write(indent + "parent: \n" + node.board.printString(node.board.getMC_n()) + indent + "children: \n");
				
				found_win = findAllCombinationNodes(node, root, attacker, attacking, lastCombination, root);
			}

			removeCombinationTTEntries(lastCombination, attacker);

			return found_win;
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
		 * @param node
		 * @param attacker
		 * @param attacking
		 * @param lev
		 * @param lastDependency
		 * @param root
		 * @param max_tier
		 * @return true if reached a goal state for the current player.
		 */
		private boolean addDependentChildren(DbNode node, byte attacker, boolean attacking, int lev, LinkedList<DbNode> lastDependency, DbNode root, int max_tier) throws IOException {

			log += "addDependentChildren\n";

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
							while( ((attacking && !foundWin()) || (!attacking && !found_sequence))
								&& ((atk_index = threat.nextAtk(atk_index)) != -1)
							) {
								if(isTimeEnded())
									return found_sequence;

								// debug
								if(DEBUG_ON) {
									file.write(indent + "\t\t\t" + threat.type + "\t" + atk_index + "\t");
									for(int i = 0; i < threat.related.length; i++) file.write(threat.related[i] + " " + threat.uses[i] + "\t");
									file.write("\n");
								}
								log += "applying type=" + threat.type + ", atk_idx=" + threat.nextAtk(0) + ": ";
								for(int i = 0; i < threat.related.length; i++) log += threat.related[i] + " ";
								log += "\n" + board.printString(1) + "\n" + board.printAlignmentsString(0) + "\n--\n";
								
								//if a goal square is marked, returns true, as goal squares are only used for defensive search, where only score matters
								MovePair atk_cell = threat.related[atk_index];
								if(GOAL_SQUARES[atk_cell.i][atk_cell.j]) {
									
									DbNode newChild = addDependentChild(node, threat, atk_index, lastDependency, attacker);
									// don't add to TT, as it's only for defense
									
									// debug
									if(DEBUG_ON) file.write(indent + "-" + lev + "\t---\n" + newChild.board.printString(lev) + "MARKED GOAL SQUARE " + atk_cell + "\n");
									
									return true;
								}
								else {
									DbNode newChild = addDependentChild(node, threat, atk_index, lastDependency, attacker);

									// debug
									if(DEBUG_ON) file.write(indent + "-" + lev + "\t---\n" + newChild.board.printString(lev) + newChild.board.printAlignmentsString(lev) + indent + "---\n");

									if(addDependentChildren(newChild, attacker, attacking, lev+1, lastDependency, root, max_tier))
										found_sequence = true;

									if(foundWin() || (attacking &&  found_win_sequences >= MAX_THREAT_SEQUENCES)) return found_sequence;
									else atk_index++;
								}
							}
						}
					}
				}
				return found_sequence;
			}
			else {
				
				// debug
				if(DEBUG_ON) file.write("STATE (dependency): " + state + "\n");

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
		 * @param attacker
		 * @param attacking
		 * @param lastCombination
		 * @param root
		 * @return only return true if the dbSearch should end, because a checked and won node was found in the TT.
		 */
		private boolean findAllCombinationNodes(DbNode partner, DbNode node, byte attacker, boolean attacking, LinkedList<DbNode> lastCombination, DbNode root) throws IOException {
			
			// debug
			log += "combNodes\n";
			
			if(node == null || (attacking && found_win_sequences >= MAX_THREAT_SEQUENCES) || isTimeEnded())
				return false;

			/* Partner's and node's state is always open (or it would have been checked earlier, when created in dependency stage).
				* However a combined child could be not in open state: then it would be checked in the next dependency stage for defenses.
				*/
			if(node.board.gameState() != GameState.OPEN) 
				return false;

			//doesn't check if isDependencyNode() : also combinations of combination nodes could result in alignments
			if(partner.validCombinationWith(node, attacker) != BoardsRelation.USEFUL) 
				return false;

			// DEBUG
			if(DEBUG_ON) {
				file.write(indent + "\t\tfirst parent: \n" + partner.board.printString(partner.board.getMC_n()));
				file.write("\n" + indent + "\t\tsecond parent: \n" + node.board.printString(node.board.getMC_n()) + "\n");
			}

			//create combination with A's board (copied)
			if(	addCombinationChild(partner, node, lastCombination, root, attacker, attacking)
				|| findAllCombinationNodes(partner, node.getFirstChild(), attacker, attacking, lastCombination, root)
				|| findAllCombinationNodes(partner, node.getSibling(), attacker, attacking, lastCombination, root)
			)
				return true;

			return false;
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
		 * @throws IOException
		 */
		private DbNode createDefensiveRoot(DbNode root, LinkedList<ThreatApplied> athreats, byte attacker) throws IOException {

			// debug
			log += "createDef start\n";

			ListIterator<ThreatApplied> it = athreats.listIterator();
			ThreatApplied athreat = null, athreat_prev = null;

			//create defenisve root copying current root, using opponent as player and marking only the move made by the current attacker in the first threat
			byte max_tier	= (byte)(Operators.tier(athreats.getFirst().threat.type) - 1);		// only look for threats better than mine
			DbNode def_root	= DbNode.copy(root.board, true, max_tier, false);
			def_root.board.setPlayer(Auxiliary.opponent(attacker));
			def_root.board.findAllAlignments(Auxiliary.opponent(attacker), max_tier, true, "defRoot_");

			
			// debug
			if(DEBUG_ON) file.write("MAX THREAT: " + max_tier + "\n");
			// debug
			if(DEBUG_ON) file.write("def_root before loop" + "\n" + def_root.board.printString(0) + def_root.board.printAlignmentsString(0) + "\n");
			
			//add a node for each threat, each node child/dependant from the previous one
			DbNode prev, node = def_root;
			while(it.hasNext()) {

				
				athreat = it.next();
				prev = node;
				prev.board.mark(athreat.threat.related[athreat.related_index], attacker);

				// debug
				if(DEBUG_ON) file.write("prev after mark atk" + "\n" + prev.board.printString(0) + prev.board.printAlignmentsString(0) + "\n");
				
				// related > 1 means there is at least 1 defensive move (bc there's always an attacker one)
				if(it.hasNext() || athreat.threat.related.length > 1) {
					
					// the last node doesn't have any defensive squares, as it's a win.
					if(athreat_prev != null && athreat_prev.threat.related.length > 1)
					prev.board.checkAlignments(athreat_prev.threat.getDefensive(athreat_prev.related_index), prev.getMaxTier(), "createDefRoot");
					
					athreat_prev = athreat;
					
					BoardBitDb node_board = prev.board.getDependant(athreat.threat, athreat.related_index, USE.DEF, prev.getMaxTier(), false);
					node = new DbNode(node_board, true, prev.getMaxTier());
					prev.addChild(node);

					// debug
					if(DEBUG_ON) file.write("node after mark def" + "\n" + node.board.printString(0) + node.board.printAlignmentsString(0) + "\n");
					
				}
				
				//DEBUG
				if(DEBUG_ON) {
					file.write("\t\t\t\t" + athreat.threat.related[athreat.related_index] + "\n" + prev.board.printString(prev.board.getMC_n()));
					for(MovePair m : athreat.threat.related) file.write("\t\t\t\t" + m + " ");
					file.write("\n");
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
		protected DbNode addDependentChild(DbNode node, ThreatCells threat, int atk, LinkedList<DbNode> lastDependency, byte attacker) {
			
			// debug
			log += "addDepChild\n";

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
		 * @return (see findAllCombinations())
		 */
		private boolean addCombinationChild(DbNode A, DbNode B, LinkedList<DbNode> lastCombination, DbNode root, byte attacker, boolean attacking) {

			// debug
			log += "addCombChild\n";
			
			int attacker_i					= Auxiliary.getPlayerBit(attacker);
			int max_threat					= Math.min(A.getMaxTier(), B.getMaxTier());
			BoardBitDb new_board						= A.board.getCombined(B.board, attacker, max_threat);
			DbNode new_child					= null;
			TranspositionElementEntry entry = TT.getState(new_board.getHash());

			// debug
			if(DEBUG_ON) new_board.printFile(file, new_board.getMC_n());

			// if already analyzed and saved in TT
			if(entry != null && entry.state[attacker_i] != GameState.NULL){
				if(entry.state[attacker_i] == Auxiliary.cellState2winState(attacker) )
					// already proved
					return true;
				else
					/* already combined,
					 * or proved draw/lost, so useless to re-analyze this board.
					 */
					return false;
				}
				
			//only create node if has threats (to continue visit)
			if(new_board.hasAlignments(attacker)) {
				new_child = new DbNode(new_board, true, (byte)max_threat);
				//only add child to tree and list if doesn't already exist, has threats and is not in ended state
				A.addChild(new_child);
				// only add to one parent, not to visit twice (and make the tree grow exponentially)
				lastCombination.add(new_child);

				// if not present in TT for attacker
				TT.setStateOrInsert(new_board.getHash(), GameState.OPEN, attacker_i);
			}

			return false;
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
		 * Complexity: O(3X(M+N)) = O(6XN)
		 * @param board
		 * @param attacker
		 * @param max_tier
		 * @return
		 */
		private ThreatsByRank getApplicableOperators(BoardBitDb board, byte attacker, int max_tier) {

			log += "start getApplicable Operators\n";

			return board.getApplicableOperators(attacker, max_tier);
		}

		/**
		 * Complexity: O(lastCombination.length)
		 * @param lastCombination
		 * @param attacker
		 */
		private void removeCombinationTTEntries(LinkedList<DbNode> lastCombination, byte attacker) {

			/* remmove TT "open" entries from lastCombination
			*/
			ListIterator<DbNode> it = lastCombination.listIterator();
			int attacker_i = Auxiliary.getPlayerBit(attacker);

			while(it.hasNext()) {
				long hash = it.next().board.getHash();
				TranspositionElementEntry entry = TT.getState(hash);
				
				if(entry != null && entry.state[attacker_i] == GameState.OPEN)
					TT.removeState(hash, attacker_i);
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

			int		winning_col;
			int[]	related_squares_by_col;
			
			// the winning move is the player's move in the first threat in the sequence
			ThreatApplied winning_threat = win_node.board.markedThreats.getFirst();
			winning_col = winning_threat.threat.related[winning_threat.related_index].j;
			
			/* fill the related_squares_by_column with the number of newly made moves for each column
			*/
			related_squares_by_col = new int[N];
			for(int j = 0; j < N; j++)
				related_squares_by_col[j] = win_node.board.free[j] - board.free[j];
			
			return new DbSearchResult(winning_col, related_squares_by_col, win_node.board.markedThreats.size());
		}
		
		/**
		 * Complexity: O(1)
		 * @return true if it's time to end the turn
		 */
		private boolean isTimeEnded() {
			return (System.currentTimeMillis() - timer_start) >= timer_end * TIMER_RATIO;
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

		//#region DEBUG

			protected void printTime() {
				ms = System.nanoTime() - ms;
				if(ms > 0) {
					System.out.println("pn, turn " + visit_loops_n + ", time select most proving: " + ms);
					System.out.println("..." + log + "\n...");
				}
				ms = System.nanoTime();
			}

			protected int debugRandomCode() {
				return (int)(Math.random() * DEBUG_CODE_MAX);
			}
		
		//#endregion DEBUG
		
	//#endregion HELPER

}
