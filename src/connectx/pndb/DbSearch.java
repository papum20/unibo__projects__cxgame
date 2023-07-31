package connectx.pndb;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;

import connectx.CXCell;
import connectx.CXGameState;
import connectx.pndb.BiList.BiNode;
import connectx.pndb.DbNode.BoardsRelation;
import connectx.pndb.Operators.ThreatsByRank;
import connectx.pndb.Operators.ThreatCells;
import connectx.pndb.Operators.USE;



/**
 * note:
 * -	never unmarks, as always creates a copy of a board.
 * -	TT: always used with MY_PLAYER = 0, YOUR = 1 (for state).
 * -	Combination stage uses TT with open states, in order to only search for already done combinations once.
 * 		However, being this specific for the current dbSearch, the TT entries are then removed, unless proved to another state.
 * -	For the previous point, boards with a state not open, in case, are added to TT as open, so they are not mistaken
 * 		in the combination stage (for implementation, because of the return type, boolean, of the functions).
 */
public class DbSearch {
	
	//#region CONSTANTS

	private byte MY_PLAYER;
	/*
	private CXCellState MY_CX_PLAYER;
	private CXCellState YOUR_CX_PLAYER;
	private CXGameState MY_WIN;
	private CXGameState YOUR_WIN;
	private byte YOUR_PLAYER;
	*/

	private final int MAX_THREAT_SEQUENCES = 10;

	//#endregion CONSTANTS

	// time / memory
	private long timer_start;						//turn start (milliseconds)
	private long timer_end;							//time (millisecs) at which to stop timer
	private static final float TIMER_RATIO = 0.9f;	// see isTimeEnded() implementation
	protected Runtime runtime;


	private int M, N;
	private BoardBitDb board;
	protected TranspositionTable TT;

	// VARIABLES FOR A DB-SEARCH EXECUTION
	private int found_win_sequences;
	private DbNode win_node;
	private boolean[][] GOAL_SQUARES;
	
	// DEBUG
	private final boolean DEBUG_ON = false;
	private final boolean DEBUG_PRINT = false;
	private final boolean DEBUG_ONLY_FOUND_SEQ = true;
	int counter = 0;
	FileWriter file = null;
	int debug_code;
	int DEBUG_CODE_MAX = 999999999;




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

		/*
		YOUR_PLAYER	= CellState.P2;

		MY_CX_PLAYER	= CXCellState.P1;
		YOUR_CX_PLAYER	= CXCellState.P2;
		//player_opponent = CXCellState.P2;
		MY_WIN		= CXGameState.WINP1;
		YOUR_WIN	= CXGameState.WINP2;
		//your_win = CXGameState.WINP2;
		*/
		/*
		if(first) {
			MY_CX_PLAYER	= CXCellState.P1;
			YOUR_CX_PLAYER	= CXCellState.P2;
			//player_opponent = CXCellState.P2;
			MY_WIN		= CXGameState.WINP1;
			YOUR_WIN	= CXGameState.WINP2;
			//your_win = CXGameState.WINP2;
		} else {
			MY_CX_PLAYER	= CXCellState.P2;
			YOUR_CX_PLAYER	= CXCellState.P1;
			//player_opponent = CXCellState.P1;
			MY_WIN		= CXGameState.WINP2;
			YOUR_WIN	= CXGameState.WINP1;
			//your_win = CXGameState.WINP1;
		}
		*/

	}

	
	/**
	 * 
	 * @param board_pn
	 * @param root_pn
	 * @param time_remaining
	 * @return a CXCell, containing the state of the winning player, on null if didn't find a sequence.
	 */
	public DbSearchResult selectColumn(BoardBit board_pn, PnNode root_pn, long time_remaining, byte player) {

		GOAL_SQUARES = new boolean[M][N];
		for(int i = 0; i < M; i++)
			for(int j = 0; j < N; j++) GOAL_SQUARES[i][j] = false;

		String log = "dbSearch\n";
		try {
			
			// timer
			timer_start	= System.currentTimeMillis();
			timer_end	= timer_start + time_remaining;

			// update own board instance
			board = new BoardBitDb(board_pn);
			board.setPlayer(player);

			board.findAllAlignments(player, Operators.TIER_MAX, "selCol_");
			//board.findAllAlignments(CellState.P2, Operators.TIER_MAX, "selCol_");
			//board.updateAlignments(last_move_pair, last_move.state);
			
			// debug
			if(DEBUG_ON) {
				try {
					debug_code = (int)(Math.random() * DEBUG_CODE_MAX);
					String filename_current = "debug/db1main/main" + (counter++) + "_" + debug_code + "_" + board.MC_n + "-" + (board.MC_n > 0 ? board.MC[board.MC_n-1] : "_") + ".txt";
					file = new FileWriter(filename_current);
					file.write("root board:\n");
					board.printFile(file, 0);
					board.printAlignmentsFile(file, 0);
					file.close();

					if(!board.hasAlignments(player)) {
						File todel = new File(filename_current);
						todel.delete();
					}
				} catch (Exception e) {
					try {
						throw e;
					} catch(IOException io) {}
				}
			}
			
			// db init
			DbNode root = createRoot(board);
			win_node = null;
			boolean found_sequence = false;
			found_win_sequences = 0;
			
			// recursive call for each possible move
			found_sequence = visit(root, player, true, Operators.TIER_MAX);
			root = null;

			// debug
			try {
				if(DEBUG_ON) file.close();
			} catch(Exception e) {}
			if(foundWin()) {
				if(DEBUG_PRINT) System.out.println("found win: " + foundWin() );
				log += "found win: " + foundWin() + "\n";
				log += "win node \n";
				log += win_node.board.printString();
				return getBestMove(player);
			}

			// best move
			return null;

		} catch (Exception e) {
			System.out.println(log);
			throw e;
		}

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

			// debug
			String log = "start";
			String filename_current = "";

			try {
				
				// DEBUG
				boolean found_something = false;
				if(DEBUG_ON) {
					if(!attacking) {
						file.write("\t\t\t\t--------\tSTART OF DEFENSE\t--------\n");
					}
				}
				short level = 1;
				if(!attacking) level = 8;

				// init dependency and combination lists
				LinkedList<DbNode> lastDependency = new LinkedList<DbNode>(), lastCombination = new LinkedList<DbNode>();
				initLastCombination(root, lastCombination);

				// debug
				log = "before loop";

				// debug filename
				if(DEBUG_ON) {
					if(attacking) {
						debug_code = (int)(Math.random() * DEBUG_CODE_MAX);
						filename_current = "debug/db1/db" + (counter++) + "_" + debug_code + "_" + root.board.MC_n + "-" + (root.board.MC_n > 0 ? root.board.MC[root.board.MC_n-1] : "_") + "-" + level + ".txt";
						//if(!attacking) filename_current = "debug/db2/db" + board.MC_n + "-" + level + "def" + defense++ + ".txt";
						file = new FileWriter(filename_current);
						file.write("attacker, attacking, maxtier = " + attacker + " " + attacking + " " + max_tier + "\n");
					}
				}
			
				// loop
				boolean found_sequence = false;
				while(	!isTimeEnded() && isTreeChanged(lastCombination) &&
						( (attacking && !foundWin() && found_win_sequences < MAX_THREAT_SEQUENCES) ||	//if attacker's visit: stop when found win
						(!attacking && !found_sequence) )												//if defender's visit: stop when found defense (any threat sequence)
				) {
					
					// debug filename
					//if(DEBUG_ON) {
					//	if(attacking) {
					//		debug_code = (int)(Math.random() * DEBUG_CODE_MAX);
					//		filename_current = "debug/db1/db" + (counter++) + "_" + debug_code + "_" + root.board.MC_n + "-" + (root.board.MC_n > 0 ? root.board.MC[root.board.MC_n-1] : "_") + "-" + level + ".txt";
					//		//if(!attacking) filename_current = "debug/db2/db" + board.MC_n + "-" + level + "def" + defense++ + ".txt";
					//		new File(filename_current);
					//		file = new FileWriter(filename_current);
					//	}
					//}
					
					// debug
					if(DEBUG_ON) {
						if(!attacking) file.write("\t\t\t\t\t\t\t\t");
						file.write("--------\tDEPENDENCY\t--------\n");
					}
					
					// start dependency stage
					lastDependency.clear();
					
					// HEURISTIC: only for attacker, only search for threats of tier < max tier found in defenses
					int max_tier_t = attacking? max_tier : root.getMaxTier();
					if(addDependencyStage(attacker, attacking, lastDependency, lastCombination, root, max_tier_t))	//uses lastCombination, fills lastDependency
						found_sequence = true;
						
						// debug
					log = "added dependency";
					if(!lastDependency.isEmpty()) found_something = true;
					
					// START COMBINATIO STAGE
					if((attacking && !foundWin()) || (!attacking && !found_sequence)) {
						lastCombination.clear();
						
						// DEBUG
						if(DEBUG_ON) {
							if(!attacking) file.write("\t\t\t\t\t\t\t\t");
							file.write("--------\tCOMBINATION\t--------\n");
						}

						if(addCombinationStage(root, attacker, attacking, lastDependency, lastCombination))				//uses lasdtDependency, fills lastCombination
							found_sequence = true;

						// debug
						log = "added combination";
						if(!lastCombination.isEmpty()) found_something = true;
						
						// DEBUG
						if(DEBUG_ON) {
							if(!attacking) file.write("\t\t\t\t\t\t\t\t");
							file.write("--------\tEND OF COMBINATION\t--------\n");
						}

					}
					
					// RE-CHECK AFTER COMBINATION
					level++;
					
					// DEBUG
					if(DEBUG_ON) {
						file.write("ATTACKING: " + (attacking? "ATTACKER":"DEFENDER") + "\n");
						file.write("FOUND SEQUENCE: " + found_sequence + "\n");
						file.write("VISIT WON: " + foundWin() + "\n");
						//if(attacking) {
						//	file.close();
						//	if(!found_something) {
						//		File todel = new File(filename_current);
						//		todel.delete();
						//	}
						//}
					}
				}

				// DEBUG
				log = "after loop";
				if(DEBUG_ON) {
					if(!attacking) {
						file.write("\t\t\t\t--------\tEND OF DEFENSE\t--------\n");
					} else {
						file.close();
						if(!found_something) {
							File todel = new File(filename_current);
							if(DEBUG_ONLY_FOUND_SEQ && !found_sequence)
								todel.delete();
						}
					}
				}

				return found_sequence;

			} 
			catch (ArrayIndexOutOfBoundsException e) {
				root.board.print();
				System.out.println(attacking + " " + attacker);
				System.out.println(log + "\n");
				throw e;
			}
			catch (Exception e) {
				System.out.println(log + "\n");
				try {
					if(DEBUG_ON)
						file.close();
					throw e;
				} catch (IOException ef) {
					System.out.println(e);
				}
			}
			return false;

		}

		/**
		 * @param root
		 * @param attacker
		 * @return true if found a defense (i.e. threat sequence was not wining)
		 */
		private boolean visitGlobalDefense(DbNode possible_win, DbNode root, byte attacker) {

			//DEBUG
			if(DEBUG_ON) {
				try {
					file.write("\t\t\t\tWIN:\n");
					possible_win.board.printFile(file, 1);
					file.write("\t\t\t\t-----\n");
				} catch(Exception e) {}
			}

			//add each combination of attacker's made threats to each dependency node
			DbNode new_root			= createDefensiveRoot(root, possible_win.board.markedThreats, attacker);
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

		/** (for now) assumptions:
		 * - the game ends only after a dependency stage is added (almost certain about proof)
		 * 	actually not true for mnk game (if you put 3 lined in a board, other 2 in another one, then merge the boards...)
		 */
		protected boolean addDependencyStage(byte attacker, boolean attacking, LinkedList<DbNode> lastDependency, LinkedList<DbNode> lastCombination, DbNode root, int max_tier) {

			String log = "start dep stage";
			try {

				boolean found_sequence = false;
				ListIterator<DbNode> it = lastCombination.listIterator();
				
				while (
					!isTimeEnded() && it.hasNext()
					&& found_win_sequences < MAX_THREAT_SEQUENCES
					&& ((attacking && !foundWin()) || (!attacking && !found_sequence))
				) {
					DbNode node = it.next();
					
					// debug
					if(DEBUG_ON) {
						if(!attacking) file.write("\t\t\t\t\t\t\t\t");
						file.write("DEPENDENCY: parent: \n");
						node.board.printFile(file, node.board.MC_n);
						if(!attacking) file.write("\t\t\t\t\t\t\t\t");
						file.write("children: \n");

						log = "before add children";
						file.write((node.board.MC_n > 0) ?
						(node.board.MC[node.board.MC_n-1].i + " " + node.board.MC[node.board.MC_n-1].j + " " + node.board.MC[node.board.MC_n-1].state)
							: "no MC");
						file.write("\n");
					}
					
					found_sequence = addDependentChildren(node, attacker, attacking, 1, lastDependency, root, max_tier);
				}
				return found_sequence;
				
			} catch(Exception e) {
				System.out.println(log + "\n\n" + e);
				try {
					if(DEBUG_ON) file.close();
					throw e;
				} catch (IOException io) {}
			}
			return false;
			
		}
		
		/**
		 * (see notes for findAllCombinationNodes() and class notes)
		 * @param root
		 * @param attacker
		 * @param attacking
		 * @param lastDependency
		 * @param lastCombination
		 * @return
		 */
		protected boolean addCombinationStage(DbNode root, byte attacker, boolean attacking, LinkedList<DbNode> lastDependency, LinkedList<DbNode> lastCombination) {
			
			ListIterator<DbNode> it = lastDependency.listIterator();
			boolean found_win = false;

			while (
				!isTimeEnded() && it.hasNext()
				&& found_win_sequences < MAX_THREAT_SEQUENCES
				&& !found_win
			) {
				DbNode node = it.next();
				
				// debug
				if(DEBUG_ON) {
					try {
						if(!attacking) file.write("\t\t\t\t\t\t\t\t");
						file.write("parent: \n");
						node.board.printFile(file, node.board.MC_n);
						if(!attacking) file.write("\t\t\t\t\t\t\t\t");
						file.write("children: \n");
					} catch (Exception e) {
						try {
							throw e;
						} catch(IOException io) {}
					}
				}
				
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
		 * @return true if found at least one possible winning sequence
		 */
		protected boolean addDependentChildren(DbNode node, byte attacker, boolean attacking, int lev, LinkedList<DbNode> lastDependency, DbNode root, int max_tier) {

			String log = "start addDependentChildren";
			try {

				byte state = node.board.gameState();
				if(state == GameState.OPEN)
				{

					// debug
					log = "before get applicable operators";

					boolean found_sequence = false;
					//LinkedList<CXCell[]> applicableOperators = getApplicableOperators(node, MAX_CHILDREN, my_attacker);
					ThreatsByRank applicableOperators = getApplicableOperators(node.board, attacker, max_tier);

					// debug
					log = "after get applicable operators";

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

									// debug
									if(DEBUG_ON) {
										try {
											if(!attacking) file.write("\t\t\t\t\t\t\t\t");
											file.write("\t\t\t" + threat.type + "\t" + atk_index + "\t");
											for(int i = 0; i < threat.related.length; i++) file.write(threat.related[i] + " " + threat.uses[i] + "\t");
											file.write("\n");
										} catch(Exception e) {}
									}
									
									//if a goal square is marked, returns true, as goal squares are only used for defensive search, where only score matters
									MovePair atk_cell = threat.related[atk_index];
									if(GOAL_SQUARES[atk_cell.i][atk_cell.j]) {
										
										DbNode newChild = addDependentChild(node, threat, atk_index, lastDependency, lev, attacker);
										// don't add to TT, as it's only for defense
										
										// debug
										if(DEBUG_ON) {
											try {
												if(!attacking) file.write("\t\t\t\t\t\t\t\t");
												file.write("-" + lev + "\t---\n");
												newChild.board.printFile(file, lev);
												file.write("MARKED GOAL SQUARE " + atk_cell + "\n");
											} catch(Exception e) {}
										}
										
										return true;
									}
									else {
										DbNode newChild = addDependentChild(node, threat, atk_index, lastDependency, lev, attacker);

										// debug
										if(DEBUG_ON) {
											try {
												if(!attacking) file.write("\t\t\t\t\t\t\t\t");
												file.write("-" + lev + "\t---\n");
												newChild.board.printFile(file, lev);
												if(!attacking) file.write("\t\t\t\t\t\t\t\t");
												file.write("---\n");
											} catch (Exception e) {
												try {
													throw e;
												} catch(IOException io) {}
											}
										}

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
					
					if(DEBUG_ON) {
						try {
							file.write("STATE (dependency): " + state + "\n");
						} catch(Exception e) {}
					}

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

			} catch (Exception e) {
				try {
					System.out.println(log);
					if(DEBUG_ON) file.close();
					throw e;
				} catch (IOException io) {}
			}
			return false;

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
		protected boolean findAllCombinationNodes(DbNode partner, DbNode node, byte attacker, boolean attacking, LinkedList<DbNode> lastCombination, DbNode root) {
			
			try {
				if(node == null || found_win_sequences >= MAX_THREAT_SEQUENCES || isTimeEnded())
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
					try {
						if(!attacking) file.write("\t\t\t\t\t\t\t\t");
						file.write("\t\tfirst parent: \n");
						partner.board.printFile(file, partner.board.MC_n);
						file.write(".\n");
						if(!attacking) file.write("\t\t\t\t\t\t\t\t");
						file.write("\t\tsecond parent: \n");
						node.board.printFile(file, node.board.MC_n);
						file.write(".\n");
					} catch (Exception e) {
						try {
							throw e;
						} catch(IOException io) {}
					}
				}

				//create combination with A's board (copied)
				if(	addCombinationChild(partner, node, lastCombination, root, attacker, attacking)
					|| findAllCombinationNodes(partner, node.getFirstChild(), attacker, attacking, lastCombination, root)
					|| findAllCombinationNodes(partner, node.getSibling(), attacker, attacking, lastCombination, root)
				)
					return true;

				return false;

			} catch(Exception e) {
				if(DEBUG_ON) {
					try{
						file.write("\nERROR\n");
						if(partner != null)
							partner.board.printFile(file, partner.board.MC_n);
							file.write("\n\n");
							if(partner != null)
							node.board.printFile(file, node.board.MC_n);
						file.close();
					}
					catch(Exception e1) {}
				}
				throw e;
			}
		}
	
	//#endregion ALGORITHM


	//#region CREATE

		protected DbNode createRoot(BoardBitDb B) {

			DbNode root = DbNode.copy(board, true, Operators.TIER_MAX, true);
			//NodeBoard root = NodeBoard.copy(board, true, Operators.TIER_MAX, true);
			return root;
		}

		private DbNode createDefensiveRoot(DbNode root, LinkedList<ThreatApplied> athreats, byte attacker) {

			String log = "createDef start\n";
			DbNode _node = null;
			int count = 0;
			try {

				ListIterator<ThreatApplied> it = athreats.listIterator();
				ThreatApplied athreat = null, athreat_prev = null;

				//create defenisve root copying current root, using opponent as player and marking only the move made by the current attacker in the first threat
				byte max_tier	= (byte)(Operators.tier(athreats.getFirst().threat.type) - 1);		// only look for threats better than mine
				DbNode def_root	= DbNode.copy(root.board, true, max_tier, false);
				def_root.board.setPlayer(Auxiliary.opponent(attacker));
				def_root.board.findAllAlignments(Auxiliary.opponent(attacker), max_tier, "defRoot_");

				
				// DEBUG
				if(DEBUG_ON) {
					try {
						file.write("MAX THREAT: " + max_tier + "\n");
					} catch(Exception e) {}
				}
				// debug
				log += "createDef before loop\n";
				
				//add a node for each threat, each node child/dependant from the previous one
				DbNode prev, node = def_root;
				while(it.hasNext()) {

					// debug
					count++;

					athreat = it.next();
					prev = node;

					// debug
					_node = node;
					log += (prev.board.alignments_by_cell[athreat.threat.related[athreat.related_index].i][athreat.threat.related[athreat.related_index].j].getFirst(attacker) == null)
					+ " " + (prev.board.alignments_by_cell[athreat.threat.related[athreat.related_index].i][athreat.threat.related[athreat.related_index].j].getFirst(Auxiliary.opponent(attacker)) == null) 
					 + "\n";
					log += athreat.threat.related[athreat.related_index] + " " + attacker + "\n";
					
					prev.board.mark(athreat.threat.related[athreat.related_index], attacker);
					
					// debug
					log += "createDef before if\n";
					
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
						log += "createDef end of if\n";
						
						// now included in getDependant()
						//node.board.markCells(threat.def, YOUR_MNK_PLAYER);
						// for future enhancements?
						//node.board.addThreat(threat);
					}
					//the new node doesn't check alignments
					
					//DEBUG
					if(DEBUG_ON) {
						try {
							file.write("\t\t\t\t" + athreat.threat.related[athreat.related_index] + "\n");
							prev.board.printFile(file, prev.board.MC_n);
							for(MovePair m : athreat.threat.related) file.write("\t\t\t\t" + m + " ");
							file.write("\n");
						} catch(Exception e) {}
					}
				}
				
				return def_root;

			} catch(Exception e) {
				System.out.println(log);
				System.out.println(count);
				if(_node != null)
					_node.board.print();
				throw e;
			}
		}

		/**
		 * sets child's game_state if entry exists in TT
		 */
		protected DbNode addDependentChild(DbNode node, ThreatCells threat, int atk, LinkedList<DbNode> lastDependency, int level, byte attacker) {
			
			try {

				BoardBitDb new_board	= node.board.getDependant(threat, atk, USE.BTH, node.getMaxTier(), true);
				DbNode newChild 		= new DbNode(new_board, false, node.getMaxTier());

				node.addChild(newChild);
				lastDependency.add(newChild);

				// debug
				if(DEBUG_ON)
					file.write("frees: " + node.board.free[0] + " " + new_board.free[0] + "\n");

				return newChild;

			} catch (Exception e) {
				try {
					if(DEBUG_ON) file.close();
					throw e;
				} catch(IOException io) {}
			}
			return null;

		}

		/**
		 * (I hope) adding the child to both parents is useless, for now.
		 * Only creates the child if the board wasn't already obtained by another combination in the same stage, of the same dbSearch (using TT, see class notes).
		 * @return (see findAllCombinations())
		 */
		protected boolean addCombinationChild(DbNode A, DbNode B, LinkedList<DbNode> lastCombination, DbNode root, byte attacker, boolean attacking) {

			int attacker_i			= (attacker == MY_PLAYER) ? 0 : 1;
			int max_threat			= Math.min(A.getMaxTier(), B.getMaxTier());
			BoardBitDb new_board	= A.board.getCombined(B.board, attacker, max_threat);
			DbNode new_child		= null;
			TranspositionElementEntry entry = TT.getState(new_board.hash);

			// DEBUG
			if(DEBUG_ON) {
				new_board.printFile(file, new_board.MC_n);
			}

			// if already analyzed and saved in TT
			if(entry != null && entry.state[attacker_i] != null){
				if(entry.state[attacker_i] == Auxiliary.gameState2CX(Auxiliary.cellState2winState(attacker)) )
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
				TT.setStateOrInsert(new_board.hash, CXGameState.OPEN, attacker_i);
			}

			//DEBUG
			if(DEBUG_ON) {
				try {
					file.write("\t\t\t\tEXISTS IN TT: " + new_board.hash + "\n");
				} catch(Exception e) {}
			}

			return false;
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

			String log = "start getApplicable Operators";
			try {

				byte defender		= Auxiliary.opponent(attacker);
				ThreatsByRank res	= new ThreatsByRank();

				for(AlignmentsList alignments_by_row : board.alignments_by_direction) {
					for(BiList_ThreatPos alignments_in_row : alignments_by_row) {
						if(alignments_in_row != null) {
							BiNode<ThreatPosition> alignment = alignments_in_row.getFirst(attacker);
							if(alignment != null && Operators.tier(alignment.item.type) <= max_tier) {
								do {

									// debug
									log = "after alignment != null";

									// debug
									/*
									if(DEBUG_ON) {
										file.write("applying operator " + alignment.item + " for attacker " + attacker + "\n");
										board.printFile(file, 1);
									}
									*/
									
									ThreatCells cell_threat_operator = Operators.applied(board, alignment.item, attacker, defender);

									// debug
									log = "got operators.applied operator";
									/*
									if(DEBUG_ON) {
										file.write("aplied: \n");
										if(cell_threat_operator != null) {
											for(int i = 0; i < cell_threat_operator.uses.length; i++) file.write(cell_threat_operator.related[i] + " ");
											file.write("\n");
											for(int i = 0; i < cell_threat_operator.uses.length; i++) file.write(cell_threat_operator.uses[i] + " ");
											file.write("\n");
											file.write(cell_threat_operator.type + "\n");
										}
										board.printFile(file, 1);
									}
									*/

									if(cell_threat_operator != null) res.add(cell_threat_operator);
									alignment = alignment.next;

									// debug
									log = "added operator to res";

								} while(alignment != null);
							}
						}
					}
				}
				return res;

			} catch (Exception e) {
				System.out.println(log);
				throw e;
				/*
				try {
					throw e;
				} catch(IOException io) {}
				*/
			}

		}

		private void removeCombinationTTEntries(LinkedList<DbNode> lastCombination, byte attacker) {

			/* remmove TT "open" entries from lastCombination
			*/
			ListIterator<DbNode> it = lastCombination.listIterator();
			int attacker_i = (attacker == MY_PLAYER) ? 0 : 1;

			while(it.hasNext()) {
				long hash = it.next().board.hash;
				TranspositionElementEntry entry = TT.getState(hash);
				
				if(entry != null && entry.state[attacker_i] == CXGameState.OPEN)
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
	
		protected CXCell getBestMove(byte player) {
			int i = board.MC_n;
			//return first player's move after initial state
			while(Auxiliary.CX2cellState(win_node.board.getMarkedCell(i).state) != player)
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
