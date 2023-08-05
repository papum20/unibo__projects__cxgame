package pndb.alpha;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

import connectx.CXCell;
import connectx.CXCellState;
import pndb.alpha.Operators.ThreatsByRank;
import pndb.alpha.threats.AlignmentsList;
import pndb.alpha.threats.BiList_Node_ThreatPos;
import pndb.alpha.threats.BiList_ThreatPos;
import pndb.alpha.threats.ThreatApplied;
import pndb.alpha.threats.ThreatCells;
import pndb.alpha.threats.ThreatPosition;
import pndb.alpha.threats.ThreatCells.USE;
import pndb.constants.Auxiliary;
import pndb.constants.CellState;
import pndb.constants.GameState;
import pndb.constants.MovePair;
import pndb.constants.Constants.BoardsRelation;
import pndb.structures.BiList.BiNode;




public class BoardBitDb extends BoardBit implements IBoardBitDb<BoardBitDb> {
	

	public static final MovePair DIRECTIONS[] = {
		new MovePair(-1, 0),
		new MovePair(-1, 1),
		new MovePair(0, 1),
		new MovePair(1, 1),
		new MovePair(1, 0),
		new MovePair(1, -1),
		new MovePair(0, -1),
		new MovePair(-1, -1)
	};

	protected static final MovePair MIN = new MovePair(0, 0);
	protected final MovePair MAX;

	public static byte MY_PLAYER;

	public CXCell[] MC; 							// Marked Cells
	private int MC_n;								// marked cells number
	public LinkedList<ThreatApplied> markedThreats;

	//AUXILIARY STRUCTURES (BOARD AND ARRAYS) FOR COUNTING ALIGNMENTS
	protected AlignmentsList alignments_rows;
	protected AlignmentsList alignments_cols;
	protected AlignmentsList alignments_diagright;		//diagonals from top-left to bottom-right
	protected AlignmentsList alignments_diagleft;		//diagonals from top-right to bottom-left
	/*
	 * horizontal:	dimension=M,		indexed: by row
	 * vertical:	dimension=N,		indexed: by col
	 * dright:		dimension=M+N-1,	indexed: by start of diagonal on the top row, i.e. from -M+1 to N-1
	 * dleft:		dimension=M+N-1,	indexed: by start of diagonal on the top row, i.e. from 0 to N+M-1
	 */
	protected AlignmentsList[] alignments_by_direction;									//for each direction 0-3, contains the reference to the proper lines array(list)
	protected static final int[] alignments_direction_indexes = new int[]{2, 4, 3, 5};	//indexes in DIRECTIONS, with same order as lines_per_dir
	

	protected BiList_Node_ThreatPos[][] alignments_by_cell;

	//protected final CXCellState[] Player	= {CXCellState.P1, CXCellState.P2};
	protected final byte[] Player_byte 		= {CellState.P1, CellState.P2};
	//protected final int[] Player_bit 		= {1, 0};
	protected int currentPlayer;		// currentPlayer plays next move (= 0 or 1)

	/* implementation
	*/
	// for findAlignmentsInDirection()
	private static MovePair	c1				= new MovePair(),
							c2				= new MovePair(),
							end_c1			= new MovePair(),
							end_c2			= new MovePair(),
							c_it			= new MovePair(),
							threat_start	= new MovePair(),
							threat_end		= new MovePair();
	
	// Debug
	int count = 0;
	private static boolean DEBUG_ON = false;
	private static boolean DEBUG_PRINT = false;
	private static FileWriter file;
  



	BoardBitDb(int M, int N, int X) {
		super(M, N, X);
		
		MAX = new MovePair(M, N);
		currentPlayer = 0;
		
		initAlignmentStructures();
		markedThreats = new LinkedList<ThreatApplied>();
		
	}

	BoardBitDb(BoardBit B) {
		super(B.M, B.N, B.X);
		
		MAX = new MovePair(M, N);
		currentPlayer = 0;
		
		initAlignmentStructures();
		markedThreats = new LinkedList<ThreatApplied>();

		copy(B);
		copyMCfromBoard(B);
	}
	
	BoardBitDb(BoardBitDb B, boolean copy_threats) {
		super(B.M, B.N, B.X);
		
		MAX = new MovePair(M, N);
		currentPlayer = B.currentPlayer;
		hash = B.hash;
		
		if(copy_threats) copyAlignmentStructures(B);
		else initAlignmentStructures();
		markedThreats = new LinkedList<ThreatApplied>(B.markedThreats);	//copy marked threats
		
		copy(B);
		copyMC(B);
	}

	public BoardBitDb getCopy(boolean copy_threats) {
		return new BoardBitDb(this, copy_threats);
	}

	public void copy(BoardBitDb B) {

		// copy all
		for(int j = 0; j < N; j++) {
			for(int i = 0; i < COL_SIZE(M); i++) {
				board[j][i]			= B.board[j][i];
				board_mask[j][i]	= B.board_mask[j][i];
			}
			free[j] = B.free[j];
		}

		free_n = B.free_n;

		// debug
		count = B.count;
	}
	

	//#region BOARD

		/**
		 * Mark cell; also remove opponent's alignments, but doesn't find new ones.
		 * @param i
		 * @param j
		 * @param player
		 */
		private void mark(int i, int j, byte player) {
			markCheck(j, player);
			addMC(i, j, cellStateCX(i, j));
			removeAlignments(new MovePair(i, j), Auxiliary.opponent(player));
		}
		public void mark(int j, byte player) {
			mark(free[j], j, player);
		}

		//public void markCell(MovePair cell) {markCell(cell.i(), cell.j(), Player[currentPlayer]);}
		public void mark(MovePair cell, byte player) {mark(cell.i, cell.j, player);}
		public void markMore(MovePair[] cells, byte player) {
			for(MovePair c : cells) mark(c.i, c.j, player);
		}
		public void markThreat(MovePair[] related, int atk_index) {

			for(int i = 0; i < related.length; i++) {
				byte state = Player_byte[(i == atk_index) ? currentPlayer : (1 - currentPlayer)];
				if(free[related[i].j] == related[i].i)
					// check needed for vertical threats (not optimized), not to repeat moves
					mark(related[i].i, related[i].j, state);
			}
		}

		/**
		 * Remove and re-calculate alignments for a cell.
		 * @param cell
		 * @param player
		 */
		public void updateAlignments(MovePair cell, byte player) {
			
			// remove alignments for both players involving this cell
			removeAlignments(cell, Auxiliary.opponent(player));
			removeAlignments(cell, player);

			// add alignments for player
			findAlignments(cell, cell, player, Operators.TIER_MAX, null, null, true, -1, "update_");

			//update gameState
			if(free_n == 0 && game_state == GameState.OPEN) game_state = GameState.DRAW;
			//currentPlayer = (currentPlayer + 1) % 2;
		}


		private void checkAlignments(MovePair cell, int max_tier, int dir_excluded, String caller) {

			if(isWinningMove(cell.i, cell.j))
				game_state = cell2GameState(cell.i, cell.j);
			else {
				findAlignments(cell, cell, cellState(cell), max_tier, null, null, true, dir_excluded, caller + "checkOne_");
				if(free_n == 0 && game_state == GameState.OPEN) game_state = GameState.DRAW;
			}
		}
		public void checkAlignments(MovePair[] cells, int max_tier, String caller) {
			if(cells.length == 1) {
				checkAlignments(cells[0], max_tier, -1, caller + "checkArray_");
			} else {
				MovePair dir = cells[0].getDirection(new MovePair(cells[1]));
				int dir_index = dirsIndexes(dir);

				for(int i = 0; i < cells.length && game_state == GameState.OPEN; i++)
					checkAlignments(cells[i], max_tier, dir_index, caller + "checkArray_");

				if(game_state == GameState.OPEN) {
					for(int i = 1; i < cells.length; i++)
						findAlignmentsInDirection(cells[i], cells[i-1], cellState(cells[i]), dir_index, max_tier, null, null, true, caller + "checkArray_");
				}
				//update gameState
				if(free_n == 0 && game_state == GameState.OPEN) game_state = GameState.DRAW;
			}
		}

	//#endregion BOARD


	//#region DB_SEARCH

		/**
		 * 
		 * @param threat as defined in Operators
		 * @param atk attacker's move index in threat
		 * @param use as def in Operators
		 * @param threats wether to update alignments and threats for this board
		 * @return :	a new board resulting after developing this with such threat (dependency stage);
		 * 				the new board only has alignment involving the newly marked cells
		 */
		public BoardBitDb getDependant(ThreatCells threat, int atk, USE use, int max_tier, boolean check_threats) {
			
			BoardBitDb res = new BoardBitDb(this, false);
			
			switch(use) {
				//used for...
				case ATK:
					MovePair cell = threat.related[threat.nextAtk(atk)];
					res.mark(cell.i, cell.j, Player_byte[currentPlayer]);
					if(check_threats) res.checkAlignments(cell, max_tier, -1, "dep");
					break;
				//used for init defensive visit (marks defensive cells as own)
				case DEF:
					//res.addThreat(threat, atk, Auxiliary.opponent(Player_byte[currentPlayer]));

					//if there exist any defensive moves
					if(threat.related.length > 1) {
						res.markMore(threat.getDefensive(atk), Player_byte[currentPlayer]);
						//if(check_threats) res.checkAlignments(threat.getDefensive(atk), max_tier, "dep");
					}
					break;
				//used for dependency stage
				case BTH:
					res.markThreat(threat.related, atk);
					res.addThreat(threat, atk, Player_byte[currentPlayer]);
					if(check_threats) res.checkAlignments(threat.related, max_tier, "dep");
			}
			return res;
		}
		
		//only checks for alignments not included in the union of A's and B's alignments, i.e. those which involve at  least one cell only present in A and one only in B
		public BoardBitDb getCombined(BoardBitDb B, byte attacker, int max_tier) {

			BoardBitDb res = new BoardBitDb(this, true);

			for(ThreatApplied athreat : B.markedThreats) {
				if(this.isUsefulThreat(athreat, attacker)) {
					//mark other board's threat on res
					for(int i = 0; i < athreat.threat.related.length; i++) {
						MovePair c = athreat.threat.related[i];

						if(cellFree(c.i, c.j)) {
							if(i == athreat.related_index) res.mark(c, athreat.attacker);
							else res.mark(c, Auxiliary.opponent(athreat.attacker));
						}
					}
					//add threats
					res.addThreat(athreat);
				}
			}

			//re-calculate alignments
			res.addAllCombinedAlignments(B, attacker, max_tier);

			return res;
		}

		@Override
		public BoardsRelation validCombinationWith(BoardBitDb B, byte attacker) {
			// TODO Auto-generated method stub
			return null;
		}
		
	//#endregion DB_SEARCH


	//#region AUXILIARY
		private void addMC(int y, int x, CXCellState player) {MC[MC_n++] = new CXCell(y, x, player);}
		public void addThreat(ThreatCells threat, int atk, byte attacker) {
			// from when appliedThreat was different:
			//MovePair[] def = new MovePair[threat.related.length - 1];
			//for(int i = 0; i < atk; i++) def[i] = threat.related[i];
			//for(int i = atk + 1; i < threat.related.length; i++) def[i - 1] = threat.related[i];
			ThreatApplied at = new ThreatApplied(threat, atk, attacker);
			markedThreats.addLast(at);
		}
		public void addThreat(ThreatApplied a_threat) {
			markedThreats.addLast(a_threat);
		}

		//returns index in lines_per_dir to the line related to this direction
		protected int dirsIndexes(MovePair dir) {
			if(dir.equals(DIRECTIONS[0]))		return 1;
			else if(dir.equals(DIRECTIONS[1]))	return 3;
			else if(dir.equals(DIRECTIONS[2]))	return 0;
			else if(dir.equals(DIRECTIONS[3]))	return 2;
			else if(dir.equals(DIRECTIONS[4]))	return 1;
			else if(dir.equals(DIRECTIONS[5]))	return 3;
			else if(dir.equals(DIRECTIONS[6]))	return 0;
			else /*if(dir==DIRECTIONS[7]) */	return 2;
		}

		//#region ALIGNMENTS

			private void removeAlignments(final MovePair center, byte player) {

				// debug
				//if(DEBUG_ON)
				//	System.out.println("\nremoveAlignments START:");
				
				//foreach alignment that was stored in (y,x)
				BiNode<BiNode<ThreatPosition>> alignments_in_cell = alignments_by_cell[center.i][center.j].getFirst(player);
				if(alignments_in_cell != null)
				{
					// debug
					if(DEBUG_PRINT) System.out.println("remove " + alignments_in_cell.item.item);

					int MAX_ALIGNMENT = X + Operators.MAX_FREE_EXTRA_TOT;
						
					do {
						MovePair	start	= alignments_in_cell.item.item.start,
									end		= alignments_in_cell.item.item.end,
									dir		= start.getDirection(end);

						// DEBUG
						//if(DEBUG_ON) {
						if(DEBUG_PRINT) {
							System.out.println("\t\trm: " + alignments_in_cell.item.item);
							System.out.println("\t\t" + alignmentsByDirection_index(dir, center));
						}

						//delete for line
						alignments_by_direction[dirsIndexes(dir)].remove(player, alignmentsByDirection_index(dir, center), alignments_in_cell.item);

						//delete for this cell
						BiNode<BiNode<ThreatPosition>> tmp = alignments_in_cell;
						alignments_in_cell = alignments_in_cell.next;
						alignments_by_cell[center.i][center.j].remove(player, tmp);

		

					} while(alignments_in_cell != null);
					
					//delete for each involved cell
					MovePair first, last;
					for(int d = 0; d < alignments_direction_indexes.length; d++)
					{
						MovePair dir = DIRECTIONS[alignments_direction_indexes[d]];
						first	= new MovePair(center);
						last	= new MovePair(center);
						first.clamp_diag(MIN, MAX, DIRECTIONS[alignments_direction_indexes[d]].getProduct(-(MAX_ALIGNMENT - 1)));
						last.clamp_diag (MIN, MAX, DIRECTIONS[alignments_direction_indexes[d]].getProduct(MAX_ALIGNMENT - 1));

						for(; !first.equals(last); first.sum(dir)) {
							// already removed in the `center` cell
							if(!first.equals(center))
								removeAlignmentsByCell_InvolvingCell(first, center, player);
						}
					}

				}

				// debug
				//if(DEBUG_ON)
				//	System.out.println("removeAlignments END\n");
			}

			/**
			 * Find all alignments involving at least one cell between the aligned first, second.
			 * Also, if check1 and check2 != null, checks that the new threat involves at least one cell only present in 1, and one only in 2.
			 * 
			 * 1 -	the cells whose alignments will change are those at max distance K-1 from this;
			 * 2 -	considering the algorithm is correct, we assume hey already have associated, if already
			 * 		existed, alignments from K-MIN_SYM_LINE to K-1 symbols (if existed of K, the game would be ended);
			 * 3 -	that said, we will only need to increase existing alignments by 1 symbol, and to add
			 * 		new alignments of K-MIN_SYM_LINE.
			 * HOWEVER, this will be a future enhancement: for now, the function simply deletes and recreates all
			 * 
			 * @param first
			 * @param second
			 * @param player
			 * @param dir_index
			 * @param max_tier
			 * @param check1
			 * @param check2
			 * @param only_valid if true, only search for immediately applicable threats, i.e. free[j] = i, for each (i,j) in threat
			 * @param caller
			 */
			private void findAlignmentsInDirection(final MovePair first, final MovePair second, final byte player, int dir_index, int max_tier, BoardBitDb check1, BoardBitDb check2, boolean only_valid, String caller) {

				String filename = "debug/db2/" + player + "_" + caller + count + "_" + (int)(Math.random() * 99999) + "_.txt";
				count++;
				
				try {
					//System.out.println(first + " " + second + " " + player + " " + dir_index);

					// debug
					int added = 0;
					int c1found = 0;
					if(DEBUG_ON) {
						file = new FileWriter(filename);
						printFile(file, 0);
						file.write("\naddAlignments START, for player " + player + ", moves " + first + " " + second + ":\n");
					}


					byte opponent						= Auxiliary.opponent(player);
					MovePair dir						= DIRECTIONS[alignments_direction_indexes[dir_index]];
					int alignments_by_direction_index	= alignmentsByDirection_index(dir, first);							//if horizontal: row index, otherwise (all other cases) col index
					MovePair negdir						= dir.getNegative();
					int MIN_MARKS						= X - Operators.MARK_DIFF_MIN;
					/*	
					MovePair c*:
						center = starting cell, end_c* = last to check for c*, c1,c2 = iterators (first and last in line to check)
						c1 goes from center-MAX_LEN to center, c2 from c1 to center+MAX_LEN
					*/

					// make such that c1 is before c2
					if(dir.equals( first.getDirection(second)) ) {
						c1.reset(first);
						c2.reset(second);
					} else {
						c1.reset(second);
						c2.reset(first);
					}
					end_c1.reset(c2);
					end_c2.reset(c2);
					end_c1.clamp_diag(MIN, MAX, dir.getProduct(Operators.MAX_FREE_EXTRA - 1) );
					end_c2.clamp_diag(MIN, MAX, dir.getProduct(X + Operators.MAX_FREE_EXTRA - 1) );
					int	lined	= 0,	// alignment length, i.e. max distance
						marks	= 0,	// marks in alignment
						in		= 0,	// inside alignment
						before	= 0,	// 
						after	= 0;	// 
					
					// find furthest c1, from center
					int distance = 0;
					c_it.reset(c1);
					while(	distance < X + Operators.MAX_FREE_EXTRA - 1 && c_it.inBounds(MIN, MAX)
							&& (cellState(c_it) != opponent)
							&& ((dir_index == 1) || (free[c_it.j] == c_it.i) || (!only_valid && (free[c_it.j] < c_it.i)) || (cellState(c_it) == player) )
					) {
						if(cellState(c_it) == player) {
							c1.reset(c_it);

							// debug
							c1found++;
						}
						c_it.sum(negdir);
						distance++;
					}
					c2.reset(c1);

					// debug
					if(DEBUG_ON) file.write("\t\tdir: " + dir + "\n");

					boolean	checked_all = false,
							found1 = false, found2 = false,		// checks if found in check1, check2
							to_check = (check1 != null && check2 != null);
					while(!checked_all) {

						// debug
						if(DEBUG_ON) file.write("\t\t\t" + c1 + "->" + c2 + " : " + lined + ", " + marks + ", " + in + "\n");
						
						if(cellFree(c1.i, c1.j)) {

							// debug
							if(DEBUG_ON) file.write("\t\t\t\tbefore checking c1!=player: " + c1 + "->" + c2 + " : " + lined + ", " + marks + ", " + in + "\n");

							// find start of an alignment, i.e. c1 contains player
							while(cellState(c1) != player && !c1.equals(end_c1))
								c1.sum(dir);

							if(c1.equals(end_c1) && cellFree(c1.i, c1.j)) checked_all = true;
							else c2.reset(c1);
						}
						
						if(!checked_all) {
							
							// debug
							c1found++;
							//while (c2 == empty && !(c2 reached end_c2) ) line++, in++, c2++;		//impossible at first iteration, when c2=c1, because of the lines above

							while(cellFree(c2.i, c2.j) && !c2.equals(end_c2) && (!only_valid || (dir_index == 1) || (free[c2.j] == c2.i)) ) {
								//doesn't update line,in when c2==end_c2; however not needed, since in that case it would not check for alignments
								lined++;
								in++;
								c2.sum(dir);
							}

							// debug
							if(DEBUG_ON) file.write("\t\t\t\tafter found c2 not empty: " + c1 + "->" + c2 + " : " + lined + ", " + marks + ", " + in + "\n");

							//if ( !(line exceeded MAX) && c2 == player): line++, mark++; check alignment;

							if(lined <= X && cellState(c2) == player) {
								lined++;
								marks++;

								if(to_check) {
									found1 = false;
									found2 = false;
									if		(!check1.cellFree(c2.i, c2.j) && check2.cellFree(c2.i, c2.j))	found1 = true;
									else if	(!check2.cellFree(c2.i, c2.j) && check1.cellFree(c2.i, c2.j))	found2 = true;
								}

								// debug
								if(DEBUG_ON) file.write("\t\t\t\tafter check c2 player: " + c1 + "->" + c2 + " : " + lined + ", " + marks + ", " + in + "\n");

								//check alignments
								if(marks >= MIN_MARKS && (!to_check || (found1 && found2)) ) {
									int tier = X - marks;
									if(tier <= max_tier)
									{
										//foreach alignment of mark marks
										for(byte threat_code : Operators.ALIGNMENT_CODES[tier])
										{
											Operators.AlignmentPattern alignment = Operators.ALIGNMENTS[tier].get((int)threat_code);

											// debug
											if(DEBUG_ON) file.write("\t\t\t\t\tstart checking alignment = " + alignment + "\n");

											//if (inner alignment conditions)
											if(lined == X + alignment.line && marks == X + alignment.mark && in == alignment.in) {

												//assuming that win is K marks aligned, without free cells, checks wether the game ended
												if(tier == 0) {
													game_state = cell2GameState(first.i, first.j);
													return;
												}

												//check outer alignment conditions
												before = countFree( c1.getSum(dir.getNegative()), dir.getNegative(), alignment.out - alignment.mnout);
												if(before >= alignment.mnout)
													after = countFree(c2.getSum(dir), dir, alignment.out - before);

												// debug
												if(DEBUG_ON) file.write("\t\t\t\t\tbefore, after = " + before + "," + after + "\n");

												//if (outer conditions)
												threat_start.resetVector(c1, dir, -before);
												threat_end.resetVector(c2, dir, after);

												for( ;
													before >= alignment.mnout && after >= alignment.mnout && before + after >= alignment.out											// alignment conditions
													&& threat_end.inBounds(MIN, MAX) && (after == 0 || (cellFree(threat_end.i, threat_end.j) && (!only_valid || (free[threat_end.j] == threat_end.i)) ) )	// in bounds and player's cells
													; after++, before--, threat_start.sum(dir), threat_end.sum(dir)
													) {
														ThreatPosition threat_pos = new ThreatPosition(threat_start, threat_end, threat_code);

														// debug
														added++;
														if(DEBUG_ON) file.write("found threat: " + threat_start + "_( " + c1 + "->" + c2 + ") _" + threat_end + " : " + threat_pos + "\n");

														//add to arrays
														BiNode<ThreatPosition> node = alignments_by_direction[dir_index].add(player, alignments_by_direction_index, threat_pos);				//add to array for alignments in row/col/diag

														// debug
														if(DEBUG_ON) file.write("dir_index, dir index in alignment dir, is alignments empty in dir: " + dir_index + ", " + alignments_by_direction_index + " : " +  alignments_by_direction[dir_index].get(alignments_by_direction_index).isEmpty(player) + "\n");

														//add reference for all in the middle
														for(c_it.reset(threat_start); ; c_it.sum(dir)) {
															alignments_by_cell[c_it.i][c_it.j].add(player, node);	//add to cell's alignments
															if(c_it.equals(threat_end))								// add for threat_end and break
																break;
														}

														// debug
														//if(debug) System.out.println("cell empty: " + c_it + " : " + cells_lines[c_it.i()][c_it.j()].isEmpty(player));
													}

											}

										}
									}
								}
							}	//end if (c2==player)

							//increment c1/c2
							if(c2.equals(end_c2) || lined >= X || cellState(c2) == opponent || (only_valid && free[c2.j] < c2.i) ) {
								if(c1.equals(end_c1)) checked_all = true;
								else {
									c1.sum(dir);
									c2.reset(c1);
									lined = marks = in = 0;
									found1 = found2 = false;
								}
							}
							else c2.sum(dir);
						}	//end if (!checked_all)
					}	//end while

					// debug
					if(DEBUG_ON) {
						file.write("addAlignments END;\n");
					}
					if(c1found == -1) throw new IOException("cacca");
					if(added == -1) throw new IOException("cacca");
					else if(DEBUG_ON) file.close();

				} catch (IOException e) {
					File todel = new File(filename);
					todel.delete();
				}

			}

			/**
			 * Find alignments for a cell in all directions.
			 */
			private void findAlignments(final MovePair first, final MovePair second, final byte player, int max_tier, BoardBitDb check1, BoardBitDb check2, boolean only_valid, int dir_excluded, String caller) {
				for(int d = 0; d < alignments_direction_indexes.length; d++) {
					if(d != dir_excluded)
						findAlignmentsInDirection(first, second, player, d, max_tier, check1, check2, only_valid, caller + "find_");
				}
			}

			/**
			 * Find all alignments for a player.
			 * @param player
			 * @param max_tier
			 */
			public void findAllAlignments(byte player, int max_tier, boolean only_valid, String caller) {

				MovePair start, end;
				for(int d = 0; d < alignments_direction_indexes.length; d++)
				{
					for(start = iterateAlignmentDirs(null, d), end = new MovePair();
						start.inBounds(MIN, MAX);
						start = iterateAlignmentDirs(start, d)
					) {
						end.reset(start);
						end.clamp_diag(MIN, MAX, DIRECTIONS[alignments_direction_indexes[d]].getProduct(Math.max(M, N)) );
						findAlignmentsInDirection(start, end,  player, d, max_tier, null, null, only_valid, caller + "all_");
					}
				}
			}

			private void addAllCombinedAlignments(BoardBitDb B, byte player, int max_tier) {

				for(int alignments_by_direction_index = 0; alignments_by_direction_index < alignments_by_direction.length; alignments_by_direction_index++) {
					MovePair start = iterateAlignmentDirs(null, alignments_by_direction_index);
					for(int i = 0; i < alignments_by_direction[alignments_by_direction_index].size();
						i++, start = iterateAlignmentDirs(start, alignments_by_direction_index))
					{
						findAlignmentsInDirection(start, start, player, alignments_by_direction_index, max_tier, this, B, true, "combined_");
					}
				}
			}


			private int countFree(MovePair start, MovePair incr, int max) {
				int count = 0;
				while(count < max && start.inBounds(MIN, MAX) && cellFree(start.i, start.j)  && (incr.equals(DIRECTIONS[alignments_direction_indexes[1]]) || (free[start.j] == start.i)) ) {
					count++;
					start.sum(incr);
				}
				return count;
			}

			/**
			 * Remove all alignments in alignments_by_cell[from] involving involved.
			 */
			private void removeAlignmentsByCell_InvolvingCell(MovePair from, MovePair involved, byte player) {

				MovePair dir_involved = from.getDirection(involved);
				
				BiNode<BiNode<ThreatPosition>> node_alignment, node_tmp;
				BiList_Node_ThreatPos alignments_in_cell = alignments_by_cell[from.i][from.j];
				node_alignment = alignments_in_cell.getFirst(player);

				while(node_alignment != null) {
					
					// debug
					if(DEBUG_PRINT) System.out.println("remove " + node_alignment.item.item);
					
					MovePair start			= node_alignment.item.item.start,
							 end			= node_alignment.item.item.end;
					MovePair dir_alignment	= start.getDirection(end);
					
					node_tmp		= node_alignment;
					node_alignment	= node_alignment.next;

					//remove if involved
					if(dirsIndexes(dir_alignment) == dirsIndexes(dir_involved) && involved.inBounds_included(start, end))
						alignments_in_cell.remove(player, node_tmp);
				}
			}

			/**
			 * index for alignments_by_direction.
			 * @return the index where, in alignments_by_direction, is contained position in direction dir
			 */
			private int 
			alignmentsByDirection_index(MovePair dir, MovePair position) {
				if(dir.i == 0)				return position.i;				//horizontal
				else if(dir.j == 0)			return position.j;				//vertical
				else if(dir.i == dir.j)		return dir.j - dir.i + M - 1;	//dright
				else return dir.i + dir.j;									//dleft
			}

			private MovePair iterateAlignmentDirs(MovePair start, int lines_dirs_index) {
				if(start == null) {
					if(lines_dirs_index == 2) start = new MovePair(M - 1, 0);	//dright
					else start = new MovePair(0, 0);
				} else {
					if(lines_dirs_index == 0) start.reset(start.i + 1, start.j);
					else if(lines_dirs_index == 1) start.reset(start.i, start.j + 1);
					else if(lines_dirs_index == 2) {
						if(start.i == 0) start.reset(start.i, start.j + 1);
						else start.reset(start.i - 1, start.j);
					}
					else {
						if(start.j == N - 1) start.reset(start.i + 1, start.j);
						else start.reset(start.i, start.j + 1);
					}
				}
				return start;
			}

			/**
			 * 
			 * @param player
			 * @return true if there are valid alignments (calculated before, with proper max_tier)
			 */
			public boolean hasAlignments(byte player) {

				for(int i = 0; i < alignments_by_direction.length; i++) {
					for(int j = 0; j < alignments_by_direction[i].size(); j++) {
						BiList_ThreatPos t = alignments_by_direction[i].get(j);
						if(t != null && !t.isEmpty(player)) return true;
					}
				}
				return false;
			}

			/**
			 * 
			 * @param athreat
			 * @param attacker
			 * @return true if the AppliedThreat is compatible with this board, and useful, i.e. adds at least one attacker's threat.
			 */
			private boolean isUsefulThreat(ThreatApplied athreat, byte attacker) {

				byte state, state_athreat;
				boolean useful = false;
				
				for(int i = 0; i < athreat.threat.related.length; i++) {
					state			= cellState(athreat.threat.related[i]);
					state_athreat	= (i == athreat.related_index) ? athreat.attacker : Auxiliary.opponent(athreat.attacker);
					if(state == CellState.FREE) {						//compatible
						if(state_athreat == attacker) useful = true;	//useful
					} else if(state != state_athreat) return false;		//not compatible
				}

				return useful;
			}

			@Override
			public ThreatsByRank getApplicableOperators(byte attacker, int max_tier) {

			byte defender		= Auxiliary.opponent(attacker);
			ThreatsByRank res	= new ThreatsByRank();

			for(AlignmentsList alignments_by_row : alignments_by_direction) {
				for(BiList_ThreatPos alignments_in_row : alignments_by_row) {
					if(alignments_in_row != null) {
						
						BiNode<ThreatPosition> alignment = alignments_in_row.getFirst(attacker);
						if(alignment != null && Operators.tier(alignment.item.type) <= max_tier) {
							do {
								ThreatCells cell_threat_operator = Operators.applied(this, alignment.item, attacker, defender);

								if(cell_threat_operator != null) res.add(cell_threat_operator);
								alignment = alignment.next;

							} while(alignment != null);
						}
					}
				}
			}

			return res;
		}

		public int[] getThreatCounts(byte player) {

			setPlayer(player);
			findAllAlignments(player, Operators.TIER_MAX, false, "selCol_");
	
			int[] threats_by_col = new int[N];
			for(int i = 0; i < M; i++) {
				for(int j = 0; j < N; j++) {
					if(cellFree(i, j)) {
						BiNode<BiNode<ThreatPosition>> alignments = alignments_by_cell[i][j].getFirst(player);
						while(alignments != null) {
							threats_by_col[j] += Operators.indexInTier(alignments.item.item.type);
							alignments = alignments.next;
						}
					}
				}
			}
	
			return threats_by_col;
		}

		//#endregion ALIGNMENTS
		
	//#endregion AUXILIARY


	//#region GET
	
		public int getCurrentPlayer() {return currentPlayer;}
	
		public void setPlayer(byte player) {currentPlayer = (player == this.Player_byte[0]) ? 0 : 1;}

		public int getMC_n() {return MC_n;}
		public CXCell getMarkedCell(int i) {return MC[i];}
		public LinkedList<ThreatApplied> getMarkedThreats() {return markedThreats;}

		@Override
		public long getHash() {return hash;}
	
	//#endregion GET

	//#region INIT

		@Override
		protected void createStructures() {
			board		= new long[N][COL_SIZE(M)];
			board_mask	= new long[N][COL_SIZE(M)];
			free		= new byte[N];
			free_n = 0;
			MC			= new CXCell[M*N];
			MC_n = 0;
		}
		private void initAlignmentStructures() {
			alignments_rows			= new AlignmentsList(M);
			alignments_cols			= new AlignmentsList(N);
			alignments_diagright	= new AlignmentsList(M + N - 1);
			alignments_diagleft		= new AlignmentsList(M + N - 1);
			alignments_by_direction	= new AlignmentsList[]{alignments_rows, alignments_cols, alignments_diagright, alignments_diagleft};
			alignments_by_cell		= new BiList_Node_ThreatPos[M][N];

			for(int i = 0; i < M; i++) {
				for(int j = 0; j < N; j++)
					alignments_by_cell[i][j] = new BiList_Node_ThreatPos();
			}
		}
		//#region COPY

			// Sets to free all board cells
			/*
			private void initStructures() {

				free_n = M * N;
				for(int j = 0; j < N; j++) {
					for(int i = 0; i < COL_SIZE(M); i++) {
						board[j][i]			= 0;
						board_mask[j][i]	= 0;
					}
					free[j] = 0;
				}
				markedThreats = new LinkedList<ThreatApplied>();
			}
			*/

			public void copyMC(BoardBitDb B) {
				MC_n = B.MC_n;
				for(int i = 0; i < MC_n; i++) MC[i] = Auxiliary.copyCell(B.MC[i]);
			}
			/**
			 * fill the MC checking the board
			 * @param B
			 */
			private void copyMCfromBoard(BoardBit B) {
				MC_n = 0;
				hash = 0;
				for(int i = 0; i < M; i++) {
					for(int j = 0; j < N; j++) {
						if(!cellFree(i, j)) {
							MC[MC_n++] = new CXCell(i, j, cellStateCX(i, j));
							hash = TT.getHash(hash, i, j, (Player_byte[currentPlayer] == MY_PLAYER) ? 0 : 1);
						}
					}
				}
			}

			private void copyAlignmentStructures(BoardBitDb DB) {
				alignments_rows			= new AlignmentsList(DB.alignments_rows);
				alignments_cols			= new AlignmentsList(DB.alignments_cols);
				alignments_diagright	= new AlignmentsList(DB.alignments_diagright);
				alignments_diagleft		= new AlignmentsList(DB.alignments_diagleft);
				alignments_by_direction	= new AlignmentsList[]{alignments_rows, alignments_cols, alignments_diagright, alignments_diagleft};
				alignments_by_cell		= new BiList_Node_ThreatPos[M][N];
				for(int i = 0; i < M; i++) {
					for(int j = 0; j < N; j++)
						alignments_by_cell[i][j] = new BiList_Node_ThreatPos();
				}
				for(int d = 0; d < alignments_by_direction.length; d++) {
					AlignmentsList alignment	= alignments_by_direction[d];
					MovePair dir				= DIRECTIONS[alignments_direction_indexes[d]];
					for(int i = 0; i < alignment.size(); i++) {
						if(alignment.get(i) != null) {
							copyAlignmentInCells(alignment.getFirst(CellState.P1, i), CellState.P1, dir);
							copyAlignmentInCells(alignment.getFirst(CellState.P2, i), CellState.P2, dir);
						}
					}
				}
			}
			private void copyAlignmentInCells(BiNode<ThreatPosition> alignment_node, byte player, MovePair dir) {

				if(alignment_node != null) {
					copyAlignmentInCells(alignment_node.next, player, dir);
					
					for(MovePair it = new MovePair(alignment_node.item.start), end = alignment_node.item.end;
						!it.equals(end); it.sum(dir))
							alignments_by_cell[it.i][it.j].add(player, alignment_node);
				}
			}
		//#endregion COPY
	//#endregion INIT


	//#region DEBUG

			public void printAlignments() {

				String indent = "";
				for(int i = 0; i < 0; i++) indent += '\t';

				System.out.println(indent + "ALIGNMENTS:\n");
				System.out.println(indent + "by rows:\n");

				for(int d = 0; d < alignments_direction_indexes.length; d++) {
					MovePair dir = DIRECTIONS[alignments_direction_indexes[d]];
					System.out.println(indent + "direction: " + dir);

					for(int player = 0; player < 2; player++) {
						System.out.println(indent + "player " + Player_byte[player] + ":\n");
						for(int i = 0; i < alignments_by_direction[d].size(); i++) {

							if(alignments_by_direction[d].get(i) != null) {
								System.out.println(indent + "index " + i + "\n");
								for(BiNode<ThreatPosition> p = alignments_by_direction[d].getFirst(Player_byte[player], i);
									p != null; p = p.next
								) {
									System.out.println(indent + p.item + "\n");
								}
								
							}
						}
						
					}
				}
				
				System.out.println(indent + "by cells:\n");
				
				for(int player = 0; player < 2; player++)
				{
					for(int i = 0; i < M; i++) {
						for(int j = 0; j < N; j++) {
							BiNode<BiNode<ThreatPosition>> p = alignments_by_cell[i][j].getFirst(Player_byte[player]);
							if(p != null) {
								System.out.println(indent + "cell: " + new MovePair(i, j) + "\n");
								do {
									System.out.println(indent + p.item.item + "\n");
								} while((p = p.next) != null);
							}
						}
					}
				}

			}

			public String printAlignmentsString(int indentation) {

				String	indent = "",
						res = "";
				for(int i = 0; i < indentation; i++) indent += '\t';

				res += indent + "ALIGNMENTS:\n";
				res += indent + "by rows:\n";

				for(int d = 0; d < alignments_direction_indexes.length; d++) {
					MovePair dir = DIRECTIONS[alignments_direction_indexes[d]];
					res += indent + "direction: " + dir + "\n";

					for(int player = 0; player < 2; player++) {
						res += indent + "player " + Player_byte[player] + ":\n\n";
						for(int i = 0; i < alignments_by_direction[d].size(); i++) {

							if(alignments_by_direction[d].get(i) != null) {
								res += indent + "index " + i + "\n\n";
								for(BiNode<ThreatPosition> p = alignments_by_direction[d].getFirst(Player_byte[player], i);
									p != null; p = p.next
								) {
									res += indent + p.item + "\n\n";
								}
								
							}
						}
						
					}
				}
				
				res += indent + "by cells:\n\n";
				
				for(int player = 0; player < 2; player++)
				{
					for(int i = 0; i < M; i++) {
						for(int j = 0; j < N; j++) {
							BiNode<BiNode<ThreatPosition>> p = alignments_by_cell[i][j].getFirst(Player_byte[player]);
							if(p != null) {
								res += indent + "cell: " + new MovePair(i, j) + "\n\n";
								do {
									res += indent + p.item.item + "\n\n";
								} while((p = p.next) != null);
							}
						}
					}
				}

				return res;
			}

			public void printAlignmentsFile(FileWriter file, int indentation) {

				try {
					String indent = "";
					for(int i = 0; i < indentation; i++) indent += '\t';

					file.write(indent + "ALIGNMENTS:\n");
					file.write(indent + "by rows:\n");

					for(int d = 0; d < alignments_direction_indexes.length; d++) {
						MovePair dir = DIRECTIONS[alignments_direction_indexes[d]];
						file.write(indent + "direction: " + dir + "\n");

						for(int player = 0; player < 2; player++) {
							file.write(indent + "player " + Player_byte[player] + ":\n");
							for(int i = 0; i < alignments_by_direction[d].size(); i++) {

								if(alignments_by_direction[d].get(i) != null) {
									file.write(indent + "index " + i + "\n");
									for(BiNode<ThreatPosition> p = alignments_by_direction[d].getFirst(Player_byte[player], i);
										p != null; p = p.next
									) {
										file.write(indent + p.item + "\n");
									}
									
								}
							}
							
						}
					}
					
					file.write(indent + "by cells:\n");
					
					for(int player = 0; player < 2; player++)
					{
						for(int i = 0; i < M; i++) {
							for(int j = 0; j < N; j++) {
								BiNode<BiNode<ThreatPosition>> p = alignments_by_cell[i][j].getFirst(Player_byte[player]);
								if(p != null) {
									file.write(indent + "cell: " + new MovePair(i, j) + "\n");
									do {
										file.write(indent + p.item.item + "\n");
									} while((p = p.next) != null);
								}
							}
						}
					}

				} catch (IOException e) {}
			}
	
	//#endregion DEBUG

}