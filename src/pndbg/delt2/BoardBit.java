package pndbg.delt2;

import java.util.ArrayList;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Math;

import connectx.CXCellState;
import connectx.CXGameState;
import pndbg.constants.Auxiliary;
import pndbg.constants.CellState;
import pndbg.constants.GameState;
import pndbg.constants.MovePair;
import pndbg.delt2.tt.TranspositionTable;




public class BoardBit {
	
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
	public long[][] board;
	public long[][] board_mask;
	public byte[] free;		// first free position for each column
	protected int free_n;		// number of free cells

	public byte game_state;	// note: could be put in DbNode.data?

	public static TranspositionTable TT;
	public long hash;



	/**
	 *  Complexity: O(3N) if M <= 64 else O(5N)
	 * @param M
	 * @param N
	 * @param X
	 */
	public BoardBit(int M, int N, int X) {
		this.M = (byte)M;
		this.N = (byte)N;
		this.X = (byte)X;

		createStructures();

		game_state = GameState.OPEN;
		hash = 0;
	}

	/**
	 * Copy all.
	 * Complexity: O(N * COL_SIZE(M))) = O(N) if M <= 64 else O(2N)
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

		hash = B.hash;
	}

	/**
	 * Complexity: O(1)
	 * @param col
	 * @param player
	 * @return GameState
	 */
	public void mark(int col, byte player) {
		hash = TT.getHash(hash, free[col], col, Auxiliary.getPlayerBit(player));

		board[col][free[col] / BITSTRING_LEN]		|= (player & 1) << (free[col] % BITSTRING_LEN);	// =1 for CellState.ME
		board_mask[col][free[col] / BITSTRING_LEN]	|= 1 << (free[col] % BITSTRING_LEN);
		free[col]++;
		free_n--;
	}
	/**
	 * Complexity: O(4X)
	 * @param i
	 * @param j
	 * @param player
	 * @return
	 */
	protected byte check(int i, int j, byte player) {
		if(isWinningMove(i, j)) game_state = cell2GameState(player);
		else if(free_n == 0) game_state = GameState.DRAW;
		else game_state = GameState.OPEN;
		
		return game_state;

	}
	/**
	 * Complexity: O(4X)
	 * @param col
	 * @param player
	 * @return GameState
	 */
	public byte markCheck(int col, byte player) {
		mark(col, player);
		return check(free[col] - 1, col, player);
	}

	/**
	 * Complexity: O(1)
	 */
	public void unmark(int col) {
		free[col]--;

		hash = TT.getHash(hash, free[col], col, _cellState(free[col], col));
		
		board[col][free[col] / BITSTRING_LEN]		&= ~(1 << (free[col] % BITSTRING_LEN));
		board_mask[col][free[col] / BITSTRING_LEN]	^= 1 << (free[col] % BITSTRING_LEN);
		free_n++;

		game_state = GameState.OPEN;
	}

	/**
	 * Complexity: O(4X)
	 * @param i
	 * @param j
	 * @return
	 */
	protected boolean isWinningMove(int i, int j) {
		long	mask_ij = (long)1 << (i % BITSTRING_LEN);
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
		for (k = i-1, mask = 1 << (k % BITSTRING_LEN);
			k >= 0
			&& (board[j][k / BITSTRING_LEN] & mask)			== s * mask
			&& (board_mask[j][k / BITSTRING_LEN] & mask)	== s_mask * mask
			; k--, mask = 1 << (k % BITSTRING_LEN)
		)
			n++;
		if (n >= X) return true;
			
		// Diagonal check
		n = 1;
		for (k = 1, mask = 1 << ((i-k) % BITSTRING_LEN); 
			i-k >= 0 && j-k >= 0 &&
			(board[j-k][(i-k) / BITSTRING_LEN] & mask)		== s * mask &&
			(board_mask[j-k][(i-k) / BITSTRING_LEN] & mask)	== s_mask * mask;
			k++, mask = 1 << ((i-k) % BITSTRING_LEN))
				n++; // backward check
		for (k = 1, mask = 1 << ((i+k) % BITSTRING_LEN); 
			i+k < M && j+k < N &&
			(board[j+k][(i+k) / BITSTRING_LEN] & mask)		== s * mask &&
			(board_mask[j+k][(i+k) / BITSTRING_LEN] & mask)	== s_mask * mask;
			k++, mask = 1 << ((i+k) % BITSTRING_LEN))
				n++; // forward check
		if (n >= X) return true;
		
		// Anti-diagonal check
		n = 1;
		for (k = 1, mask = 1 << ((i-k) % BITSTRING_LEN); 
			i-k >= 0 && j+k < N &&
			(board[j+k][(i-k) / BITSTRING_LEN] & mask)		== s * mask &&
			(board_mask[j+k][(i-k) / BITSTRING_LEN] & mask)	== s_mask * mask;
			k++, mask = 1 << ((i-k) % BITSTRING_LEN))
				n++; // backward check
		for (k = 1, mask = 1 << ((i+k) % BITSTRING_LEN); 
			i+k < M && j-k >= 0 &&
			(board[j-k][(i+k) / BITSTRING_LEN] & mask)		== s * mask &&
			(board_mask[j-k][(i+k) / BITSTRING_LEN] & mask)	== s_mask * mask;
			k++, mask = 1 << ((i+k) % BITSTRING_LEN))
				n++; // forward check
		if (n >= X) return true;
			
		return false;
	}


	//#region GET_SET

		/*
		 * Return board's value, i.e. 1 if occupied by first player.
		 * Complexity: O(1)
		 */
		public byte _cellState(int i, int j) {
			return (byte)(1 & (board[j][i / BITSTRING_LEN] >> (i % BITSTRING_LEN)));
		}
		/*
		 * Return board_mask's value, i.e. 1 if occupied by someone.
		 * Complexity: O(1)
		 */
		public byte _cellMaskState(int i, int j) {
			return (byte)(1 & (board_mask[j][i / BITSTRING_LEN] >> (i % BITSTRING_LEN)));
		}

		/**
		 * Complexity: O(1)
		 */
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
		/**
		 * Complexity: O(1)
		 */
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
		 * Complexity: O(1)
		 * @param c
		 * @return the cell's state, as CellState
		 */
		public byte cellState(MovePair c) {return cellState(c.i, c.j);}
		/**
		 * Complexity: O(1)
		 */
		public CXCellState cellStateCX(MovePair c) {return cellStateCX(c.i, c.j);}

		/**
		 * Complexity: O(1)
		 */
		public boolean cellFree(int i, int j) {return (1 & (board_mask[j][i / BITSTRING_LEN] >> (i % BITSTRING_LEN))) == 0;}
		
		/**
		 * Convert cell to GameState, assuming cell is occupied by someone.
		 * Complexity: O(1)
		 */
		public byte cell2GameState(int i, int j) {
			return (_cellState(i, j) == 1)? GameState.WINP1 : GameState.WINP2;
		}
		/**
		 * Complexity: O(1)
		 */
		public byte cell2GameState(byte cell_state) {
			return (cell_state == CellState.P1)? GameState.WINP1 : GameState.WINP2;
		}
		/**
		 * Complexity: O(1)
		 */
		public CXGameState cell2GameStateCX(int i, int j) {
			return (_cellState(i, j) == 1)? CXGameState.WINP1 : CXGameState.WINP2;
		}

		/**
		 * Complexity: O(1)
		 */
		public boolean freeCol(int j) {
			//return board_mask[j][COL_SIZE(M) - 1] != -1;
			return (free[j] != M);
		}

		/**
		 * Complexity: O(2N)
		 */
		public ArrayList<Integer> freeCols() {
			ArrayList<Integer> res = new ArrayList<Integer>(N);
			for(int j = 0; j < N; j++)
				if(board[j][COL_SIZE(M) - 1] != -1) res.add(j);
			return res;
		}

		/**
		 * Complexity: O(1)
		 */
		public byte gameState() {return game_state;}
		/**
		 * Complexity: O(1)
		 */
		public CXGameState gameStateCX() {
			switch(game_state) {
				case GameState.DRAW:	return CXGameState.DRAW;
				case GameState.WINP1:	return CXGameState.WINP1;
				case GameState.WINP2:	return CXGameState.WINP2;
				default:				return CXGameState.OPEN;
			}
		}
		/**
		 * Complexity: O(1)
		 */
		public void setGameState(CXGameState state) {game_state = Auxiliary.CX2gameState(state); }
		
	//#endregion GET_SET

	//#region MACROS

		/**
		 * How many bitstrings (long) a column of col_cells cells takes.
		 * Complexity: O(1)
		 * @param col_cells
		 * @return
		 */
		protected int COL_SIZE(int col_cells) {return (int)Math.ceil((float)col_cells / BITSTRING_LEN);}

	//#endregion MACROS

	//#region INIT

		/**
		 * Complexity: O(2 * (N * COL_SIZE(M)) + N + 1) = O(3N) if M <= 64 else O(5N)
		 */
		protected void createStructures() {
			board		= new long[N][COL_SIZE(M)];
			board_mask	= new long[N][COL_SIZE(M)];
			free		= new byte[N];
			free_n = M * N;
		}
		
	//#endregion INIT


	//#region DEBUG

		public void print() {
			System.out.println(printString(0));
		}

		/**
		 * don't print, return as a string.
		 */
		public String printString(int indentation) {
			//boolean[][] out = new boolean[M+1][N];
			//System.out.println(COL_SIZE(M));

			String lines = "";
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
				lines += line + "\n";
			}
			lines += "\n";
			return lines;
		}

		public void printFile(FileWriter file, int indentation) {
			try {				
				file.write(printString(indentation));
			} catch(IOException io) {}
		}
		
	//#endregion DEBUG


}