package connectx.pndb;

import java.io.File;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.ListIterator;

import connectx.CXCellState;
import connectx.CXGameState;
import connectx.pndb.BiList.BiNode;
import connectx.pndb.Operators.RankedThreats;
import connectx.pndb.Operators.Threat;

public class DbSearch {
	
	//#region CONSTANTS
	private CXCellState MY_CX_PLAYER;
	private CXCellState YOUR_CX_PLAYER;
	private CXGameState MY_WIN;
	private CXGameState YOUR_WIN;
	private int MY_PLAYER;

	private final int MAX_THREAT_SEQUENCES = 10;

	//#endregion CONSTANTS

	private long timer_start;				//turn start (milliseconds)
	private long timer_end;					//time (millisecs) at which to stop timer

	private BoardBitDb board;

	// VARIABLES FOR A DB-SEARCH EXECUTION
	private int found_win_sequences;
	private BoardBitDb win_node;

	// DEBUG
	private final boolean DEBUG_ON = true;
	FileWriter file = null;





	public void initPlayer(boolean first) {
		if(first) {
			MY_CX_PLAYER	= CXCellState.P1;
			YOUR_CX_PLAYER	= CXCellState.P2;
			//player_opponent = CXCellState.P2;
			MY_WIN		= CXGameState.WINP1;
			YOUR_WIN	= CXGameState.WINP2;
			MY_PLAYER	= 0;
			//your_win = CXGameState.WINP2;
		} else {
			MY_CX_PLAYER	= CXCellState.P2;
			YOUR_CX_PLAYER	= CXCellState.P1;
			//player_opponent = CXCellState.P1;
			MY_WIN		= CXGameState.WINP2;
			YOUR_WIN	= CXGameState.WINP1;
			MY_PLAYER	= 1;
			//your_win = CXGameState.WINP1;
		}
	}

	
	public int selectColumn(BoardBit board_pn, PnNode root_pn, long time_remaining) {

		// timer
		timer_start	= System.currentTimeMillis();
		timer_end	= time_remaining;

		// update own board instance
		board = new BoardBitDb(board_pn);
		board.updateAlignments();

		// debug
		if(DEBUG_ON) {
			try {
				file = new FileWriter("debug/db2/main" + 0 + ".txt");
				board.print();
				file.close();
			} catch (Exception e) {}
		}
		
		// db init
		DbNode root = createRoot(board);
		win_node = null;
		boolean found_sequence = false;
		found_win_sequences = 0;

		// recursive call for each possible move
		found_sequence = visit(root, MY_CX_PLAYER, true, Operators.TIER_MAX);

		// debug	
		System.out.println("found win: " + foundWin() );

		// best move
		return -1;

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
		protected boolean visit(DbNode root, CXCellState attacker, boolean attacking, int max_tier) {

			// DEBUG
			if(DEBUG_ON) {
				if(!attacking) {
					try {
						file.write("\t\t\t\t--------\tSTART OF DEFENSE\t--------\n");
					} catch(Exception e) {}
				}
			}
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

				// debug filename
				if(DEBUG_ON) {
					if(attacking) {
						try {
							String filename_current = "debug/db2/db" + 1 + "-" + level + ".txt";
							//if(!attacking) filename_current = "debug/db2/db" + board.MC_n + "-" + level + "def" + defense++ + ".txt";
							new File(filename_current);
							file = new FileWriter(filename_current);
						} catch(Exception e) { }
					}
				}
				
				// debug
				if(DEBUG_ON) {
					try {
						if(!attacking) file.write("\t\t\t\t\t\t\t\t");
						file.write("--------\tDEPENDENCY\t--------\n");
					} catch(Exception e) {}
				}

				// start dependency stage
				lastDependency.clear();

				// HEURISTIC: only for attacker, only search for threats of tier < max tier found in defenses
				int max_tier_t = attacking? max_tier : root.getMaxTier();
				if(addDependencyStage(attacker, attacking, lastDependency, lastCombination, root, max_tier_t))			//uses lastCombination, fills lastDependency
					found_sequence = true;
				
				// START COMBINATIO STAGE
				if((attacking && !foundWin()) || (!attacking && !found_sequence)) {
					lastCombination.clear();
					// DEBUG
					if(DEBUG_ON) {
						try {
							if(!attacking) file.write("\t\t\t\t\t\t\t\t");
							file.write("--------\tCOMBINATION\t--------\n");
						} catch(Exception e) {}
					}
					if(addCombinationStage(root, attacker, attacking, lastDependency, lastCombination))		//uses lasdtDependency, fills lastCombination
						found_sequence = true;
					// DEBUG
					if(DEBUG_ON) {
						try {
							if(!attacking) file.write("\t\t\t\t\t\t\t\t");
							file.write("--------\tEND OF COMBINATION\t--------\n");
						} catch(Exception e) {}
					}
				}
				// RE-CHECK AFTER COMBINATION
				level++;
				
				// DEBUG
				if(DEBUG_ON) {
					try {
						file.write("ATTACKING: " + (attacking? "ATTACKER":"DEFENDER") + "\n");
						file.write("FOUND SEQUENCE: " + found_sequence + "\n");
						file.write("VISIT WON: " + foundWin() + "\n");
						if(attacking) file.close();
					} catch (Exception e) {}
				}
			}
			// DEBUG
			if(!attacking) {
				if(DEBUG_ON) {
					try { 
						file.write("\t\t\t\t--------\tEND OF DEFENSE\t--------\n");
					} catch(Exception e) {}
				}
			}
			return found_sequence;
		}

		/** (for now) assumptions:
		 * - the game ends only after a dependency stage is added (almost certain about proof)
		 * 	actually not true for mnk game (if you put 3 lined in a board, other 2 in another one, then merge the boards...)
		 */
		protected boolean addDependencyStage(CXCellState attacker, boolean attacking, LinkedList<DbNode> lastDependency, LinkedList<DbNode> lastCombination, DbNode root, int max_tier) {

			boolean found_sequence = false;
			ListIterator<DbNode> it = lastCombination.listIterator();
			while(it.hasNext() && !found_sequence) {
				DbNode node = it.next();

				// debug
				if(DEBUG_ON) {
					try {
						if(!attacking) file.write("\t\t\t\t\t\t\t\t");
						file.write("parent: \n");
						node.board.print();
						if(!attacking) file.write("\t\t\t\t\t\t\t\t");
						file.write("children: \n");
					} catch (Exception e) {}
				}

				found_sequence = addDependentChildren(node, attacker, attacking, 1, lastDependency, root, max_tier);
			}
			return found_sequence;
		}

		protected boolean addCombinationStage(DbNode root, CXCellState attacker, boolean attacking, LinkedList<DbNode> lastDependency, LinkedList<DbNode> lastCombination) {

			boolean found_sequence = false;
			ListIterator<DbNode> it = lastDependency.listIterator();
			while(it.hasNext() && !found_sequence) {
				DbNode node = it.next();

				// debug
				if(DEBUG_ON) {
					try {
						if(!attacking) file.write("\t\t\t\t\t\t\t\t");
						file.write("parent: \n");
						node.board.print();
						if(!attacking) file.write("\t\t\t\t\t\t\t\t");
						file.write("children: \n");
					} catch (Exception e) {}
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
		protected boolean addDependentChildren(DbNode node, CXCellState attacker, boolean attacking, int lev, LinkedList<DbNode> lastDependency, DbNode root, int max_tier) {

			byte state = node.board.gameState();
			if(state == GameState.OPEN)
			{
				boolean found_sequence = false;
				//LinkedList<CXCell[]> applicableOperators = getApplicableOperators(node, MAX_CHILDREN, my_attacker);
				RankedThreats applicableOperators = getApplicableOperators(node.board, attacker, max_tier);
				for(LinkedList<Threat> tier : applicableOperators) {
					if(tier != null) {
						for(Threat threat : tier) {
							int atk_index = 0;
							//stops either after checking all threats, or if found a win/defense (for defended it is just any possible winning sequence)
							while(	((attacking && !foundWin()) || (!attacking && !found_sequence)) &&
									(atk_index = threat.nextAtk(atk_index)) != -1
							) {
								// DEBUG
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
								if(GOAL_SQUARES[atk_cell.i()][atk_cell.j()]) {
									// DEBUG
									if(DEBUG_ON) {
										try {
											NodeBoard newChild = addDependentChild(node, threat, atk_index, lastDependency);
											if(!attacking) file.write("\t\t\t\t\t\t\t\t");
											file.write("-" + lev + "\t---\n");
											DbTest.printBoard(newChild.board, file, lev + (attacking?0:8));
											file.write("MARKED GOAL SQUARE " + atk_cell + "\n");
										} catch(Exception e) {}
									}

									return true;
								}
								else {
									NodeBoard newChild = addDependentChild(node, threat, atk_index, lastDependency);
									// DEBUG
									if(DEBUG_ON) {
										try {
											if(!attacking) file.write("\t\t\t\t\t\t\t\t");
											file.write("-" + lev + "\t---\n");
											DbTest.printBoard(newChild.board, file, lev + (attacking?0:8));
											if(!attacking) file.write("\t\t\t\t\t\t\t\t");
											file.write("---\n");
										} catch (Exception e) {}
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
				TT.setState(node.board.hash, state, attacker_i);
				if(DEBUG_ON) {
					try {
						file.write("STATE (dependency): " + state + "\n");
					} catch(Exception e) {}
				}
				if(state == CXGameState.DRAW) return !attacking;
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
		protected boolean findAllCombinationNodes(DbNode partner, DbNode node, CXCellState attacker, boolean attacking, LinkedList<DbNode> lastCombination, DbNode root) {
			
			try {
				if(node == null || found_win_sequences >= MAX_THREAT_SEQUENCES) return false;
				else {
					CXGameState state = node.board.gameState();
					//DEBUG
					if(DEBUG_ON) {
						if(state != CXGameState.OPEN) {
							try {
								file.write("\t\t\t\tSTATE: " + state);
							} catch(Exception e) {}
						}
					}

					if(state == CXGameState.OPEN) {
						boolean found_sequence = false;
						//doesn't check if isDependencyNode() : also combinations of combination nodes could result in alignments
						BoardBitDb.BoardsRelation relation = partner.validCombinationWith(node, attacker);
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
										DbTest.printBoard(partner.board, file,attacking?2:10);
										file.write(".\n");
										if(!attacking) file.write("\t\t\t\t\t\t\t\t");
										file.write("\t\tsecond parent: \n");
										DbTest.printBoard(node.board, file,attacking?2:10);
										file.write(".\n");
									} catch (Exception e) {}
								}
								//create combination with A's board (copied)
								if(addCombinationChild(partner, node, lastCombination, root, attacker, attacking))
									found_sequence = true;
								//DEBUG
								if(DEBUG_ON) {
									if(found_sequence) {
										try {
											file.write("found sequence");
										} catch(Exception e) {}
									}
								}
								// DEBUG
								if(DEBUG_ON) {
									try {
										if(!attacking) file.write("\t\t\t\t\t\t\t\t");
										file.write("---\n");
										if(!attacking) file.write("\t\t\t\t\t\t\t\t");
										file.write("---\n");
									} catch (Exception e) {}
								}

								if(foundWin()) return true;
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
					else if(state == CXGameState.DRAW) return !attacking;
					else return (state == Auxiliary.cellState2winState(attacker));
				}
			}

			// debug
			catch(Exception e) {
					if(DEBUG_ON) {
						try{
							file.write("\nERROR\n");
							if(partner != null)
								partner.board.print();
							file.write("\n\n");
							if(partner != null)
								node.board.print();
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
			DbNode root = new DbNode(B);
			//NodeBoard root = NodeBoard.copy(board, true, Operators.TIER_MAX, true);
			return root;
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

	//#region GET

		protected RankedThreats getApplicableOperators(BoardBitDb board, CXCellState attacker, int max_tier) {
			CXCellState defender = Auxiliary.opponent(attacker);
			RankedThreats res = new RankedThreats();
			for(AlignmentsList rows_in_dir : board.lines_per_dir) {
				for(BiList_OpPos row : rows_in_dir) {
					if(row != null) {
						BiNode<OperatorPosition> line = row.getFirst(attacker);
						if(line != null && Operators.tier(line.item.type) <= max_tier) {
							do {
								Threat cell_threat_operator = Operators.applied(board, line.item, attacker, defender);
								if(cell_threat_operator != null) res.add(cell_threat_operator);
								line = line.next;
							} while(line != null);
						}
					}
				}
			}
			return res;
		}
	
	//#endregion GET

	//#region HELPER

		protected boolean foundWin() {
			return win_node != null;
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
