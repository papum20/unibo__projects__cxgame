package pndbg.alpha;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import connectx.CXCell;
import pndbg.alpha._Operators.AlignmentPattern;
import pndbg.alpha.threats.AlignmentsList;
import pndbg.alpha.threats.BiList_Node_ThreatPos;
import pndbg.alpha.threats.ThreatPosition;
import pndbg.constants.Auxiliary;
import pndbg.constants.CellState;
import pndbg.constants.MovePair;
import pndbg.structures.BiList.BiNode;




public class BoardBitDb extends _BoardBitDb<BoardBitDb, BoardBit> {
	

	public CXCell[] MC; 							// Marked Cells
	private int MC_n;								// marked cells number

	protected BiList_Node_ThreatPos[][] alignments_by_cell;




	BoardBitDb(int M, int N, int X, _Operators operators) {
		super(M, N, X, operators);
	}

	/**
	 * Complexity: O(3M+7N + MN) = O(10N + N**2)
	 * @param B
	 */
	BoardBitDb(BoardBit B, _Operators operators) {
		super(B, operators);
		copyMCfromBoard(B);
	}
	
	/**
	 * Complexity:
	 * 		with mc: O(3M + 7N + B.marked_threats.length + MN) = O(B.marked_threats.length + N**2 + 10N)
	 * 		no mc: O(3M + 7N + B.marked_threats.length) = O(B.marked_threats.length + 10N)
	 * @param B
	 * @param copy_threats
	 */
	BoardBitDb(BoardBitDb B, boolean copy_threats, _Operators operators) {
		super(B, copy_threats, operators);
		copyMC(B);
	}

	/**
	 * Complexity: O(marked_threats.length + N**2 + 13N)
	 */
	public BoardBitDb getCopy(boolean copy_threats) {
		return new BoardBitDb(this, copy_threats, OPERATORS);
	}
	

	//#region BOARD

		/**
		 * Mark cell; also remove opponent's alignments, but doesn't find new ones.
		 * @param i
		 * @param j
		 * @param player
		 */
		@Override
		protected void mark(int i, int j, byte player) {
			markCheck(j, player);
			MC[MC_n++] = new CXCell(i, j, cellStateCX(i, j));
			removeAlignments(new MovePair(i, j), Auxiliary.opponent(player));
		}

	//#endregion BOARD


	//#region AUXILIARY

		//#region ALIGNMENTS

			@Override
			protected void removeAlignments(final MovePair center, byte player) {

				// debug
				//if(DEBUG_ON)
				//	System.out.println("\nremoveAlignments START:");
				
				//foreach alignment that was stored in (y,x)
				BiNode<BiNode<ThreatPosition>> alignments_in_cell = alignments_by_cell[center.i][center.j].getFirst(player);
				if(alignments_in_cell != null)
				{
					// debug
					if(DEBUG_PRINT) System.out.println("remove " + alignments_in_cell.item.item);

					int MAX_ALIGNMENT = X + OPERATORS.MAX_OUT;
						
					do {
						MovePair	start	= alignments_in_cell.item.item.start,
									end		= alignments_in_cell.item.item.end,
									dir		= start.getDirection(end);

						// DEBUG
						//if(DEBUG_ON) {
						if(DEBUG_PRINT) {
							System.out.println("\t\trm: " + alignments_in_cell.item.item);
							System.out.println("\t\t" + getIndex_for_alignmentsByDir(dir, center));
						}

						//delete for line
						alignments_by_dir[dirIdx_fromDir(dir)].remove(player, getIndex_for_alignmentsByDir(dir, center), alignments_in_cell.item);

						//delete for this cell
						BiNode<BiNode<ThreatPosition>> tmp = alignments_in_cell;
						alignments_in_cell = alignments_in_cell.next;
						alignments_by_cell[center.i][center.j].remove(player, tmp);

					} while(alignments_in_cell != null);
					
					//delete for each involved cell
					MovePair first, last;
					for(int d = 0; d < DIR_ABS_N; d++)
					{
						MovePair dir = DIRECTIONS[d];
						first	= new MovePair(center).clamp_diag(MIN, MAX, DIRECTIONS[d], -(MAX_ALIGNMENT - 1) );
						last	= new MovePair(center).clamp_diag(MIN, MAX, DIRECTIONS[d], MAX_ALIGNMENT - 1 );

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
					if(dirIdx_fromDir(dir_alignment) == dirIdx_fromDir(dir_involved) && involved.inBounds_included(start, end))
						alignments_in_cell.remove(player, node_tmp);
				}
			}

						
			/**
			 * Find all alignments involving at least one cell between the aligned first, second.
			 * Also, if check1 and check2 != null, checks that the new threat involves at least one cell only present in 1, and one only in 2.
			 * 
			 * 1 -	the cells whose alignments will change are those at max distance K-1 from this;
			 * 2 -	considering the algorithm is correct, we assume they already have associated, if already
			 * 		existed, alignments from K-MIN_SYM_LINE to K-1 symbols (if existed of K, the game would be ended);
			 * 3 -	that said, we will only need to increase existing alignments by 1 symbol, and to add
			 * 		new alignments of K-MIN_SYM_LINE.
			 * HOWEVER, this will be a future enhancement: for now, the function simply deletes and recreates all.  
			 * 
			 * Complexity: variable, worst cases (with worst approximation):
			 * 	-	whole line: O(max{M,N}**2 * 6 * 3) = O(18 max{M,N}**2), 
			 * 			where 6 is the max number of alignments to check in a tier,
			 * 			and 3 is the maximum number of free cells on one side of an alignment (i.e. number of iteration for before, after).
			 *  -	single cell: O((2X)**2 * 6 * 3)
			 * 				= O(72 X**2),
			 * 			as c1,c2 get at most X cells distant from center, on each side.
			 *  -	sequence: O( (2X+(second-first))**2 * 6*3 )
			 * 				= O(18(2X+second-first)**2 )
			 * 				= O(18(3X)**2 ) if second-first==X
			 * 				= O(162X**2 )
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
			@Override
			protected void findAlignmentsInDirection(MovePair first, MovePair second, byte player, int dir_index, int max_tier, _BoardBitDb<BoardBitDb, ?> check1, _BoardBitDb<BoardBitDb, ?> check2, boolean only_valid, String caller) {

				String filename = "debug/db2/" + player + "_" + caller + count + "_" + (int)(Math.random() * 99999) + "_.txt";
				count++;
				
				try {

					byte opponent						= Auxiliary.opponent(player);
					MovePair dir						= DIRECTIONS[dir_index];
					MovePair dir_neg					= DIRECTIONS[(dir_index + 4) % 8];

					// swap first, second if not first->second in same direction as dir
					if(!dir.equals( first.getDirection(second)) ) {
						MovePair tmp = first;
						first	= second;
						second	= tmp;
					}
					/*	
					MovePair c*:
						center = starting cell, end_c* = after last to check for c* (excluded), c1,c2 = iterators (first and last in line to check)
						c1 goes from center-MAX_LEN to center, c2 from c1 to center+MAX_LEN
					*/

					end_c1.reset(second).clamp_diag(MIN, MAX, dir, OPERATORS.MAX_OUT_ONE_SIDE);
					end_c2.reset(second).clamp_diag(MIN, MAX, dir, X + OPERATORS.MAX_OUT_ONE_SIDE);
					
					int	lined,			// alignment length, i.e. max distance. It's always c2-c1+1
						marks,			// marks in alignment
						in,				// inside alignment
						before,			// 
						after;			// 
					int tier;
					
					boolean	c2_passed_endc1 = false, c1_reset_to_c2 = false,
						found1, found2,			// checks if found in check1, check2
						to_check = (check1 != null && check2 != null);
						
					// debug
					int found = 0;
					if(DEBUG_ON) {
						file = new FileWriter(filename);
						file.write(printString(0) + "\naddAlignments START, for player " + player + ", moves " + first + " " + second + " dir: " + DIRECTIONS[dir_index] + ", end_c1/c2:" + end_c1 + " " + end_c2 + ", onlyvalid:" + only_valid + ":\n");
					}
					if(DEBUG_PRINT) System.out.println(printString(0) + "\naddAlignments START, for player " + player + ", moves " + first + " " + second + " dir: " + DIRECTIONS[dir_index] + ", end_c1/c2:" + end_c1 + " " + end_c2 + ", onlyvalid:" + only_valid + ":\n");
						
					for( _findOccurrenceUntil(c1, c1.reset(first), dir_neg, MAX, X + OPERATORS.MAX_OUT_ONE_SIDE - 1, player, opponent, false, false, only_valid, dir_index)	// find furthest c1 back, from center
						; !c1.equals(end_c1) && !(c2_passed_endc1 && c1_reset_to_c2)
						&& cellState(c1) == player
						; _findOccurrenceUntil(c1, c1.sum(dir), dir, end_c1, MAX_SIDE, player, CellState.NULL, false, true, only_valid, dir_index)							// find first player cell, before end_c1
					) {
						lined = 0;					// always c2-c1+1.
						marks = 0;					// we are assured c1 (thus c2) contains player, or the loop would end.
						in = before = after = 0;	// in = lined - marks = 0
						found1 = found2 = false;
						c2_passed_endc1 = c1_reset_to_c2 = false;
					
						for( c2.reset(c1)
							; !c2.equals(end_c2)
							; c2.sum(dir)
						) {

							// check boards
							if(to_check) {
								if		(!check1.cellFree(c2.i, c2.j) && check2.cellFree(c2.i, c2.j))	found1 = true;
								else if	(!check2.cellFree(c2.i, c2.j) && check1.cellFree(c2.i, c2.j))	found2 = true;
							}

							// update alignment values
							if(c2.equals(end_c1)) c2_passed_endc1 = true;
							lined++;
							if (cellFree(c2.i, c2.j)) {
								if(!only_valid || dir_index == DIR_IDX_VERTICAL || free[c2.j] == c2.i) {
									in++;
									continue;										// c2 must be player's
								} else {
									if(c2.getDistanceAbs(c1) < MIN_MARKS + 2){
										c1.reset(c2);								// skip useless c1's
										c1_reset_to_c2 = true;
									}
									break;
								}
							}
							else if	(cellState(c2) == player) marks++;	
							else {													// opponent: c1++
								if(c2.getDistanceAbs(c1) < MIN_MARKS + 2) {
									c1.reset(c2);
									c1_reset_to_c2 = true;
								}
								break;
							}

							// debug
							if(DEBUG_ON) file.write("\t\t\t\tvalues: " + c1 + "->" + c2 + " : " + lined + ", " + marks + ", " + in + "\n");
							if(DEBUG_PRINT) System.out.println("\t\t\t\tvalues: " + c1 + "->" + c2 + " : " + lined + ", " + marks + ", " + in + "\n");

							if(marks > X)
								break;
							
							tier = X - marks;
							if(marks < MIN_MARKS || (to_check && !(found1 && found2)) || tier > max_tier)
								continue;

							// check alignments, foreach alignment of mark marks
							for(byte threat_code : OPERATORS.alignmentCodes(tier))
							{
								AlignmentPattern alignment = OPERATORS.alignmentPatterns(tier).get((int)threat_code);

								// debug
								if(DEBUG_ON) file.write("\t\t\t\t\tstart checking alignment = " + alignment + "\n");
								if(DEBUG_PRINT) System.out.println("\t\t\t\t\tstart checking alignment = " + alignment + "\n");

								//if (inner alignment conditions)
								if(alignment.isCompatible(X, lined, marks, in)) {

									if(tier == 0) {
										game_state = cell2GameState(first.i, first.j);
										return;
									}

									//add to structures
									for( before = _findOccurrenceUntil(threat_start.reset(c1), c1.getDiff(dir), dir_neg, MAX, alignment.out - alignment.mnout, CellState.FREE, CellState.NULL, true, false, only_valid, dir_index),
										after = _findOccurrenceUntil(threat_end.reset(c2), c2.getSum(dir), dir, MAX, alignment.out - before, CellState.FREE, CellState.NULL, true, false, only_valid, dir_index)
										; before >= alignment.mnout && after >= alignment.mnout && before + after >= alignment.out																// alignment conditions
										&& threat_end.inBounds(MIN, MAX) && (after == 0 || (cellFree(threat_end.i, threat_end.j) && (!only_valid || dir_index == DIR_IDX_VERTICAL || free[threat_end.j] == threat_end.i) ) )	// in bounds and player's cells
										; after++, before--, threat_start.sum(dir), threat_end.sum(dir)
									) {
										ThreatPosition threat_pos = new ThreatPosition(threat_start, threat_end, threat_code);
										BiNode<ThreatPosition> node_threat = alignments_by_dir[dir_index].add(player, getIndex_for_alignmentsByDir(dir, threat_start), threat_pos);		//add to array for alignments in row/col/diag

										for(c_it.reset(threat_start); ; c_it.sum(dir)) {
											alignments_by_cell[c_it.i][c_it.j].add(player, node_threat);
											if(c_it.getDistanceAbs(threat_end) == 0) break;
										}
										
										// debug
										found++;
										if(DEBUG_ON) file.write("found threat: " + threat_start + "_( " + c1 + "->" + c2 + ") _" + threat_end + " : " + threat_pos + "before, after=" + before + " " + after + "\n");
										if(DEBUG_PRINT) System.out.println("found threat: " + threat_start + "_( " + c1 + "->" + c2 + ") _" + threat_end + " : " + threat_pos +  "before, after=" + before + " " + after + "\n");
									}
								}
							}

						}
					}

					// debug
					if(DEBUG_ON) {
						file.write("addAlignments END;\n");
						file.close();
						if(found == 0) {
							File todel = new File(filename);
							todel.delete();
						}
					}
				} catch (IOException e) {}
				catch (Exception e) {
					try{file.write("\n\nERROR\n\n");file.close();} catch(IOException io) {}
					throw e;
				}
				
			}


			@Override
			public int[] getThreatCounts(byte player) {

				setPlayer(player);
				findAllAlignments(player, OPERATORS.MAX_TIER, false, "selCol_");
		
				int[] threats_by_col = new int[N];
				for(int i = 0; i < M; i++) {
					for(int j = 0; j < N; j++) {
						if(cellFree(i, j)) {
							BiNode<BiNode<ThreatPosition>> alignments = alignments_by_cell[i][j].getFirst(player);
							while(alignments != null) {
								threats_by_col[j] += OPERATORS.indexInTier(alignments.item.item.type);
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

		public int getMC_n() {return MC_n;}
		public CXCell getMarkedCell(int i) {return MC[i];}
	
	//#endregion GET

	//#region INIT

		@Override
		protected void createStructures() {
			super.createStructures();
			MC			= new CXCell[M*N];
			MC_n = 0;
		}
		@Override
		protected void initAlignmentStructures() {
			super.initAlignmentStructures();

			alignments_by_cell = new BiList_Node_ThreatPos[M][N];
			
			for(int i = 0; i < M; i++) {
				for(int j = 0; j < N; j++)
					alignments_by_cell[i][j] = new BiList_Node_ThreatPos();
			}
		}
		//#region COPY

			/**
			 * Complexity: worst: O(M*N)
			 */
			private void copyMC(BoardBitDb B) {
				MC_n = B.MC_n;
				for(int i = 0; i < MC_n; i++) MC[i] = Auxiliary.copyCell(B.MC[i]);
			}
			/**
			 * fill the MC checking the board.
			 * Complexity: O(MN)
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

			@Override
			protected void copyAlignmentStructures(BoardBitDb DB) {
				super.copyAlignmentStructures(DB);

				alignments_by_cell = new BiList_Node_ThreatPos[M][N];
				for(int i = 0; i < M; i++) {
					for(int j = 0; j < N; j++)
						alignments_by_cell[i][j] = new BiList_Node_ThreatPos();
				}
				for(int d = 0; d < alignments_by_dir.length; d++) {
					AlignmentsList alignments_by_row	= alignments_by_dir[d];
					MovePair dir						= DIRECTIONS[d];
					for(int i = 0; i < alignments_by_row.size(); i++) {
						if(alignments_by_row.get(i) != null) {
							copyAlignmentInCells(alignments_by_row.getFirst(CellState.P1, i), CellState.P1, dir);
							copyAlignmentInCells(alignments_by_row.getFirst(CellState.P2, i), CellState.P2, dir);
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


}