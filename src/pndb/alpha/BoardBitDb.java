package pndb.alpha;

import connectx.CXCell;
import pndb.alpha.threats.AlignmentsList;
import pndb.alpha.threats.BiList_Node_ThreatPos;
import pndb.alpha.threats.ThreatPosition;
import pndb.constants.Auxiliary;
import pndb.constants.CellState;
import pndb.constants.MovePair;
import pndb.structures.BiList.BiNode;




public class BoardBitDb extends _BoardBitDb<BoardBitDb, BoardBit> {
	

	public CXCell[] MC; 							// Marked Cells
	private int MC_n;								// marked cells number

	protected BiList_Node_ThreatPos[][] alignments_by_cell;




	BoardBitDb(int M, int N, int X) {
		super(M, N, X);
	}

	BoardBitDb(BoardBit B) {
		super(B);
		copyMCfromBoard(B);
	}
	
	BoardBitDb(BoardBitDb B, boolean copy_threats) {
		super(B, copy_threats);
		copyMC(B);
	}

	public BoardBitDb getCopy(boolean copy_threats) {
		return new BoardBitDb(this, copy_threats);
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

					int MAX_ALIGNMENT = X + Operators.MAX_FREE_EXTRA_TOT;
						
					do {
						MovePair	start	= alignments_in_cell.item.item.start,
									end		= alignments_in_cell.item.item.end,
									dir		= start.getDirection(end);

						// DEBUG
						//if(DEBUG_ON) {
						if(DEBUG_PRINT) {
							System.out.println("\t\trm: " + alignments_in_cell.item.item);
							System.out.println("\t\t" + getIndex_for_alignmentsByDirection(dir, center));
						}

						//delete for line
						alignments_by_direction[dirsIndexes(dir)].remove(player, getIndex_for_alignmentsByDirection(dir, center), alignments_in_cell.item);

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

			@Override
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

			private void copyMC(BoardBitDb B) {
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

			@Override
			protected void copyAlignmentStructures(BoardBitDb DB) {
				super.copyAlignmentStructures(DB);

				alignments_by_cell = new BiList_Node_ThreatPos[M][N];
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


}