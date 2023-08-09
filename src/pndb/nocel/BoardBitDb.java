package pndb.nocel;

import connectx.CXCell;
import pndb.alpha.BoardBit;
import pndb.alpha._BoardBitDb;
import pndb.alpha._Operators;
import pndb.constants.Auxiliary;
import pndb.constants.MovePair;




public class BoardBitDb extends _BoardBitDb<BoardBitDb, BoardBit> {
	

	public CXCell[] MC; 							// Marked Cells
	private int MC_n;								// marked cells number


	BoardBitDb(int M, int N, int X, _Operators operators) {
		super(M, N, X, operators);
	}

	BoardBitDb(BoardBit B, _Operators operators) {
		super(B, operators);
		copyMCfromBoard(B);
	}
	
	BoardBitDb(BoardBitDb B, boolean copy_threats, _Operators operators) {
		super(B, copy_threats, operators);
		copyMC(B);
	}

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

		//#endregion COPY
	//#endregion INIT

}