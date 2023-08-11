package pndb.betha;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import pndb.alpha.BoardBit;
import pndb.alpha._BoardBitDb;
import pndb.alpha._Operators;
import pndb.alpha._Operators.AlignmentPattern;
import pndb.alpha._Operators.ThreatsByRank;
import pndb.alpha.threats.AlignmentsList;
import pndb.alpha.threats.BiList_ThreatPos;
import pndb.alpha.threats.ThreatApplied;
import pndb.alpha.threats.ThreatCells;
import pndb.alpha.threats.ThreatPosition;
import pndb.constants.Auxiliary;
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
		 * Only checks for alignments not included in the union of A's and B's alignments, i.e. those which involve at  least one cell only present in A and one only in B.
		 * Complexity: O(marked_threats.length + N**2 + 13N) + O(B.marked_threats.length * (4+4+16X*AVG_THREATS_PER_DIR_PER_LINE) )
		 *		= O(marked_threats.length + N**2 + 13N) + O(B.marked_threats.length * (8 + 16X * avg_threats_per_dir_per_line) )
		 *		= O(marked_threats.length + N**2) + O(B.marked_threats.length * (16X * avg_threats_per_dir_per_line) )
		 */
		public BoardBitDb getCombined(BoardBitDb B, byte attacker, int max_tier) {

			BoardBitDb res = getCopy(true);

			for(ThreatApplied athreat : B.markedThreats) {
				if(isUsefulThreat(athreat, attacker)) {
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
			res.addAllCombinedAlignments(B, attacker, Math.min(max_tier, 2));

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
					if(alignments_by_dir[d] != null) {
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

			}

			/**
			 * note: no max_tier, must check before calling.
			 * @param dir_index
			 * @param player
			 * @param lined
			 * @param marks
			 * @param before
			 * @param after
			*/
			private void _addValidAlignments(int dir_index, byte player, int lined, int marks, int before, int after) throws IOException {

				int tier = X - marks;

				// check alignments, foreach alignment of mark marks
				for(byte threat_code : OPERATORS.alignmentCodes(tier))
				{
					AlignmentPattern alignment = OPERATORS.alignmentPatterns(tier).get((int)threat_code);

					// debug
					if(DEBUG_ON) file.write("\t\t\t\t\tstart checking alignment = " + alignment + "\n");
					if(DEBUG_PRINT) System.out.println("\t\t\t\t\tstart checking alignment = " + alignment + "\n");

					//if (inner alignment conditions)
					if(alignment.isApplicableExactly(X, lined, marks, before, after)) {

						if(tier == 0) {
							game_state = Auxiliary.cellState2winState(player);
							return;
						}

						//add to structures
						threat_start.resetToVector(c1, DIRECTIONS[dir_index], -before);
						threat_end.resetToVector(c2, DIRECTIONS[dir_index], alignment.out - before);
						ThreatPosition threat_pos = new ThreatPosition(threat_start, threat_end, threat_code);
						addAlignment(threat_pos, dir_index, player);

						// debug
						found++;
						if(DEBUG_ON) file.write("found threat: " + threat_start + "_( " + c1 + "->" + c2 + ") _" + threat_end + " : " + threat_pos + "before, after=" + before + " " + after + "\n");
						if(DEBUG_PRINT) System.out.println("found threat: " + threat_start + "_( " + c1 + "->" + c2 + ") _" + threat_end + " : " + threat_pos +  "before, after=" + before + " " + after + "\n");
					}
				}
			}
			
			/**
			 * invariants:
			 * -	c1 and c2 indicate the first and last mark of a possible alignment (otehrwise, the priority is to bring both on a player's cell);
			 * -	lined = c2-c1 : length of the alignment (excluding outer free cells);
			 * -	marks : number of marks in range c1-c2;
			 * -	in = lined - marks : number of free cells in range c1-c2;
			 * -	before, after: trailing and lookahead number of free cells, constantly kept updated not to go beyond max value
			 * 		(before is initialized correctly as a parameter, while after is checked in a recursion condition);
			 * -	c3 = c2 + after : lookahead;
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
			 * @param checking_alignments (implementative) if true: need to check for alignments, and not increase c1 while before > 0
			 */
			private void _findAlignmentsInDirection(MovePair second, MovePair dir, int dir_index, byte player,
				int lined, int marks, int before, int after,
				boolean only_valid, int max_tier, boolean checking_alignments
			) throws IOException {
				if(checking_alignments && before > 0) {
					_addValidAlignments(dir_index, player, lined, marks, before, after);
					// also check for alignments with next before
					_findAlignmentsInDirection(second, dir, dir_index, player, lined, marks, before - 1, after, only_valid, max_tier, true);
				} else if(c1.getDistanceInDir(second, dir) > before || !c1.inBounds(MIN, MAX)) {
					return;
				} else if(lined > X) {
					if(before > 0 && X - marks <= max_tier) {	// also check for right tier
						_findAlignmentsInDirection(second, dir, dir_index, player, lined, marks, before, after, only_valid, max_tier, true);
					} else {
						if(X - marks <= max_tier)
							_addValidAlignments(dir_index, player, lined, marks, before, after);
						c1.sum(dir);
						_findAlignmentsInDirection(second, dir, dir_index, player, lined - 1,
							marks - 1, 0,	// c1 is player's cell, otherwise that would be the priority.
							0,				// lined only grows if c2 finds a player's cell, so after is already 0
							only_valid, max_tier, false);
					}
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
						_findAlignmentsInDirection(second, dir, dir_index, player, lined - 1, marks,
							Math.min(before + 1, OPERATORS.MAX_OUT_ONE_SIDE), after, only_valid, max_tier, false);
					else
						// still searching for the start of any alignment
						_findAlignmentsInDirection(second, dir, dir_index, player, 0, 0, Math.min(before + 1, OPERATORS.MAX_OUT_ONE_SIDE), 0, only_valid, max_tier, false);
				} else if( (only_valid && free[c1.j] < c1.i)	// if free is greater but it's a player's cell, no problem
					|| _cellState(c1.i, c1.j) != Auxiliary.getPlayerBit(player)
				) {
					// can only happen whlie was searching for the start of any alignment
					c1.sum(dir);
					_findAlignmentsInDirection(second, dir, dir_index, player, 0, 0, 0, 0, only_valid, max_tier, false);
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
						if(before > 0 && X - marks <= max_tier) {
							_findAlignmentsInDirection(second, dir, dir_index, player, lined, marks, before, after, only_valid, max_tier, true);
						} else {
							if(X - marks <= max_tier)
								_addValidAlignments(dir_index, player, lined, marks, before, after);
							c1.sum(dir);
							_findAlignmentsInDirection(second, dir, dir_index, player, lined - 1, marks - 1, 0, after, only_valid, max_tier, false);
						}
					} else if(!c3.inBounds(MIN, MAX)) {
						System.out.println(X - marks + " " + max_tier);
						if(X - marks > max_tier) {
							return;
						} else if(before > 0) {
							_findAlignmentsInDirection(second, dir, dir_index, player, lined, marks, before, after, only_valid, max_tier, true);
						} else {
							if(X - marks <= max_tier)
								_addValidAlignments(dir_index, player, lined, marks, before, after);
							c1.sum(dir);
							_findAlignmentsInDirection(second, dir, dir_index, player, lined - 1, marks - 1, 0, after, only_valid, max_tier, false);
						}
					}
					else if( (!only_valid || free[c3.j] == c3.i		// make sure the free cell is the first free in the column
						|| dir.equals(DIRECTIONS[DIR_IDX_VERTICAL]))
						&& cellFree(c3.i, c3.j)
					) {
						if(dir.equals(DIRECTIONS[DIR_IDX_VERTICAL])) {
							if(X - marks <= max_tier)
								// vertical: last check and break (note: before is 0)
								_addValidAlignments(dir_index, player, lined, marks, before, after + M - c3.i);
						} else {
							c3.sum(dir);
							_findAlignmentsInDirection(second, dir, dir_index, player, lined, marks, before, after + 1, only_valid, max_tier, false);
						}
					} else if( (only_valid && free[c3.j] < c3.i)	// if free is greater but it's a player's cell, no problem
						|| _cellState(c3.i, c3.j) != Auxiliary.getPlayerBit(player)
					) {
						if(before > 0 && X - marks <= max_tier) {
							_findAlignmentsInDirection(second, dir, dir_index, player, lined, marks, before, after, only_valid, max_tier, true);
						} else {
							if(X - marks <= max_tier)
								_addValidAlignments(dir_index, player, lined, marks, before, after);
							c1.reset(c3.sum(dir));
							_findAlignmentsInDirection(second, dir, dir_index, player, 0, 0, 0, 0, only_valid, max_tier, false);
						}
					} else {
						c2.reset(c3);
						c3.sum(dir);
						_findAlignmentsInDirection(second, dir, dir_index, player, lined + after + 1, marks + 1, before, 0, only_valid, max_tier, false);
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

				try {

					// debug
					String filename = "debug/db2/" + player + "_" + caller + count + "_" + (int)(Math.random() * 99999) + "_.txt";
					if(DEBUG_ON) {
						file = new FileWriter(filename);
						file.write(printString(0) + "\naddAlignments START, for player " + player + ", moves " + first + " " + second + " dir: " + DIRECTIONS[dir_index] + ", end_c1/c2:" + end_c1 + " " + end_c2 + ", onlyvalid:" + only_valid + ":\n");
					}
					if(DEBUG_PRINT) System.out.println(printString(0) + "\naddAlignments START, for player " + player + ", moves " + first + " " + second + " dir: " + DIRECTIONS[dir_index] + ", end_c1/c2:" + end_c1 + " " + end_c2 + ", onlyvalid:" + only_valid + ":\n");
					count++;
					found = 0;

					// swap first, second if not first->second in same direction as dir
					if(!DIRECTIONS[dir_index].equals( first.getDirection(second)) ) {
						MovePair tmp = first;
						first	= second;
						second	= tmp;
					}

					System.out.println(c1 + " "+first + second);
					// find furthest c1 back, from center
					_findOccurrenceUntil(c1, c1.reset(first), DIRECTIONS[dir_index + DIR_ABS_N],
						MAX, X + OPERATORS.MAX_OUT_ONE_SIDE - 1, player, Auxiliary.opponent(player), false, false, only_valid, dir_index);
					c2.reset(c1);
					c3.reset(c1);

					System.out.println(c1 + "dir " + dir_index + " "  + DIRECTIONS[dir_index]);
					
					_findAlignmentsInDirection(second,				// flip if direction inverted
						DIRECTIONS[dir_index], dir_index, player,
						0, 0, 0, 0,
						only_valid, max_tier, false
					);

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
					if(DEBUG_ON) try{file.write("\n\nERROR\n\n");file.close();} catch(IOException io) {}
					throw e;
				}
				
			}

			@Override
			/**
			 * Complexity: worst: O(3(M+N) * AVG_THREATS_PER_DIR_PER_LINE * AVG(O(Operators.applied)) ) = O(3X(M+N))
			 */
			public ThreatsByRank getApplicableOperators(byte attacker, int max_tier) {

				byte defender		= Auxiliary.opponent(attacker);
				ThreatsByRank res	= OPERATORS.new ThreatsByRank();

				for(AlignmentsList alignments_by_row : alignments_by_dir) {
					if( alignments_by_row != null) {
						for(BiList_ThreatPos alignments_in_line : alignments_by_row) {
							if(alignments_in_line != null) {
								
								BiNode<ThreatPosition> alignment = alignments_in_line.getFirst(attacker);
								if(alignment != null && OPERATORS.tier(alignment.item.type) <= max_tier) {
									do {
										ThreatCells cell_threat_operator = OPERATORS.applied(this, alignment.item, attacker, defender);
										
										if(cell_threat_operator != null) res.add(cell_threat_operator);
										alignment = alignment.next;

									} while(alignment != null);
								}
							}
						}
					}
				}

				return res;
			}

			/**
			 * Complexity: O(3(M+N) * AVG_THREATS_PER_DIR_PER_LINE ) = O(3(M+N))
			 */
			@Override
			public int[] getThreatCounts(byte player) {

				setPlayer(player);
				findAllAlignments(player, OPERATORS.MAX_TIER, false, "selCol_");
		
				int[] threats_by_col = new int[N];

				for(int d = 0; d < DIR_ABS_N; d++) {
					if(alignments_by_dir[d] != null) {
						for(BiList_ThreatPos alignments_in_line : alignments_by_dir[d]) {
							if(alignments_in_line != null) {
								
								BiNode<ThreatPosition> p = alignments_in_line.getFirst(player);
								while(p != null) {
									// if in same col
									if(p.item.start.j == p.item.end.j)
										threats_by_col[p.item.start.j] += (p.item.start.getDistanceAbs(p.item.end) + 1) * OPERATORS.indexInTier(p.item.type);
									
									else {
										for(int j = p.item.start.j; j <= p.item.end.j; j++)
											threats_by_col[j] += OPERATORS.indexInTier(p.item.type);
									}
			
									p = p.next;
								}
							}
						}
					}
				}
		
				return threats_by_col;
			}
			
		//#endregion ALIGNMENTS

		

		/**
		 * Check if a combination with node is valid, i.e. if they're not in conflict and both have a marked cell the other doesn't.
		 * Assumes both boards have the same `MY_PLAYER` (i.e. the same bit-byte association for players).
		 * Complexity: O(N COLSIZE(M)) = O(N)
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
				(DB.alignments_by_dir[0] != null) ? new AlignmentsList(DB.alignments_by_dir[0]) : null,
				(DB.alignments_by_dir[1] != null) ? new AlignmentsList(DB.alignments_by_dir[1]) : null,
				(DB.alignments_by_dir[2] != null) ? new AlignmentsList(DB.alignments_by_dir[2]) : null,
				(DB.alignments_by_dir[3] != null) ? new AlignmentsList(DB.alignments_by_dir[3]) : null
			};
		}

		//#endregion COPY
		
	//#endregion INIT


	//#region DEBUG

		@Override
		public String printAlignmentsString(int indentation) {

			String	indent = "",
					res = "";
			for(int i = 0; i < indentation; i++) indent += '\t';

			res += indent + "ALIGNMENTS:\n";
			res += indent + "by rows:\n";

			for(int d = 0; d < DIR_ABS_N; d++) {
				MovePair dir = DIRECTIONS[d % DIR_ABS_N];
				res += indent + "direction: " + dir + "\n";

				for(int player = 0; player < 2; player++) {
					res += indent + "player " + Player_byte[player] + ":\n\n";
					if(alignments_by_dir[d] != null) {
						for(int i = 0; i < alignments_by_dir[d].size(); i++) {

							if(alignments_by_dir[d].get(i) != null) {
								res += indent + "index " + i + "\n\n";
								for(BiNode<ThreatPosition> p = alignments_by_dir[d].getFirst(Player_byte[player], i);
									p != null; p = p.next
								) {
									res += indent + p.item + "\n\n";
								}
								
							}
						}
					}
					
				}
			}
			return res;
		}
	
	//#endregion DEBUG
	
}