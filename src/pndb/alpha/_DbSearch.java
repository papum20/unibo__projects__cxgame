package pndb.alpha;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;

import pndb.alpha.Operators.ThreatsByRank;
import pndb.alpha.threats.ThreatApplied;
import pndb.alpha.threats.ThreatCells;
import pndb.alpha.threats.ThreatCells.USE;
import pndb.constants.Auxiliary;
import pndb.constants.GameState;
import pndb.constants.MovePair;
import pndb.constants.Constants.BoardsRelation;
import pndb.tt.TranspositionElementEntry;
import pndb.tt.TranspositionTable;



/**
 * note:
 * -	never unmarks, as always creates a copy of a board.
 * -	TT: always used with MY_PLAYER = 0, YOUR = 1 (for state).
 * -	Combination stage uses TT with open states, in order to only search for already done combinations once.
 * 		However, being this specific for the current dbSearch, the TT entries are then removed, unless proved to another state.
 * -	For the previous point, boards with a state not open, in case, are added to TT as open, so they are not mistaken
 * 		in the combination stage (for implementation, because of the return type, boolean, of the functions).
 */
public abstract class _DbSearch<RES, BB extends _BoardBit<BB>, B extends IBoardBitDb<B, BB>, NODE extends _DbNode<NODE,BB,B>> extends IDbSearch<RES> {
	
	//#region CONSTANTS

		protected byte MY_PLAYER;
		private final int MAX_THREAT_SEQUENCES = 10;

		private final NODE NODE_INSTANCE;

	//#endregion CONSTANTS

	// time / memory
	protected long timer_start;						//turn start (milliseconds)
	protected long timer_end;						//time (millisecs) at which to stop timer
	private static final float TIMER_RATIO = 0.9f;	// see isTimeEnded() implementation
	private Runtime runtime;


	protected int M, N;
	public B board;
	protected TranspositionTable TT;

	// VARIABLES FOR A DB-SEARCH EXECUTION
	protected int found_win_sequences;
	protected NODE win_node;
	protected boolean[][] GOAL_SQUARES;		// used for defensive search.
	
	// DEBUG
	protected final boolean DEBUG_ON			= false;
	private final boolean DEBUG_TIME			= false;
	protected final boolean DEBUG_PRINT			= false;
	private final boolean DEBUG_ONLY_FOUND_SEQ	= false;
	protected int counter			= 0;
	protected FileWriter file		= null;
	private int DEBUG_CODE_MAX	= 999999999;
	protected String log;
	private long ms;
	private int visit_loops_n;
	private String indent;




	public _DbSearch(NODE node_instance) {

		runtime = Runtime.getRuntime();
		NODE_INSTANCE = node_instance;
	}
	

	public abstract void init(int M, int N, int X, boolean first);

	
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
	public abstract RES selectColumn(BoardBit B, PnNode root_pn, long time_remaining, byte player);

	public abstract int[] getThreatCounts(BoardBit B, byte player);


		//#region ALGORITHM

		/**
		 * 
		 * @param root
		 * @param attacker
		 * @param attacking
		 * @param max_tier
		 * @return true if the visit was successful, i.e. reached a goal state for the attacker.
		 */
		protected boolean visit(NODE root, byte attacker, boolean attacking, int max_tier) throws IOException {

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
			LinkedList<NODE> lastDependency = new LinkedList<NODE>(), lastCombination = new LinkedList<NODE>();
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
				log = "added dependency";
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
		private boolean visitGlobalDefense(NODE possible_win, NODE root, byte attacker) throws IOException {

			//DEBUG
			if(DEBUG_ON) file.write("\t\t\t\tWIN:\n" + possible_win.board.printString(1) + "\t\t\t\t-----\n");

			//add each combination of attacker's made threats to each dependency node
			NODE new_root			= createDefensiveRoot(root, possible_win.board.getMarkedThreats(), attacker);
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
		 * 
		 * @param attacker
		 * @param attacking
		 * @param lastDependency
		 * @param lastCombination
		 * @param root
		 * @param max_tier
		 * @return true if reached a goal state for the current player.
		 */
		private boolean addDependencyStage(byte attacker, boolean attacking, LinkedList<NODE> lastDependency, LinkedList<NODE> lastCombination, NODE root, int max_tier) throws IOException {

			// debug
			log += "depStage\n";

			boolean found_sequence = false;
			ListIterator<NODE> it = lastCombination.listIterator();
			
			while (
				!isTimeEnded() && it.hasNext()
				&& (!attacking || found_win_sequences < MAX_THREAT_SEQUENCES)
				&& ((attacking && !foundWin()) || (!attacking && !found_sequence))
			) {
				NODE node = it.next();

				// debug
				if(DEBUG_ON) file.write(	indent + "DEPENDENCY: parent: \n" + node.board.printString(node.board.getMC_n()) + indent + "children: \n" +
								((node.board.getMC_n() == 0 || node.board.getMarkedCell(node.board.getMC_n()-1) == null) ? "no MC\n" :
									(node.board.getMarkedCell(node.board.getMC_n()-1).i + " " + node.board.getMarkedCell(node.board.getMC_n()-1).j + " " + node.board.getMarkedCell(node.board.getMC_n()-1).state + "\n")));
				
				found_sequence = addDependentChildren(node, attacker, attacking, 1, lastDependency, root, max_tier);
			}
			return found_sequence;
				
		}
		
		/**
		 * (see notes for findAllCombinationNodes() and class notes)
		 * @param root
		 * @param attacker
		 * @param attacking
		 * @param lastDependency
		 * @param lastCombination
		 * @return only return true if the dbSearch should end, because a checked and won node was found in the TT.
		 */
		private boolean addCombinationStage(NODE root, byte attacker, boolean attacking, LinkedList<NODE> lastDependency, LinkedList<NODE> lastCombination) throws IOException {
			
			// debug
			log += "combStage\n";

			ListIterator<NODE> it = lastDependency.listIterator();
			boolean found_win = false;

			while (
				!isTimeEnded() && it.hasNext()
				&& (!attacking || found_win_sequences < MAX_THREAT_SEQUENCES)
				&& !found_win
			) {
				NODE node = it.next();
				
				// debug
				if(DEBUG_ON) file.write(indent + "parent: \n" + node.board.printString(node.board.getMC_n()) + indent + "children: \n");
				
				found_win = findAllCombinationNodes(node, root, attacker, attacking, lastCombination, root);
			}

			removeCombinationTTEntries(lastCombination, attacker);

			return found_win;
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
		 * @return true if reached a goal state for the current player.
		 */
		private boolean addDependentChildren(NODE node, byte attacker, boolean attacking, int lev, LinkedList<NODE> lastDependency, NODE root, int max_tier) throws IOException {

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
								
								//if a goal square is marked, returns true, as goal squares are only used for defensive search, where only score matters
								MovePair atk_cell = threat.related[atk_index];
								if(GOAL_SQUARES[atk_cell.i][atk_cell.j]) {
									
									NODE newChild = addDependentChild(node, threat, atk_index, lastDependency, attacker);
									// don't add to TT, as it's only for defense
									
									// debug
									if(DEBUG_ON) file.write(indent + "-" + lev + "\t---\n" + newChild.board.printString(lev) + "MARKED GOAL SQUARE " + atk_cell + "\n");
									
									return true;
								}
								else {
									NODE newChild = addDependentChild(node, threat, atk_index, lastDependency, attacker);

									// debug
									if(DEBUG_ON) file.write(indent + "-" + lev + "\t---\n" + newChild.board.printString(lev) + indent + "---\n");

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
		 * @param partner fixed node for combination
		 * @param node iterating node for combination
		 * @param attacker
		 * @param attacking
		 * @param lastCombination
		 * @param root
		 * @return only return true if the dbSearch should end, because a checked and won node was found in the TT.
		 */
		private boolean findAllCombinationNodes(NODE partner, NODE node, byte attacker, boolean attacking, LinkedList<NODE> lastCombination, NODE root) throws IOException {
			
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

		protected abstract NODE createNode(B board, boolean is_combination, int max_tier);
	
		protected NODE createRoot(B B) {

			NODE root = NODE_INSTANCE.copy(board, true, Operators.TIER_MAX, true);
			//NodeBoard root = NodeBoard.copy(board, true, Operators.TIER_MAX, true);
			return root;
		}

		private NODE createDefensiveRoot(NODE root, LinkedList<ThreatApplied> athreats, byte attacker) throws IOException {

			// debug
			log += "createDef start\n";

			ListIterator<ThreatApplied> it = athreats.listIterator();
			ThreatApplied athreat = null, athreat_prev = null;

			//create defenisve root copying current root, using opponent as player and marking only the move made by the current attacker in the first threat
			byte max_tier	= (byte)(Operators.tier(athreats.getFirst().threat.type) - 1);		// only look for threats better than mine
			NODE def_root	= NODE_INSTANCE.copy(root.board, true, max_tier, false);
			def_root.board.setPlayer(Auxiliary.opponent(attacker));
			def_root.board.findAllAlignments(Auxiliary.opponent(attacker), max_tier, true, "defRoot_");

			
			// debug
			if(DEBUG_ON) file.write("MAX THREAT: " + max_tier + "\n");
			
			//add a node for each threat, each node child/dependant from the previous one
			NODE prev, node = def_root;
			while(it.hasNext()) {

				athreat = it.next();
				prev = node;
				prev.board.mark(athreat.threat.related[athreat.related_index], attacker);
				
				// related > 1 means there is at least 1 defensive move (bc there's always an attacker one)
				if(it.hasNext() || athreat.threat.related.length > 1) {
					
					// the last node doesn't have any defensive squares, as it's a win.
					if(athreat_prev != null && athreat_prev.threat.related.length > 1)
						prev.board.checkAlignments(athreat_prev.threat.getDefensive(athreat_prev.related_index), prev.getMaxTier(), "createDefRoot");
						
					athreat_prev = athreat;
					
					B node_board = prev.board.getDependant(athreat.threat, athreat.related_index, USE.DEF, prev.getMaxTier(), false);
					node = createNode(node_board, true, prev.getMaxTier());
					prev.addChild(node);
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
		 * sets child's game_state if entry exists in TT
		 */
		protected abstract NODE addDependentChild(NODE node, ThreatCells threat, int atk, LinkedList<NODE> lastDependency, byte attacker);

		/**
		 * (I hope) adding the child to both parents is useless, for now.
		 * Only creates the child if the board wasn't already obtained by another combination in the same stage, of the same dbSearch (using TT, see class notes).
		 * @return (see findAllCombinations())
		 */
		private boolean addCombinationChild(NODE A, NODE B, LinkedList<NODE> lastCombination, NODE root, byte attacker, boolean attacking) {

			// debug
			log += "addCombChild\n";
			
			int attacker_i					= Auxiliary.getPlayerBit(attacker);
			int max_threat					= Math.min(A.getMaxTier(), B.getMaxTier());
			B new_board						= A.board.getCombined(B.board, attacker, max_threat);
			NODE new_child				= null;
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
				new_child = createNode(new_board, true, (byte)max_threat);
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
		 * add to lastCombination all node's descendants
		 * @param node
		 * @param lastCombination
		 */
		private void initLastCombination(NODE node, LinkedList<NODE> lastCombination) {
			if(node != null) {
				lastCombination.addLast(node);
				initLastCombination(node.getSibling(), lastCombination);
				initLastCombination(node.getFirstChild(), lastCombination);
			}
		}
		
	//#endregion CREATE

	//#region GET_SET

		private ThreatsByRank getApplicableOperators(B board, byte attacker, int max_tier) {

			log += "start getApplicable Operators\n";

			return board.getApplicableOperators(attacker, max_tier);
		}

		private void removeCombinationTTEntries(LinkedList<NODE> lastCombination, byte attacker) {

			/* remmove TT "open" entries from lastCombination
			*/
			ListIterator<NODE> it = lastCombination.listIterator();
			int attacker_i = Auxiliary.getPlayerBit(attacker);

			while(it.hasNext()) {
				long hash = it.next().board.getHash();
				TranspositionElementEntry entry = TT.getState(hash);
				
				if(entry != null && entry.state[attacker_i] == GameState.OPEN)
					TT.removeState(hash, attacker_i);
			}
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
	
		protected abstract RES getReturnValue(byte player);
		
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
		private boolean isTreeChanged(LinkedList<NODE> lastCombination) {
			return lastCombination.size() > 0;
		}

		//#region DEBUG

			protected void printTime() {
				ms = System.currentTimeMillis() - ms;
				if(ms > 0) {
					System.out.println("pn, turn " + visit_loops_n + ", time select most proving: " + ms);
					System.out.println("..." + log + "\n...");
				}
				ms = System.currentTimeMillis();
			}

			protected int debugRandomCode() {
				return (int)(Math.random() * DEBUG_CODE_MAX);
			}
		
		//#endregion DEBUG
		
	//#endregion HELPER

}
