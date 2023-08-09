package pndb.betha.scomb;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

import pndb.alpha.BoardBit;
import pndb.alpha._BoardBitDb;
import pndb.alpha._Operators;
import pndb.alpha._Operators.AlignmentPattern;
import pndb.alpha.threats.AlignmentsList;
import pndb.alpha.threats.BiList_ThreatPos;
import pndb.alpha.threats.ThreatApplied;
import pndb.alpha.threats.ThreatCells;
import pndb.alpha.threats.ThreatCells.USE;
import pndb.alpha.threats.ThreatPosition;
import pndb.constants.Auxiliary;
import pndb.constants.CellState;
import pndb.constants.Constants.BoardsRelation;
import pndb.constants.MovePair;
import pndb.structures.BiList.BiNode;




public class BoardBitDb extends _BoardBitDb<BoardBitDb, BoardBit> {
	

	private static MovePair c3 = new MovePair();
	


	/**
	 * Complexity: O(3N)
	 * @param M
	 * @param N
	 * @param X
	 */
	public BoardBitDb(int M, int N, int X, _Operators operators) {
		super(M, N, X, operators);
	}

	/**
	 * Complexity: O(3N + N) = O(4N)
	 * @param B
	 */
	public BoardBitDb(BoardBit B, _Operators operators) {
		super(B, operators);
	}

	/**
	 * Complexity:
	 * 		if copy_threats: O(3N + 3(M+N) + B.markedThreats.length) = O(9N + B.markedThreats.length)
	 * 		else:			 O(3N + B.markedThreats.length) = O(3N + B.markedThreats.length)
	 * @param B
	 * @param copy_threats
	 */
	protected BoardBitDb(BoardBitDb B, boolean copy_threats, _Operators operators) {
		super(B, copy_threats, operators);
	}

	/**
	 * Complexity: O(relative constructor) = O()
	 */
	public BoardBitDb getCopy(boolean copy_threats) {
		return new BoardBitDb(this, copy_threats, OPERATORS);
	}

	
	//#region DB_SEARCH

		/**
		 * Complexity: (all assuming check_threats=true, otherwise don't consider that)
		 * 	-	case atk: O(16X * 4 + CheckAlignments ) = O(64X CheckAlignments)
		 * 	-	case def: O(64X * 4) = O(256X)
		 * 			( + CheckAlignments), performed in DbSearch.defensiveVisit
		 * 	-	case bth: O(16 * 4 + 4X + CheckAlignments ) = O(64 + 4X + CheckAlignments)
		 * 	note: using getCopy(false), no threat to remove.
		 * @param threat as defined in Operators
		 * @param atk attacker's move index in threat
		 * @param use as def in Operators
		 * @param threats wether to update alignments and threats for this board
		 * @return :	a new board resulting after developing this with such threat (dependency stage);
		 * 				the new board only has alignment involving the newly marked cells
		 */
		public BoardBitDb getDependant(ThreatCells threat, int atk, USE use, int max_tier, boolean check_threats) {
			
			BoardBitDb res = null;
			
			switch(use) {
				//used for...
				case ATK:
				/*
					res = getCopy(false);
					MovePair cell = threat.related[threat.nextAtk(atk)];
					res.mark(cell.i, cell.j, Player_byte[currentPlayer]);
					if(check_threats) res.checkAlignments(cell, max_tier, -1, "dep");
				*/
					break;
					//used for init defensive visit (marks defensive cells as own)
				case DEF:
					res = getCopy(true);
					//if there exist any defensive moves
					if(threat.related.length > 1)
					res.markMore(threat.getDefensive(atk), Player_byte[currentPlayer]);
					// add this threat and check_alignments are done in getDefensiveRoot
					break;
					//used for dependency stage
				case BTH:
					res = getCopy(false);
					res.markThreat(threat.related, atk);
					res.addThreat(threat, atk, Player_byte[currentPlayer]);
					if(check_threats) res.checkAlignments(threat.related[atk], Math.min(max_tier, 2), -1, "dep");
			}
			return res;
		}
		
		/**
		 * Only checks for alignments not included in the union of A's and B's alignments, i.e. those which involve at  least one cell only present in A and one only in B.
		 * Complexity: O(marked_threats.length + N**2 + 13N) + O(B.marked_threats.length * (4+4+4X+4 AVG_THREATS_PER_DIR_PER_LINE) + added_threats.length O(4 findAlignmentsInDir) )
		 *		= O(marked_threats.length + N**2 + 13N) + O(B.marked_threats.length * (8 + 4X + 4 avg_threats_per_dir_per_line) + added_threats.length O(4 findAlignmentsInDir) )
		 *		= O(marked_threats.length + N**2 + 4 B.marked_threats.length * (X + avg_threats_per_dir_per_line) + added_threats.length O(4 findAlignmentsInDir) )
		 *		worst case for findAlignmentsInDirection:
		 *			 O(marked_threats.length + N**2) + 4 B.marked_threats.length * (X + avg_threats_per_dir_per_line) + added_threats.length 288 X**2 )
		 */
		public BoardBitDb getCombined(BoardBitDb B, byte attacker, int max_tier) {

			BoardBitDb res = getCopy(true);
			LinkedList<MovePair> added_threat_attacks = new LinkedList<MovePair>();

			for(ThreatApplied athreat : B.markedThreats) {
				if(isUsefulThreat(athreat, attacker)) {
					//mark other board's threat on res
					for(int i = 0; i < athreat.threat.related.length; i++) {
						MovePair c = athreat.threat.related[i];

						if(cellFree(c.i, c.j)) {
							if(i == athreat.related_index) {
								res.mark(c, athreat.attacker);
								added_threat_attacks.add(c);
							}
							else res.mark(c, Auxiliary.opponent(athreat.attacker));
						}
					}
					//add threats
					res.addThreat(athreat);
				}
			}

			// calculate alignments
			for(MovePair m = added_threat_attacks.pop(); !added_threat_attacks.isEmpty(); m = added_threat_attacks.pop())
				findAlignments(m, attacker,  Math.min(max_tier, 2), this, B, true, 1, "combined_");

			return res;
		}

	
		//#region ALIGNMENTS
	
			/**
			 * Complexity: O(4 * AVG_THREATS_PER_DIR_PER_LINE)
			 * @param center
			 * @param player
			 */
			protected void removeAlignments(final MovePair center, byte player) {

				// debug
				//if(DEBUG_ON)
				//	System.out.println("\nremoveAlignments START:");
				// foreach direction
				for(int d = 0; d < DIR_ABS_N; d++) {
					BiList_ThreatPos alignments_in_line = alignments_by_dir[d].get(getIndex_for_alignmentsByDir(DIRECTIONS[d], center));

					if(alignments_in_line != null) {
						BiNode<ThreatPosition>	p = alignments_in_line.getFirst(player),
												p_next;
						
						while(p != null) {
							p_next = p.next;

							// debug
							if(DEBUG_PRINT) System.out.println("remove " + p.item);

							if(center.inBetween_included(p.item.start, p.item.end))
								removeAlignmentNode(alignments_in_line, p, player);

							p = p_next;
						}
					}
				}

			}

			private void _addValidAlignments() {

			}
			
			/**
			 * invariants:
			 * - c1 and c2 indicate the first and last mark of a possible alignment (otehrwise, the priority is to bring both on a player's cell);
			 * - lined = c2-c1 : length of the alignment (excluding outer free cells);
			 * - marks : number of marks in range c1-c2;
			 * - in = lined - marks : number of free cells in range c1-c2;
			 * - before, after: trailing and lookahead number of free cells, constantly kept updated not to go beyond max value;
			 * - c3 = c2 + after : lookahead;
			 * 
			 * note: the second extreme is always excluded from counting the alignment (c1, c2, after..).
			 * @param second
			 * @param dir
			 * @param player
			 * @param lined
			 * @param marks
			 * @param in
			 * @param before
			 * @param after
			 * @param only_valid
			 * @param max_tier
			 */
			private void _findAlignmentsInDirection(MovePair second, MovePair dir, byte player,
				int lined, int marks, int before, int after,
				boolean only_valid, int max_tier
			) {
				if(before > OPERATORS.MAX_OUT_ONE_SIDE) {
						_findAlignmentsInDirection(second, dir, player, lined, marks, before - 1, after, only_valid, max_tier);
				} else if(c1.getDistanceInDir(second, dir) > before) {
					return;
				} else if(lined > X) {
					c1.sum(dir);
					_findAlignmentsInDirection(second, dir, player, lined - 1,
					marks - 1, 0,	// c1 is player's cell, otherwise that would be the priority.
						0,				// lined only grows if c2 finds a player's cell, so after is already 0
						only_valid, max_tier);
				} else if( (!only_valid || free[c1.j] == c1.i		// make sure the free cell is the first free in the column
					|| dir.equals(DIRECTIONS[DIR_IDX_VERTICAL]))
					&& cellFree(c1.i, c1.j)
				) {
					if(dir.equals(DIRECTIONS[DIR_IDX_VERTICAL])) {
						// vertical: break
						return;
					}
					c1.sum(dir);
					if(lined > 0)
					// it means c2 is on a player's cell, and c1 was already advancing, and now is passing on an 'in' free cell
						_findAlignmentsInDirection(second, dir, player, lined - 1, marks, before + 1,
						0,				// if was already advancing, it was because c2 advanced too much, so c2 is on a player's cell
							only_valid, max_tier);
					else
						// still searching for the start of any alignment
						_findAlignmentsInDirection(second, dir, player, 0, 0, before + 1, 0, only_valid, max_tier);
				} else if( (only_valid && free[c1.j] < c1.i)	// if free is greater but it's a player's cell, no problem
					|| _cellState(c1.i, c1.j) != player
				) {
					// can only happen whlie was searching for the start of any alignment
					c1.sum(dir);
					_findAlignmentsInDirection(second, dir, player, 0, 0, 0, 0, only_valid, max_tier);
				} else {
					if(lined + after == 0)
						// c3 could be before c1
						c3.reset(c1);
						
					/* c1 is on a player's cell, now let's analyze .
					 * not to add useless threats (i.e. smaller ones when there are bigger ones available), we extend c2 as much as we can,
					 * and then we add them just before reducing 'lined' - and we (try to) do that each very time we need to do that (all cases).
					 */
					if(after + lined > X + OPERATORS.MAX_LINED) {
						// too much free space while looking for the next c2, need to increase c1.
						_addValidAlignments();
						c1.sum(dir);
						_findAlignmentsInDirection(second, dir, player, lined - 1, marks - 1, 0, after, only_valid, max_tier);
					} else if(cellFree(c3.i, c3.j)) {
						c3.sum(dir);
						_findAlignmentsInDirection(second, dir, player, lined, marks, before, after + 1, only_valid, max_tier);
					} else if(_cellState(c3.i, c3.j) == player) {
						c2.reset(c3);
						c3.sum(dir);
						_findAlignmentsInDirection(second, dir, player, lined + after + 1, marks + 1, before, 0, only_valid, max_tier);
					} else {
						_addValidAlignments();
						c1.reset(c3.sum(dir));
						_findAlignmentsInDirection(second, dir, player, 0, 0, 0, 0, only_valid, max_tier);
					}
				}
						
						
					for( _findOccurrenceUntil(c1, c1.reset(first), dir_neg, MAX, X + OPERATORS.MAX_FREE_EXTRA - 1, player, opponent, false, false, only_valid, dir_index)	// find furthest c1 back, from center
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
									c1.reset(c2);									// skip useless c1's
									c1_reset_to_c2 = true;
									break;
								}
							}
							else if	(cellState(c2) == player) marks++;	
							else {													// opponent: c1++
								c1.reset(c2);
								c1_reset_to_c2 = true;
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
										addAlignment(threat_pos, dir_index, player);

										// debug
										found++;
										if(DEBUG_ON) file.write("found threat: " + threat_start + "_( " + c1 + "->" + c2 + ") _" + threat_end + " : " + threat_pos + "before, after=" + before + " " + after + "\n");
										if(DEBUG_PRINT) System.out.println("found threat: " + threat_start + "_( " + c1 + "->" + c2 + ") _" + threat_end + " : " + threat_pos +  "before, after=" + before + " " + after + "\n");
									}
								}
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

					end_c1.reset(second).clamp_diag(MIN, MAX, dir, OPERATORS.MAX_FREE_EXTRA);
					end_c2.reset(second).clamp_diag(MIN, MAX, dir, X + OPERATORS.MAX_FREE_EXTRA);
					
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
						
					for( _findOccurrenceUntil(c1, c1.reset(first), dir_neg, MAX, X + OPERATORS.MAX_FREE_EXTRA - 1, player, opponent, false, false, only_valid, dir_index)	// find furthest c1 back, from center
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
									c1.reset(c2);									// skip useless c1's
									c1_reset_to_c2 = true;
									break;
								}
							}
							else if	(cellState(c2) == player) marks++;	
							else {													// opponent: c1++
								c1.reset(c2);
								c1_reset_to_c2 = true;
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
										addAlignment(threat_pos, dir_index, player);

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


		//#endregion ALIGNMENTS

		

		/**
		 * Check if a combination with node is valid, i.e. if they're not in conflict and both have a marked cell the other doesn't.
		 * Assumes both boards have the same `MY_PLAYER` (i.e. the same bit-byte association for players).
		 * @param B
		 * @param attacker
		 * @return
		 */
		@Override
		public BoardsRelation validCombinationWith(BoardBitDb B, byte attacker) {

			long flip = (attacker == MY_PLAYER) ? 0 : -1;
			boolean useful_own = false, useful_other = false;

			for(int i = 0; i < COL_SIZE(M); i++) {
				for(int j = 0; j < N; j++) {
					// check conflict
					if( (board_mask[j][i] & B.board_mask[j][i] & (board[j][i] ^ B.board[j][i])) != 0 )
						return BoardsRelation.CONFLICT;
					// check own board adds something for attaacker
					if( (~(board_mask[j][i] & B.board_mask[j][i]) & (board[j][i] ^ flip)) != 0 )
						useful_own = true;
					// check other board adds something for attaacker
					if( (~(board_mask[j][i] & B.board_mask[j][i]) & (B.board[j][i] ^ flip)) != 0 )
						useful_own = true;
					
					if(useful_own && useful_other)
						return BoardsRelation.USEFUL;
				}
			}
			return BoardsRelation.USELESS;

		}
	
	//#endregion DB_SEARCH

	
	//#region SET

		/**
		 * Helper, for adding an alignment to the structures.
		 */
		@Override
		protected void addAlignment(ThreatPosition alignment, int dir_index, byte player) {
			if(alignments_by_dir[dir_index] == null)
				alignments_by_dir[dir_index] = new AlignmentsList(alignments_by_dir_sizes[dir_index]);
			alignments_by_dir[dir_index].add(player, getIndex_for_alignmentsByDir(DIRECTIONS[dir_index], threat_start), alignment);
		}	
	
	//#endregion SET
	
	//#region INIT

		/**
		 * Complexity: O(1)
		 */
		@Override
		protected void initAlignmentStructures() {
			alignments_by_dir = new AlignmentsList[DIR_ABS_N];
		}

		//#region COPY

		/**
		 * Complexity: O(M + N + M+N-1 + M+N-1 + 4) = O(3(M+N))
		 * @param DB
		 */
		@Override
		protected void copyAlignmentStructures(BoardBitDb DB) {
			alignments_by_dir = new AlignmentsList[]{
				new AlignmentsList(DB.alignments_by_dir[0]),
				new AlignmentsList(DB.alignments_by_dir[1]),
				new AlignmentsList(DB.alignments_by_dir[2]),
				new AlignmentsList(DB.alignments_by_dir[3])
			};
		}

		//#endregion COPY
		
	//#endregion INIT
	
}