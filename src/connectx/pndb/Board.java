package connectx.pndb;

import java.util.ArrayList;




public class Board {
	
	//#region CONSTANTS
	public static final int COL_NULL = -1;

	//#endregion CONSTANTS
	
	public final byte M;		// number of rows
	public final byte N;		// columns
	public final byte X;		// marks to align

	private final byte[][] board;
	private final byte[] free;		// first free position for each column
	private int free_n;				// number of free cells



	Board(int M, int N, int X) {
		this.M = (byte)M;
		this.N = (byte)N;
		this.X = (byte)X;

		board = new byte[M][N];
		free = new byte[N];
		free_n = M * N;
	}


	/**
	 * 
	 * @param col
	 * @param player
	 * @return GameState
	 */
	public byte mark(int col, byte player) {
		board[free[col]][col] = player;
		free[col]++;
		free_n--;
		if(isWinningMove(free[col] - 1, col)) return player;
		else if(free_n == 0) return GameState.DRAW;
		else return GameState.OPEN;
	}

	/**
	 * 
	 * @param col
	 */
	public void unmark(int col) {
		free[col]--;
		board[free[col]][col] = CellState.FREE;
		free_n++;
	}

	/**
	 * 
	 * @param i
	 * @param j
	 * @return
	 */
	private boolean isWinningMove(int i, int j) {
		byte s = board[i][j];
		int n;

		// Horizontal check
		n = 1;
		for (int k = 1; j-k >= 0 && board[i][j-k] == s; k++) n++; // backward check
		for (int k = 1; j+k <  N && board[i][j+k] == s; k++) n++; // forward check
		if (n >= X) return true;

		// Vertical check
		n = 1;
		for (int k = 1; i+k <  M && board[i+k][j] == s; k++) n++;
		if (n >= X) return true;

		// Diagonal check
		n = 1;
		for (int k = 1; i-k >= 0 && j-k >= 0 && board[i-k][j-k] == s; k++) n++; // backward check
		for (int k = 1; i+k <  M && j+k <  N && board[i+k][j+k] == s; k++) n++; // forward check
		if (n >= X) return true;

		// Anti-diagonal check
		n = 1;
		for (int k = 1; i-k >= 0 && j+k <  N && board[i-k][j+k] == s; k++) n++; // backward check
		for (int k = 1; i+k <  M && j-k >= 0 && board[i+k][j-k] == s; k++) n++; // forward check
		if (n >= X) return true;

		return false;
	}


	//#region GET
		/**
		 * 
		 */
		public ArrayList<Integer> freeCols() {
			ArrayList<Integer> res = new ArrayList<Integer>(N);
			for(int i = 0; i < N; i++)
				if(board[M - 1][i] == CellState.FREE) res.add(i);
			return res;
		}

	//#endregion GET


	//#region DEBUG

		void print() {
			//boolean[][] out = new boolean[M+1][N];
			for(int i = 0; i < M; i++) {
				for(int j = 1; j <= N; j++) {
					char t;
					switch(board[i][N-j]) {
						case CellState.ME:
							t = 'x';
						case CellState.YOU:
							t = 'o';
						default:
							t = '.';
					}
					System.out.println(t);
				}
				System.out.println("\n");
			}
		}
		
	//#endregion DEBUG


}