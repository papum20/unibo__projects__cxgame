package connectx.pndb;

import java.util.LinkedList;

import connectx.CXCell;
import connectx.CXCellState;
import connectx.pndb.BiList.BiNode;
import connectx.pndb.Operators.ThreatCells;
import connectx.pndb.Operators.USE;




public class BoardBitDb extends BoardBit {
	

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

	protected CXCell[] MC; 							// Marked Cells
	protected int MC_n;								// marked cells number
	protected LinkedList<ThreatApplied> markedThreats;

	protected static TranspositionTable TT;
	protected long hash;

	//AUXILIARY STRUCTURES (BOARD AND ARRAYS) FOR COUNTING ALIGNMENTS
	protected AlignmentsList lines_rows;
	protected AlignmentsList lines_cols;
	protected AlignmentsList lines_dright;		//diagonals from top-left to bottom-right
	protected AlignmentsList lines_dleft;		//diagonals from top-right to bottom-left
	/*
	 * horizontal:	dimension=M,		indexed: by row
	 * vertical:	dimension=N,		indexed: by col
	 * dright:		dimension=M+N-1,	indexed: by start of diagonal on the top row, i.e. from -M+1 to N-1
	 * dleft:		dimension=M+N-1,	indexed: by start of diagonal on the top row, i.e. from 0 to N+M-1
	 */
	protected AlignmentsList[] alignments_by_direction;									//for each direction 0-3, contains the reference to the proper lines array(list)
	protected static final int[] alignments_direction_indexes = new int[]{2, 4, 3, 5};	//indexes in DIRECTIONS, with same order as lines_per_dir
	

	protected BiList_Node_ThreatPos[][] alignments_by_cell;

	protected final CXCellState[] Player	= {CXCellState.P1, CXCellState.P2};
	protected final byte[] Player_byte 		= {CellState.ME, CellState.YOU};
	protected final int[] Player_bit 		= {1, 0};
	protected int currentPlayer;		// currentPlayer plays next move (= 0 or 1)
  



	BoardBitDb(int M, int N, int X) {
		super(M, N, X);
		
		MAX = new MovePair(M, N);
		hash = 0;
		initStructures();

		initAlignmentStructures();
		reset();
	}

	BoardBitDb(BoardBit B) {
		super(B.M, B.N, B.X);
		super.copy(B);

		MAX = new MovePair(M, N);
		hash = 0;
		initStructures();

		initAlignmentStructures();
		reset();
	}

	BoardBitDb(BoardBitDb B, boolean copy_threats) {
		super(B.M, B.N, B.X);
		super.copy(B);

		MAX = new MovePair(M, N);
		currentPlayer = B.currentPlayer;
		hash = B.hash;
		initStructures();

		if(copy_threats) copyAlignmentStructures(B);
		else initAlignmentStructures();
		copyArrays(B);
	}

	//#region BOARD
		private void mark(int i, int j, byte state) {
			mark(j, state);
			addMC(i, j, cellStateCX(i, j));
			hash = TT.getHash(hash, i, j, state);
		}
		//public void markCell(MovePair cell) {markCell(cell.i(), cell.j(), Player[currentPlayer]);}
		public void mark(MovePair cell, CXCellState state) {mark(cell.i, cell.j, Auxiliary.CX2cellState(state));}
		public void mark(MovePair cell, byte player) {mark(cell.i, cell.j, player);}
		public void markMore(MovePair[] cells, CXCellState player) {
			for(MovePair c : cells) mark(c.i, c.j, Auxiliary.CX2cellState(player));
		}
		public void markThreat(MovePair[] related, int atk_index) {
			for(int i = 0; i < related.length; i++) {
				byte state = Player_byte[(i == atk_index) ? currentPlayer : (1 - currentPlayer)];
				mark(related[i].i, related[i].j, state);
			}
		}

		/**
		 * Remove and re-calculate alignments for a cell.
		 * @param cell
		 * @param player
		 */
		public void updateAlignments(MovePair cell, CXCellState player) {
			
			// remove alignments for both players involving this cell
			removeAlignments(cell, Auxiliary.opponent(player));
			removeAlignments(cell, player);

			// add alignments for player
			findAlignments(cell, cell, player, Operators.TIER_MAX, null, null, -1);

			//update gameState
			if(free_n == 0 && game_state == GameState.OPEN) game_state = GameState.DRAW;
			//currentPlayer = (currentPlayer + 1) % 2;
		}


		private void checkAlignments(MovePair cell, int max_tier, int dir_excluded) {
			if(isWinningMove(cell.i, cell.j))
				game_state = cell2GameState(cell.i, cell.j);
			else {
				findAlignments(cell, cell, cellStateCX(cell), max_tier, null, null, dir_excluded);
				if(free_n == 0 && game_state == GameState.OPEN) game_state = GameState.DRAW;
			}
		}
		private void checkAlignments(MovePair[] cells, int max_tier) {
			if(cells.length == 1) {
				checkAlignments(cells[0], max_tier, -1);
			} else {
				MovePair dir = cells[0].getDirection(new MovePair(cells[1]));
				int dir_index = dirsIndexes(dir);

				for(int i = 0; i < cells.length && game_state == GameState.OPEN; i++)
					checkAlignments(cells[i], max_tier, dir_index);

				if(game_state == GameState.OPEN) {
					for(int i = 1; i < cells.length; i++)
						findAlignmentsInDirection(cells[i], cells[i-1], cellStateCX(cells[i]), dir_index, max_tier, null, null);
				}
				//update gameState
				if(free_n == 0 && game_state == GameState.OPEN) game_state = GameState.DRAW;
			}
		}

	//#endregion BOARD


	//#region DB_SEARCH

		/**
		 * 
		 * @param threat : as defined in Operators
		 * @param atk : attacker's move index in threat
		 * @param use : as def in Operators
		 * @param threats : wether to update alignments and threats for this board
		 * @return :	a new board resulting after developing this with such threat (dependency stage);
		 * 				the new board only has alignment involving the newly marked cells
		 */
		public BoardBitDb getDependant(ThreatCells threat, int atk, USE use, int max_tier, boolean check_threats) {
			
			BoardBitDb res = new BoardBitDb(this, false);
			switch(use) {
				//used for...
				case ATK:
					MovePair cell = threat.related[threat.nextAtk(atk)];
					res.mark(cell.j, Player_byte[currentPlayer]);
					if(check_threats) res.checkAlignments(cell, max_tier, -1);
					break;
				//used for init defensive visit (marks defensive cells as own)
				case DEF:
					res.addThreat(threat, atk, Auxiliary.opponent(Player[currentPlayer]));
					//if there exist any defensive moves
					if(threat.related.length > 1) {
						res.markMore(threat.getDefensive(atk), Player[currentPlayer]);
						if(check_threats) res.checkAlignments(threat.getDefensive(atk), max_tier);
					}
					break;
				//used for dependency stage
				case BTH:
					res.markThreat(threat.related, atk);
					res.addThreat(threat, atk, Player[currentPlayer]);
					if(check_threats) res.checkAlignments(threat.related, max_tier);
			}
			return res;
		}
		//only checks for alignments not included in the union of A's and B's alignments, i.e. those which involve at  least one cell only present in A and one only in B
		public BoardBitDb getCombined(BoardBitDb B, CXCellState attacker, int max_tier) {

			BoardBitDb res = new BoardBitDb(this, false);

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
		
	//#endregion DB_SEARCH


	//#region AUXILIARY
		private void addMC(int y, int x, CXCellState player) {MC[MC_n++] = new CXCell(y, x, player);}
		public void addThreat(ThreatCells threat, int atk, CXCellState attacker) {
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

			private void removeAlignments(final MovePair center, CXCellState player) {

				// debug
				System.out.println("\nremoveAlignments START:");

				int MAX_ALIGNMENT = X + Operators.MAX_FREE_EXTRA_TOT;

				//foreach alignment that was stored in (y,x)
				BiNode<BiNode<ThreatPosition>> alignements_in_cell = alignments_by_cell[center.i][center.j].getFirst(player);
				if(alignements_in_cell != null)
				{
					do {
						MovePair	start	= alignements_in_cell.item.item.start,
									end		= alignements_in_cell.item.item.end,
									dir		= start.getDirection(end);

						// DEBUG
						System.out.println("\t\trm: " + alignements_in_cell.item.item);
						System.out.println("\t\t" + alignmentsByDirection_index(dir, center));

						//delete for line
						alignments_by_direction[dirsIndexes(dir)].remove(player, alignmentsByDirection_index(dir, center), alignements_in_cell.item);

						//delete for this cell
						BiNode<BiNode<ThreatPosition>> tmp = alignements_in_cell;
						alignements_in_cell = alignements_in_cell.next;
						alignments_by_cell[center.i][center.j].remove(player, tmp);

					} while(alignements_in_cell != null);
					
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
				System.out.println("removeAlignments END\n");
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
			 */
			private void findAlignmentsInDirection(final MovePair first, final MovePair second, final CXCellState player, int dir_index, int max_tier, BoardBitDb check1, BoardBitDb check2) {

				System.out.println(first + " " + second + " " + player + " " + dir_index);

				// debug
				boolean debug = false;
				if(debug) System.out.println("\naddAlignments START:");

				CXCellState opponent				= Auxiliary.opponent(player);
				MovePair dir						= DIRECTIONS[alignments_direction_indexes[dir_index]];
				int alignments_by_direction_index	= alignmentsByDirection_index(dir, first);							//if horizontal: row index, otherwise (all other cases) col index
				MovePair negdir						= dir.getNegative();
				int MIN_MARKS						= X - Operators.MARK_DIFF_MIN;
				MovePair end_c1, end_c2, c1, c2;
				//center = starting cell, end_c* = last to check for c*, c1,c2 = iterators (first and last in line to check)
				//c1 goes from center-MAX_LEN to center, c2 from c1 to center+MAX_LEN

				// make such that c1 is before c2
				if(dir.equals( first.getDirection(second)) ) {
					c1 = new MovePair(first);
					c2 = new MovePair(second);
				} else {
					c1 = new MovePair(second);
					c2 = new MovePair(first);
				}
				end_c1 = new MovePair(c2);
				end_c2 = new MovePair(c2);
				end_c1.clamp_diag(MIN, MAX, dir.getProduct(Operators.MAX_FREE_EXTRA - 1) );
				end_c2.clamp_diag(MIN, MAX, dir.getProduct(X + Operators.MAX_FREE_EXTRA - 1) );
				int	lined	= 0,	// alignment length, i.e. max distance
					marks	= 0,	// marks in alignment
					in		= 0,	// inside alignment
					before	= 0,	// 
					after	= 0;	// 
				
				// find furthest c1, from center
				int distance = 0;
				MovePair c_tmp = first.getSum(negdir);
				while(distance < X + Operators.MAX_FREE_EXTRA - 1 && c_tmp.inBounds(MIN, MAX) && cellStateCX(c_tmp) != opponent) {
					if(cellStateCX(c_tmp) == player) c1.reset(c_tmp);
					c_tmp.sum(negdir);
					distance++;
				}
				c2 = new MovePair(c1);

				// debug
				if(debug) System.out.println("\t\t\tdir: " + dir);

				boolean	checked_all = false,
						found1 = false, found2 = false,		// checks if found in check1, check2
						to_check = (check1 != null && check2 != null);
				while(!checked_all) {

					// debug
					if(debug) System.out.println("\t\t\t" + c1 + "->" + c2 + " : " + lined + ", " + marks + ", " + in);

					if(cellFree(c1.i, c1.j)) {

						// debug
						if(debug) System.out.println("\t\t\t\tc1!=player: " + c1 + "->" + c2 + " : " + lined + ", " + marks + ", " + in);

						// find start of an alignment, i.e. c1 contains player
						while(cellStateCX(c1) != player && !c1.equals(end_c1))
							c1.sum(dir);

						if(c1.equals(end_c1) && cellFree(c1.i, c1.j)) checked_all = true;
						else c2.reset(c1);
					}

					if(!checked_all) {

						//while (c2 == empty && !(c2 reached end_c2) ) line++, in++, c2++;		//impossible at first iteration, when c2=c1, because of the lines above

						while(cellFree(c2.i, c2.j) && !c2.equals(end_c2)) {
							//doesn't update line,in when c2==end_c2; however not needed, since in that case it would not check for alignments
							lined++;
							in++;
							c2.sum(dir);
						}

						// debug
						if(debug) System.out.println("\t\t\t\tc2 empty: " + c1 + "->" + c2 + " : " + lined + ", " + marks + ", " + in);

						//if ( !(line exceeded MAX) && c2 == player): line++, mark++; check alignment;

						if(lined <= X && cellStateCX(c2) == player) {
							lined++;
							marks++;

							if(to_check) {
								if		(!check1.cellFree(c2.i, c2.j) && check2.cellFree(c2.i, c2.j))	found1 = true;
								else if	(!check2.cellFree(c2.i, c2.j) && check1.cellFree(c2.i, c2.j))	found2 = true;
							}

							// debug
							if(debug) System.out.println("\t\t\t\tc2 player: " + c1 + "->" + c2 + " : " + lined + ", " + marks + ", " + in);

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
										if(debug) System.out.println("\t\t\t\t\tal = " + alignment);

										//if (inner alignment conditions)
										if(lined <= X - alignment.line && in == alignment.in) {

											//assuming that win is K marks aligned, without free cells, checks wether the game ended
											if(tier == 0) {
												game_state = cell2GameState(first.i, first.j);
												return;
											}

											//check outer alignment conditions
											before = countMarks( c1.getSum(dir.getNegative()), dir.getNegative(), alignment.out - alignment.mnout, CXCellState.FREE);
											if(before >= alignment.mnout)
												after = countMarks(c2.getSum(dir), dir, alignment.out - before, CXCellState.FREE);

											// debug
											if(debug) System.out.println("\t\t\t\t\tbefore, after = " + before + "," + after);

											//if (outer conditions)
											MovePair	threat_start	= c1.getSum(dir.getProduct(-before)),
														threat_end		= c2.getSum(dir.getProduct(after));

											for( ;
												before >= alignment.mnout && after >= alignment.mnout && before + after >= alignment.out	// alignment conditions
												&& threat_end.inBounds(MIN, MAX) && !cellFree(threat_end.i, threat_end.j)					// in bounds and player's cells
												; after++, before--, threat_start.sum(dir), threat_end.sum(dir)
												) {
												if(first.inBetween_included(threat_start, threat_end)) {
													ThreatPosition f = new ThreatPosition(threat_start, threat_end, threat_code);

													// debug
													if(debug) System.out.println(threat_start + "_( " + c1 + "->" + c2 + ") _" + threat_end + " : " + f);

													//add to arrays
													BiNode<ThreatPosition> node = alignments_by_direction[dir_index].add(player, alignments_by_direction_index, f);				//add to array for alignments in row/col/diag

													// debug
													if(debug) System.out.println("line sizes: " + dir_index + ", " + alignments_by_direction_index + " : " +  alignments_by_direction[dir_index].get(alignments_by_direction_index).isEmpty(player));

													//add reference for all in the middle
													for(MovePair c_it = new MovePair(threat_start); !c_it.equals(threat_end); c_it.sum(dir))
														alignments_by_cell[c_it.i][c_it.j].add(player, node);		//add to cell's alignments

													// debug
													//if(debug) System.out.println("cell empty: " + c_it + " : " + cells_lines[c_it.i()][c_it.j()].isEmpty(player));

												}
											}
										}

									}
								}
							}
						}	//end if (c2==player)

						//increment c1/c2
						if(c2.equals(end_c2) || lined >= X || cellStateCX(c2) == opponent) {
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
				if(debug) System.out.println("addAlignments END;\n");
			}

			/**
			 * Find alignments for a cell in all directions.
			 */
			private void findAlignments(final MovePair first, final MovePair second, final CXCellState player, int max_tier, BoardBitDb check1, BoardBitDb check2, int dir_excluded) {
				for(int d = 0; d < alignments_direction_indexes.length; d++) {
					if(d != dir_excluded)
						findAlignmentsInDirection(first, second, player, d, max_tier, check1, check2);
				}
			}

			/**
			 * Find all alignments for a player.
			 * @param player
			 * @param max_tier
			 */
			public void findAllAlignments(CXCellState player, int max_tier) {
			for(int d = 0; d < alignments_direction_indexes.length; d++) {
				MovePair start	= iterateLineDirs(null, d);
				MovePair end	= new MovePair(start);
				end.clamp_diag(MIN, MAX, DIRECTIONS[alignments_direction_indexes[d]]);
				findAlignmentsInDirection(start, end,  player, d, max_tier, null, null);
			}
		}

			private void addAllCombinedAlignments(BoardBitDb B, CXCellState player, int max_tier) {

				for(int alignments_by_direction_index = 0; alignments_by_direction_index < alignments_by_direction.length; alignments_by_direction_index++) {
					MovePair start = iterateLineDirs(null, alignments_by_direction_index);
					for(int i = 0; i < alignments_by_direction[alignments_by_direction_index].size();
						i++, start = iterateLineDirs(start, alignments_by_direction_index))
					{
						findAlignmentsInDirection(start, start, player, alignments_by_direction_index, max_tier, this, B);
					}
				}
			}


			private int countMarks(MovePair start, MovePair incr, int max, CXCellState mark) {
				int count = 0;
				while(count < max && start.inBounds(MIN, MAX) && cellStateCX(start) == mark) {
					count++;
					start.sum(incr);
				}
				return count;
			}

			/**
			 * Remove all alignments in alignments_by_cell[from] involving involved.
			 */
			private void removeAlignmentsByCell_InvolvingCell(MovePair from, MovePair involved, CXCellState player) {

				MovePair dir_involved = from.getDirection(involved);
				
				BiNode<BiNode<ThreatPosition>> node_alignment, node_tmp;
				BiList_Node_ThreatPos alignments_in_cell = alignments_by_cell[from.i][from.j];
				node_alignment = alignments_in_cell.getFirst(player);

				while(node_alignment != null) {
					
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
			private int alignmentsByDirection_index(MovePair dir, MovePair position) {
				if(dir.i == 0)				return position.i;				//horizontal
				else if(dir.j == 0)			return position.j;				//vertical
				else if(dir.i == dir.j)		return dir.j - dir.i + M - 1;	//dright
				else return dir.i + dir.j;									//dleft
			}

			private MovePair iterateLineDirs(MovePair start, int lines_dirs_index) {
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
			public boolean hasAlignments(CXCellState player) {

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
			private boolean isUsefulThreat(ThreatApplied athreat, CXCellState attacker) {

				CXCellState state, state_athreat;
				boolean useful = false;
				
				for(int i = 0; i < athreat.threat.related.length; i++) {
					state			= cellStateCX(athreat.threat.related[i]);
					state_athreat	= (i == athreat.related_index) ? athreat.attacker : Auxiliary.opponent(athreat.attacker);
					if(state == CXCellState.FREE) {						//compatible
						if(state_athreat == attacker) useful = true;	//useful
					} else if(state != state_athreat) return false;		//not compatible
				}

				return useful;
			}
		//#endregion ALIGNMENTS
		
	//#endregion AUXILIARY


	//#region GET
	
		public int getCurrentPlayer() {return currentPlayer;}
	
		public void setPlayer(byte player) {currentPlayer = (player == this.Player_byte[0]) ? 0 : 1;}

		public CXCell getMarkedCell(int i) {return MC[i];}
		public LinkedList<ThreatApplied> getMarkedThreats() {return markedThreats;}
		
	//#endregion GET

	//#region INIT

		private void initStructures() {
			board		= new long[N][COL_SIZE(M)];
			board_mask	= new long[N][COL_SIZE(M)];
			free		= new byte[N];
			MC			= new CXCell[M*N];
		}
		private void initAlignmentStructures() {
			lines_rows		= new AlignmentsList(M);
			lines_cols		= new AlignmentsList(N);
			lines_dright	= new AlignmentsList(M + N - 1);
			lines_dleft		= new AlignmentsList(M + N - 1);
			alignments_by_direction	= new AlignmentsList[]{lines_rows, lines_cols, lines_dright, lines_dleft};
			alignments_by_cell		= new BiList_Node_ThreatPos[M][N];
			for(int i = 0; i < M; i++) {
				for(int j = 0; j < N; j++)
					alignments_by_cell[i][j] = new BiList_Node_ThreatPos();
			}
		}
		//#region COPY
			public void copyArrays(BoardBitDb B) {
				copyBoard(B);
				/*
				copyFreeCells(AB);
				copyMarkedCells(AB);
				markedThreats = new LinkedList<ThreatApplied>(AB.markedThreats);	//copy marked threats
				*/
			}
			public void reset() {
				currentPlayer = 0;
				initBoard();
				initFreeCells();
				initMarkedCells();
				markedThreats = new LinkedList<ThreatApplied>();
			}
			// Sets to free all board cells
			private void initBoard() {
				for(int j = 0; j < N; j++)
					for(int i = 0; i < COL_SIZE(M); i++) {
						board[j][i]			= 0;
						board_mask[j][i]	= 0;
					}
			}
			// Rebuilds the free cells set 
			private void initFreeCells() {
				free_n = M * N;
				for(int j = 0; j < N; j++) free[j] = 0;
			}
			// Resets the marked cells list
			private void initMarkedCells() {MC_n = 0;}

			private void copyBoard(BoardBitDb B) {
				for(int j = 0; j < N; j++) {
					for(int i = 0; i < COL_SIZE(M); i++) {
						board[j][i]			= B.board[j][i];
						board_mask[j][i]	= B.board_mask[j][i];
					}
				}
			}

			private void copyFreeCells(BoardBitDb AB) {
				free_n = AB.free_n;
				for(int j = 0; j < N; j++) free[j] = AB.free[j];
			}
			private void copyMarkedCells(BoardBitDb AB) {
				MC_n = AB.MC_n;
				for(int i = 0; i < MC_n; i++) MC[i] = copyCell(AB.MC[i]);
			}

			// copies an MNKCell
			private CXCell copyCell(CXCell c) {
				return new CXCell(c.i, c.j, c.state);
			}
			private void copyAlignmentStructures(BoardBitDb DB) {
				lines_rows		= new AlignmentsList(DB.lines_rows);
				lines_cols		= new AlignmentsList(DB.lines_cols);
				lines_dright	= new AlignmentsList(DB.lines_dright);
				lines_dleft		= new AlignmentsList(DB.lines_dleft);
				alignments_by_direction	= new AlignmentsList[]{lines_rows, lines_cols, lines_dright, lines_dleft};
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
							copyAlignmentInCells(alignment.getFirst(CXCellState.P1, i), CXCellState.P1, dir);
							copyAlignmentInCells(alignment.getFirst(CXCellState.P2, i), CXCellState.P2, dir);
						}
					}
				}
			}
			private void copyAlignmentInCells(BiNode<ThreatPosition> alignment_node, CXCellState player, MovePair dir) {

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