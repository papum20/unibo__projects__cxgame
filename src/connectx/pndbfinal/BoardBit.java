package connectx.pndbfinal;

import java.util.ArrayList;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Math;

import connectx.CXCell;
import connectx.CXCellState;
import connectx.CXGameState;




public class BoardBit implements IBoard<BoardBit> {
	
	//#region CONSTANTS
	public static final int COL_NULL = -1;

	protected static final int BITSTRING_LEN = 64;
	//#endregion CONSTANTS
	
	public final byte M;		// number of rows
	public final byte N;		// columns
	public final byte X;		// marks to align

	// board is an array of columns, each represented by one or more bitstrings.
	// board =1 for the first player's cells; board_mask =1 for any player's cell.
	// the least significative bit refers to the bottom of a column.
	protected long[][] board;
	protected long[][] board_mask;
	protected byte[] free;		// first free position for each column
	protected int free_n;				// number of free cells

	protected byte game_state;	// could be put in DbNode.data?



	BoardBit(int M, int N, int X) {
		this.M = (byte)M;
		this.N = (byte)N;
		this.X = (byte)X;

		createStructures();

		game_state = GameState.OPEN;
	}


	/**
	 * copy all
	 * @param B to copy
	 */
	public void copy(BoardBit B) {

		// copy all
		for(int j = 0; j < N; j++) {
			for(int i = 0; i < COL_SIZE(M); i++) {
				board[j][i]			= B.board[j][i];
				board_mask[j][i]	= B.board_mask[j][i];
			}
			free[j] = B.free[j];
		}

		free_n = B.free_n;
	}

	/**
	 * 
	 * @param col
	 * @param player
	 * @return GameState
	 */
	public byte mark(int col, byte player) {
		board[col][free[col] / BITSTRING_LEN]		^= (player & 1) << (free[col] % BITSTRING_LEN);	// =1 for CellState.ME
		board_mask[col][free[col] / BITSTRING_LEN]	|= 1 << (free[col] % BITSTRING_LEN);
		free[col]++;
		free_n--;

		if(isWinningMove(free[col] - 1, col)) game_state = cell2GameState(player);
		else if(free_n == 0) game_state = GameState.DRAW;
		else game_state = GameState.OPEN;
		
		return game_state;
	}

	/**
	 * 
	 * @param col
	 */
	public void unmark(int col) {
		free[col]--;
		board[col][free[col] / BITSTRING_LEN]		&= -1 ^ (1 << (free[col] % BITSTRING_LEN));
		board_mask[col][free[col] / BITSTRING_LEN]	^= 1 << (free[col] % BITSTRING_LEN);
		free_n++;

		game_state = GameState.OPEN;
	}

	/**
	 * 
	 * @param i
	 * @param j
	 * @return
	 */
	protected boolean isWinningMove(int i, int j) {
		long	mask_ij = 1 << (i % BITSTRING_LEN);
		long	s		= (board[j][i / BITSTRING_LEN] & mask_ij)		>> (i % BITSTRING_LEN),
				s_mask	= (board_mask[j][i / BITSTRING_LEN] & mask_ij)	>> (i % BITSTRING_LEN);
		long mask = mask_ij;
		int n;
		int k;

		// Horizontal check
		n = 1;
		for (k = j-1;
			k >= 0 &&
			(board[k][i / BITSTRING_LEN] & mask_ij)			== s * mask_ij &&
			(board_mask[k][i / BITSTRING_LEN] & mask_ij)	== s_mask * mask_ij;
			k--) n++; // backward check
		for (k = j+1;
			k < N &&
			(board[k][i / BITSTRING_LEN] & mask_ij)			== s * mask_ij &&
			(board_mask[k][i / BITSTRING_LEN] & mask_ij)	== s_mask * mask_ij;
			k++) n++; // forward check
		if (n >= X) return true;

		// Vertical check
		n = 1;
		for (k = i+1, mask = 1 << (k % BITSTRING_LEN);
			k >= 0 && 
			(board[j][k / BITSTRING_LEN] & mask)		== s * mask &&
			(board_mask[j][k / BITSTRING_LEN] & mask)	== s_mask * mask
			; k--, mask = 1 << (k % BITSTRING_LEN))
			n++;
			if (n >= X) return true;
			
			// Diagonal check
			n = 1;
			for (k = 1, mask = 1 << ((i-k) % BITSTRING_LEN); 
				i-k >= 0 && j-k >= 0 &&
				(board[j-k][(i-k) / BITSTRING_LEN] & mask)		== s * mask &&
				(board_mask[j-k][(i-k) / BITSTRING_LEN] & mask)	== s_mask * mask;
				k++, mask = 1 << (k % BITSTRING_LEN))
					n++; // backward check
			for (k = 1, mask = 1 << ((i+k) % BITSTRING_LEN); 
				i+k < M && j+k < N &&
				(board[j+k][(i+k) / BITSTRING_LEN] & mask)		== s * mask &&
				(board_mask[j+k][(i+k) / BITSTRING_LEN] & mask)	== s_mask * mask;
				k++, mask = 1 << (k % BITSTRING_LEN))
					n++; // forward check
			if (n >= X) return true;
			
			// Anti-diagonal check
			n = 1;
			for (k = 1, mask = 1 << ((i-k) % BITSTRING_LEN); 
				i-k >= 0 && j+k < N &&
				(board[j+k][(i-k) / BITSTRING_LEN] & mask)		== s * mask &&
				(board_mask[j+k][(i-k) / BITSTRING_LEN] & mask)	== s_mask * mask;
				k++, mask = 1 << (k % BITSTRING_LEN))
					n++; // backward check
			for (k = 1, mask = 1 << ((i+k) % BITSTRING_LEN); 
				i+k < M && j-k >= 0 &&
				(board[j-k][(i+k) / BITSTRING_LEN] & mask)		== s * mask &&
				(board_mask[j-k][(i+k) / BITSTRING_LEN] & mask)	== s_mask * mask;
				k++, mask = 1 << (k % BITSTRING_LEN))
					n++; // forward check
			if (n >= X) return true;
			
		return false;
	}


	//#region GET_SET

		/*
		 * return board's value, i.e. 1 if occupied by first player.
		 */
		public byte _cellState(int i, int j) {
			return (byte)(1 & (board[j][i / BITSTRING_LEN] >> (i % BITSTRING_LEN)));
		}
		/*
		 * return board_mask's value, i.e. 1 if occupied by someone.
		 */
		public byte _cellMaskState(int i, int j) {
			return (byte)(1 & (board_mask[j][i / BITSTRING_LEN] >> (i % BITSTRING_LEN)));
		}

		public byte cellState(int i, int j) {
			switch(_cellState(i, j)) {
				case 1:
					return CellState.P1;
				default:
					switch(_cellMaskState(i, j)) {
						case 1:
							return CellState.P2;
						default:
							return CellState.FREE;
					}
			}
		}
		public CXCellState cellStateCX(int i, int j) {
			switch(_cellState(i, j)) {
				case 1:
					return CXCellState.P1;
				default:
					switch(_cellMaskState(i, j)) {
						case 1:
							return CXCellState.P2;
						default:
							return CXCellState.FREE;
					}
			}
		}
		/**
		 * 
		 * @param c
		 * @return the cell's state, as CellState
		 */
		public byte cellState(MovePair c) {return cellState(c.i, c.j);}
		public CXCellState cellStateCX(MovePair c) {return cellStateCX(c.i, c.j);}
		public boolean cellFree(int i, int j) {/*;System.out.println(i+ " "+j+" "+board_mask[j][i/BITSTRING_LEN]);*/return (1 & (board_mask[j][i / BITSTRING_LEN] >> (i % BITSTRING_LEN))) == 0;}

		/*
		 * convert cell to GameState, assuming cell is occupied by someone.
		 */
		public byte cell2GameState(int i, int j) {
			return (_cellState(i, j) == 1)? GameState.P1 : GameState.P2;
		}
		public byte cell2GameState(byte cell_state) {
			return (cell_state == CellState.P1)? GameState.P1 : GameState.P2;
		}
		public CXGameState cell2GameStateCX(int i, int j) {
			return (_cellState(i, j) == 1)? CXGameState.WINP1 : CXGameState.WINP2;
		}

		/**
		 * 
		 */
		public ArrayList<Integer> freeCols() {
			ArrayList<Integer> res = new ArrayList<Integer>(N);
			for(int j = 0; j < N; j++)
				if(board[j][COL_SIZE(M) - 1] != -1) res.add(j);
			return res;
		}

		public byte gameState() {return game_state;}
		public CXGameState gameStateCX() {
			switch(game_state) {
				case GameState.DRAW:	return CXGameState.DRAW;
				case GameState.P1:		return CXGameState.WINP1;
				case GameState.P2:		return CXGameState.WINP2;
				default:				return CXGameState.OPEN;
			}
		}
		public void setGameState(CXGameState state) {game_state = Auxiliary.CX2gameState(state); }

	//#endregion GET_SET

	//#region MACROS

		/**
		 * how many bitstrings (long) a column of col_cells cells takes.
		 * @param col_cells
		 * @return
		 */
		protected int COL_SIZE(int col_cells) {return (int)Math.ceil((float)col_cells / BITSTRING_LEN);}

	//#endregion MACROS

	//#region INIT

		protected void createStructures() {
			board		= new long[N][COL_SIZE(M)];
			board_mask	= new long[N][COL_SIZE(M)];
			free		= new byte[N];
			free_n = M * N;
		}
	
	//#endregion INIT


	//#region DEBUG

		void print() {
			//boolean[][] out = new boolean[M+1][N];
			//System.out.println(COL_SIZE(M));

			for(int i = M - 1; i >= 0; i--) {
				String line = "";
				for(int j = 0; j < N; j++) {

					//System.out.println(i + " " + j + " " + board[j][i / BITSTRING_LEN] + " " + (board[j][i / BITSTRING_LEN] >> (i % BITSTRING_LEN)) );
					
					if(_cellState(i, j) == 0) {
						if(_cellMaskState(i, j) == 1)
							line += 'o';
						else
							line += '.';
					} else {
						line += 'x';
					}
				}
				System.out.println(line);
			}
			System.out.println("\n");
		}

		void printFile(FileWriter file, int indentation) {
			//boolean[][] out = new boolean[M+1][N];
			//System.out.println(COL_SIZE(M));

			try {				
				for(int i = M - 1; i >= 0; i--) {
					String line = "";
					for(int k = 0; k < indentation; k++) line += '\t';
					for(int j = 0; j < N; j++) {

						//System.out.println(i + " " + j + " " + board[j][i / BITSTRING_LEN] + " " + (board[j][i / BITSTRING_LEN] >> (i % BITSTRING_LEN)) );
						
						if(_cellState(i, j) == 0) {
							if(_cellMaskState(i, j) == 1)
								line += 'o';
							else
								line += '.';
						} else {
							line += 'x';
						}
					}
					line += "\n";
					file.write(line);
				}
				file.write("\n");
			} catch(IOException io) {}
		}
		
	//#endregion DEBUG


}