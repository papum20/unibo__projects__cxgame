package pndb.betha;

import pndb.alpha.BoardBit;
import pndb.alpha._BoardBitDb;
import pndb.alpha._Operators;
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
				new AlignmentsList(DB.alignments_by_dir[0]),
				new AlignmentsList(DB.alignments_by_dir[1]),
				new AlignmentsList(DB.alignments_by_dir[2]),
				new AlignmentsList(DB.alignments_by_dir[3])
			};
		}

		//#endregion COPY
		
	//#endregion INIT
	
}