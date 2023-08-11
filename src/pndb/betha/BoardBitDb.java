package pndb.betha;

import pndb.alpha.BoardBit;
import pndb.alpha._BoardBitDb;
import pndb.alpha._Operators;
import pndb.alpha._Operators.ThreatsByRank;
import pndb.alpha.threats.AlignmentsList;
import pndb.alpha.threats.BiList_ThreatPos;
import pndb.alpha.threats.ThreatApplied;
import pndb.alpha.threats.ThreatCells;
import pndb.alpha.threats.ThreatCells.USE;
import pndb.alpha.threats.ThreatPosition;
import pndb.constants.Auxiliary;
import pndb.constants.Constants.BoardsRelation;
import pndb.constants.MovePair;
import pndb.structures.BiList.BiNode;




public class BoardBitDb extends _BoardBitDb<BoardBitDb, BoardBit> {
	


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
				findAllAlignments(player, OPERATORS.TIER_MAX, false, "selCol_");
		
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