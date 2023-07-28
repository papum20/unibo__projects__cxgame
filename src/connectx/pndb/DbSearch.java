package connectx.pndb;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;

import connectx.CXCell;
import connectx.CXCellState;
import connectx.CXGameState;
import connectx.pndb.BiList.BiNode;
import connectx.pndb.DbNode.BoardsRelation;
import connectx.pndb.Operators;
import connectx.pndb.Operators.ThreatsByRank;
import connectx.pndb.Operators.ThreatCells;
import connectx.pndb.Operators.USE;

public class DbSearch {
	
	//#region CONSTANTS
	private CXCellState MY_CX_PLAYER;
	private CXCellState YOUR_CX_PLAYER;
	private CXGameState MY_WIN;
	private CXGameState YOUR_WIN;
	private byte MY_PLAYER;
	private byte YOUR_PLAYER;

	private final int MAX_THREAT_SEQUENCES = 10;

	//#endregion CONSTANTS

	private long timer_start;				//turn start (milliseconds)
	private long timer_end;					//time (millisecs) at which to stop timer

	private int M, N;
	private BoardBitDb board;
	protected TranspositionTable TT;

	// VARIABLES FOR A DB-SEARCH EXECUTION
	private int found_win_sequences;
	private DbNode win_node;
	private boolean[][] GOAL_SQUARES;
	
	// DEBUG
	private final boolean DEBUG_ON = true;
	int counter = 0;
	FileWriter file = null;
	int debug_code;
	int DEBUG_CODE_MAX = 999999999;





	public void init(int M, int N, int X, boolean first) {
		
		this.M = M;
		this.N = N;

		board = new BoardBitDb(M, N, X);
		TT = new TranspositionTable(M, N);
		BoardBitDb.TT = TT;
		
		MY_PLAYER	= CellState.ME;
		YOUR_PLAYER	= CellState.YOU;

		MY_CX_PLAYER	= CXCellState.P1;
		YOUR_CX_PLAYER	= CXCellState.P2;
		//player_opponent = CXCellState.P2;
		MY_WIN		= CXGameState.WINP1;
		YOUR_WIN	= CXGameState.WINP2;
		//your_win = CXGameState.WINP2;

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
		board.copy(board_pn);
		board.setPlayer(player);

		board.findAllAlignments(MY_PLAYER, Operators.TIER_MAX, "selCol_");
		board.findAllAlignments(YOUR_PLAYER, Operators.TIER_MAX, "selCol_");
		//board.updateAlignments(last_move_pair, last_move.state);
		
		// debug
		if(DEBUG_ON) {
			try {
				debug_code = (int)(Math.random() * DEBUG_CODE_MAX);
				String filename_current = "debug/db1/main" + (counter++) + "_" + debug_code + "_" + board.MC_n + "-" + (board.MC_n > 0 ? board.MC[board.MC_n-1] : "_") + ".txt";
				file = new FileWriter(filename_current);
				file.write("root board:\n");
				board.printFile(file, 0);
				board.printAlignmentsFile(file, 0);
				file = new FileWriter(filename_current);
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
		found_sequence = visit(root, MY_PLAYER, true, Operators.TIER_MAX);
		
		// debug	
		try {
			if(file != null) file.close();
		} catch(Exception e) {}
		if(foundWin()) {
			System.out.println("found win: " + foundWin() );
			System.out.println("win node ");
			win_node.board.print();
			return getBestMove();
		}

		// best move
		return null;

	}


		//#region ALGORITHM

		/**
		 * @param root : root for this db-search
		 * @param my_attacker : true if i'm attacker
		 * @param goal_squares : if one occupied by attacker, terminates search
		 * @param attacking : potential winning threat sequences only investigated for attacker
		 * @param max_tier : only threats <= this category can be applied
		 * @return true if there's a winning sequence
		 */
		protected boolean visit(DbNode root, byte attacker, boolean attacking, int max_tier) {

			String log = "start";

			try {
				
				// DEBUG
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
				
				// loop
				boolean found_sequence = false;
				while(	!isTimeEnded() && isTreeChanged(lastCombination) &&
						( (attacking && !foundWin() && found_win_sequences < MAX_THREAT_SEQUENCES) ||	//if attacker's visit: stop when found win
						(!attacking && !found_sequence) )												//if defender's visit: stop when found defense (any threat sequence)
				) {

					// debug filename
					if(DEBUG_ON) {
						if(attacking) {
							debug_code = (int)(Math.random() * DEBUG_CODE_MAX);
							String filename_current = "debug/db1/db" + (counter++) + "_" + debug_code + "_" + root.board.MC_n + "-" + (root.board.MC_n > 0 ? root.board.MC[root.board.MC_n-1] : "_") + "-" + level + ".txt";
							//if(!attacking) filename_current = "debug/db2/db" + board.MC_n + "-" + level + "def" + defense++ + ".txt";
							new File(filename_current);
							file = new FileWriter(filename_current);
						}
					}
					
					// debug
					if(DEBUG_ON) {
						if(!attacking) file.write("\t\t\t\t\t\t\t\t");
						file.write("--------\tDEPENDENCY\t--------\n");
					}

					// start dependency stage
					lastDependency.clear();

					// HEURISTIC: only for attacker, only search for threats of tier < max tier found in defenses
					int max_tier_t = attacking? max_tier : root.getMaxTier();
					if(addDependencyStage(attacker, attacking, lastDependency, lastCombination, root, max_tier_t))			//uses lastCombination, fills lastDependency
						found_sequence = true;
					
					// debug
					log = "added dependency";

					// START COMBINATIO STAGE
					if((attacking && !foundWin()) || (!attacking && !found_sequence)) {
						lastCombination.clear();

						// DEBUG
						if(DEBUG_ON) {
							if(!attacking) file.write("\t\t\t\t\t\t\t\t");
							file.write("--------\tCOMBINATION\t--------\n");
						}

						if(addCombinationStage(root, attacker, attacking, lastDependency, lastCombination))		//uses lasdtDependency, fills lastCombination
							found_sequence = true;

						log = "added combination";
							
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
						if(attacking) file.close();
					}
				}

				// DEBUG
				log = "after loop";
				if(!attacking) {
					if(DEBUG_ON) {
						file.write("\t\t\t\t--------\tEND OF DEFENSE\t--------\n");
					}
				}

				return found_sequence;

			} catch (Exception e) {
				System.out.println(log + "\n");
				try {
					if(file != null)
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
			DbNode new_root			= createDefensiveRoot(root, possible_win.board.markedThreats);
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

			String log = "start dep stage";
			try {

				boolean found_sequence = false;
				ListIterator<DbNode> it = lastCombination.listIterator();
				while(it.hasNext() && !found_sequence) {
					DbNode node = it.next();

					// debug
					if(DEBUG_ON) {
						if(!attacking) file.write("\t\t\t\t\t\t\t\t");
						file.write("DEPENDENCY: parent: \n");
						node.board.printFile(file, node.board.MC_n);
						if(!attacking) file.write("\t\t\t\t\t\t\t\t");
						file.write("children: \n");

						log = "before add children";
						file.write((node.board.MC_n > 0) ? node.board.MC[node.board.MC_n-1].toString() : "no MC");
						file.write("\n");
					}
					
					found_sequence = addDependentChildren(node, attacker, attacking, 1, lastDependency, root, max_tier);
				}
				return found_sequence;

			} catch(Exception e) {
				System.out.println(log + "\n\n" + e);
				try {
					file.close();
					throw e;
				} catch (IOException io) {}
			}
			return false;
			
		}

		protected boolean addCombinationStage(DbNode root, byte attacker, boolean attacking, LinkedList<DbNode> lastDependency, LinkedList<DbNode> lastCombination) {

			boolean found_sequence = false;
			ListIterator<DbNode> it = lastDependency.listIterator();
			while(it.hasNext() && !found_sequence) {
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
								while(	((attacking && !foundWin()) || (!attacking && !found_sequence)) &&
								((atk_index = threat.nextAtk(atk_index)) != -1)
								) {
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
										
										// debug
										if(DEBUG_ON) {
											try {
												DbNode newChild = addDependentChild(node, threat, atk_index, lastDependency, lev);
												if(!attacking) file.write("\t\t\t\t\t\t\t\t");
												file.write("-" + lev + "\t---\n");
												newChild.board.printFile(file, lev);
												file.write("MARKED GOAL SQUARE " + atk_cell + "\n");
											} catch(Exception e) {}
										}
										
										return true;
									}
									else {
										DbNode newChild = addDependentChild(node, threat, atk_index, lastDependency, lev);

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
					int attacker_i = attacking? 0:1;
					TT.setState(node.board.hash, Auxiliary.gameState2CX(state), attacker_i);
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
					file.close();
					throw e;
				} catch (IOException io) {}
			}
			return false;

		}

		/**
		 * @param partner : fixed node for combination
		 * @param node : iterating node for combination
		 */
		protected boolean findAllCombinationNodes(DbNode partner, DbNode node, byte attacker, boolean attacking, LinkedList<DbNode> lastCombination, DbNode root) {
			
			try {
				if(node == null || found_win_sequences >= MAX_THREAT_SEQUENCES) return false;
				else {
					byte state = node.board.gameState();
					
					//DEBUG
					if(DEBUG_ON) {
						if(state != GameState.OPEN) {
							try {
								file.write("\t\t\t\tSTATE: " + state);
							} catch(Exception e) {}
						}
					}

					if(state == GameState.OPEN) {
						boolean found_sequence = false;

						//doesn't check if isDependencyNode() : also combinations of combination nodes could result in alignments
						DbNode.BoardsRelation relation = partner.validCombinationWith(node, attacker);
						// DEBUG
						//try {
						//	DbTest.printBoard(partner.board, file, 10);
						//	file.write("\n");
						//	DbTest.printBoard(node.board, file, 10);
						//	file.write("\t\t\t\t\t\t\t\t\t\t" + relation + "\n");
						//} catch(Exception e) {}
						if(relation != BoardsRelation.CONFLICT) {
							if(relation == BoardsRelation.USEFUL) {

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
								if(addCombinationChild(partner, node, lastCombination, root, attacker, attacking))
									found_sequence = true;

								//DEBUG
								if(DEBUG_ON) {
									if(found_sequence) {
										try {
											file.write("found sequence\n");
										} catch(Exception e) {}
									}
									try {
										if(!attacking) file.write("\t\t\t\t\t\t\t\t");
										file.write("---\n");
										if(!attacking) file.write("\t\t\t\t\t\t\t\t");
										file.write("---\n");
									} catch (Exception e) {
										try {
											throw e;
										} catch(IOException io) {}
									}
								}

								if(foundWin())
									return true;
							}
							
							if(findAllCombinationNodes(partner, node.getFirstChild(), attacker, attacking, lastCombination, root)) {
								if(foundWin()) return true;
								else found_sequence = true;
							}
						}

						if(findAllCombinationNodes(partner, node.getSibling(), attacker, attacking, lastCombination, root))
							found_sequence = true;
						return found_sequence;
					}
					// GAME STATE CASES
					else if(state == GameState.DRAW) return !attacking;
					else return (state == Auxiliary.cellState2winState(attacker));
				}
			}

			// debug
			catch(Exception e) {
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

		private DbNode createDefensiveRoot(DbNode root, LinkedList<ThreatApplied> athreats) {

			ListIterator<ThreatApplied> it = athreats.listIterator();
			ThreatApplied athreat = null;

			//create defenisve root copying current root, using opponent as player and marking only the move made by the current attacker in the first threat
			byte max_tier	= (byte)(Operators.tier(athreats.getFirst().threat.type) - 1);		// only look for threats better than mine
			DbNode def_root	= DbNode.copy(root.board, true, max_tier, false);
			def_root.board.setPlayer(YOUR_PLAYER);
			def_root.board.findAllAlignments(YOUR_PLAYER, max_tier, "defRoot_");

			// DEBUG
			if(DEBUG_ON) {
				try {
					file.write("MAX THREAT: " + max_tier + "\n");
				} catch(Exception e) {}
			}

			//add a node for each threat, each node child/dependant from the previous one
			DbNode prev, node = def_root;
			while(it.hasNext()) {
				athreat = it.next();
				prev = node;
				prev.board.mark(athreat.threat.related[athreat.related_index], MY_PLAYER);

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
			
			//DEBUG
			if(athreat.threat.related.length > 0) node.board.print();
			return def_root;
		}

		/**
		 * sets child's game_state if entry exists in TT
		 */
		protected DbNode addDependentChild(DbNode node, ThreatCells threat, int atk, LinkedList<DbNode> lastDependency, int level) {
			
			try {

				BoardBitDb new_board	= node.board.getDependant(threat, atk, USE.BTH, node.getMaxTier(), true);

				// debug
				file.write("frees: " + node.board.free[0] + " " + new_board.free[0] + "\n");
				
				int attacker_i			= new_board.currentPlayer;
				DbNode newChild			= new DbNode(new_board, false, node.getMaxTier());
				TranspositionElementEntry entry = TT.getState(new_board.hash);

				if(entry != null && entry.state[attacker_i] != null) {

					//DEBUG
					if(DEBUG_ON) {
						try {
							file.write("\t\t\t\tEXISTS IN TT: " + new_board.hash + "\n");
							new_board.printFile(file, level);
						} catch(Exception e) {}
					}

					new_board.setGameState(entry.state[attacker_i]);
				}
				else {
					TT.insert(new_board.hash, CXGameState.OPEN, attacker_i);
					//only adds child to tree and list if doesn't already exist
					node.addChild(newChild);
					lastDependency.add(newChild);
				}
				return newChild;

			} catch (Exception e) {
				try {
					file.close();
					throw e;
				} catch(IOException io) {}
			}
			return null;

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

			// DEBUG
			if(DEBUG_ON) {
				new_board.printFile(file, new_board.MC_n);
			}

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

					//DEBUG
					if(DEBUG_ON) {
						try {
							file.write("\t\t\t\tGAME STATE: " + state + "\n");
						} catch(Exception e) {}
					}

				}
				else {
					//if TT has entry, update board's state (if OPEN, remains OPEN)
					new_board.setGameState(entry.state[attacker_i]);

					//DEBUG
					if(DEBUG_ON) {
						try {
							file.write("\t\t\t\tEXISTS IN TT: " + new_board.hash + "\n");
						} catch(Exception e) {}
					}

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
									file.write("applying operator " + alignment.item + " for attacker " + attacker + "\n");
									board.printFile(file, 1);
									
									ThreatCells cell_threat_operator = Operators.applied(board, alignment.item, attacker, defender);
									
									// debug
									log = "got operators.applied operator";
									file.write("aplied: \n");
									if(cell_threat_operator != null) {
										for(int i = 0; i < cell_threat_operator.uses.length; i++) file.write(cell_threat_operator.related[i] + " ");
										file.write("\n");
										for(int i = 0; i < cell_threat_operator.uses.length; i++) file.write(cell_threat_operator.uses[i] + " ");
										file.write("\n");
										file.write(cell_threat_operator.type + "\n");
									}
									board.printFile(file, 1);

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
				try {
					throw e;
				} catch(IOException io) {}
			}
			return null;

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
	
		protected CXCell getBestMove() {
			int i = board.MC_n;
			//return first player's move after initial state
			while(win_node.board.getMarkedCell(i).state != MY_CX_PLAYER)
				i++;
			return win_node.board.getMarkedCell(i);
		}
		
		/**
		 * 
		 * @return true if it's time to end the turn
		 */
		private boolean isTimeEnded() {
			return (System.currentTimeMillis() - timer_start) >= timer_end;
		}

		/* tree is changed if either lastdCombination o lastDependency are not empty;
			* however, dependency node are created from other dependency nodes only in the same level,
			* so such iteration would be useless
			*/
		protected boolean isTreeChanged(LinkedList<DbNode> lastCombination) {
			return lastCombination.size() > 0;
		}

	
	//#endregion HELPER

}
