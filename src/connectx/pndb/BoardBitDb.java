package connectx.pndb;

import java.util.ArrayList;
import java.util.LinkedList;

import connectx.CXCell;
import connectx.CXCellState;
import connectx.pndb.BiList.BiNode;




public class BoardBitDb extends BoardBit implements IBoardDb {
	

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

	public final int M;		// rows
	public final int N;		// columns
	public final int K;		// Number of symbols to be aligned (horizontally, vertically, diagonally) for a win
	protected static final MovePair MIN = new MovePair(0, 0);
	protected final MovePair MAX;

	protected CXCell[] MC; 							// Marked Cells
	protected int MC_n;								// marked cells number
	protected LinkedList<AppliedThreat> markedThreats;

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
	protected AlignmentsList[] lines_per_dir;								//for each direction 0-3, contains the reference to the proper lines array(list)
	protected static final int[] lines_dirs = new int[]{2, 4, 3, 5};		//indexes in DIRECTIONS, with same order as lines_per_dir
	

	protected BiList_NodeOpPos[][] cells_lines;

	protected final CXCellState[] Player = {CXCellState.P1, CXCellState.P2};
	protected int currentPlayer;		// currentPlayer plays next move (= 0 or 1)
  



	BoardBitDb(int M, int N, int X) {
		super(M, N, X);
		
		MAX = new MovePair(M, N);
		hash = 0;
		initStructures();

		initLinesStructures();
		reset();
	}

	BoardBitDb(BoardBit B) {
		super(B.M, B.N, B.X);
		super.copy(B);

		MAX = new MovePair(M, N);
		hash = 0;
		initStructures();

		initLinesStructures();
		reset();
	}

	BoardBitDb(BoardBitDb B, boolean copy_threats) {
		super(B.M, B.N, B.X);
		super.copy(B);

		MAX = new MovePair(M, N);
		currentPlayer = B.currentPlayer;
		hash = B.hash;
		initStructures();
		
		if(copy_threats) copyLinesStructures(B);
		else initLinesStructures();
		copyArrays(B);
	}

	

	public void updateAlignments() {
		
	}



	//#region INIT

	private void initStructures() {
		board		= new long[N][COL_SIZE(M)];
		board_mask	= new long[N][COL_SIZE(M)];
		free		= new byte[N];
		MC			= new CXCell[M*N];
	}
	private void initLinesStructures() {
			lines_rows		= new AlignmentsList(M);
			lines_cols		= new AlignmentsList(N);
			lines_dright	= new AlignmentsList(M + N - 1);
			lines_dleft		= new AlignmentsList(M + N - 1);
			lines_per_dir	= new AlignmentsList[]{lines_rows, lines_cols, lines_dright, lines_dleft};
			cells_lines		= new BiList_NodeOpPos[M][N];
			for(int i = 0; i < M; i++) {
				for(int j = 0; j < N; j++)
					cells_lines[i][j] = new BiList_NodeOpPos();
			}
		}
		//#region COPY
			public void copyArrays(BoardBitDb AB) {
				copyBoard(AB);
				copyFreeCells(AB);
				copyMarkedCells(AB);
				markedThreats = new LinkedList<AppliedThreat>(AB.markedThreats);	//copy marked threats
			}
			public void reset() {
				currentPlayer = 0;
				initBoard();
				initFreeCells();
				initMarkedCells();
				markedThreats = new LinkedList<AppliedThreat>();
			}
			// Sets to free all board cells
			private void initBoard() {
				for(int j = 0; j < N; j++)
					for(int i = 0;i < COL_SIZE(M); i++) {
						board[i][j]			= 0;
						board_mask[i][j]	= 0;
					}
			}
			// Rebuilds the free cells set 
			private void initFreeCells() {
				free_n = M * N;
				for(int j = 0; j < N; j++) free[free_n] = 0;
			}
			// Resets the marked cells list
			private void initMarkedCells() {MC_n = 0;}

			private void copyBoard(BoardBitDb AB) {
				for(int i = 0; i < M; i++)
					for(int x = 0; x < N; x++) board[i][x] = AB.board[i][x];
			}
			private void copyFreeCells(BoardBitDb AB) {
				free_n = AB.free_n;
				for(int j = 0; j < COL_SIZE(M); j++) free[j] = AB.free[j];
			}
			private void copyMarkedCells(BoardBitDb AB) {
				MC_n = AB.MC_n;
				for(int i = 0; i < MC_n; i++) MC[i] = copyCell(AB.MC[i]);
			}
			// copies an MNKCell
			private CXCell copyCell(CXCell c) {
				return new CXCell(c.i, c.j, c.state);
			}
			private void copyLinesStructures(BoardBitDb DB) {
				lines_rows		= new AlignmentsList(DB.lines_rows);
				lines_cols		= new AlignmentsList(DB.lines_cols);
				lines_dright	= new AlignmentsList(DB.lines_dright);
				lines_dleft		= new AlignmentsList(DB.lines_dleft);
				lines_per_dir	= new AlignmentsList[]{lines_rows, lines_cols, lines_dright, lines_dleft};
				cells_lines		= new BiList_NodeOpPos[M][N];
				for(int i = 0; i < M; i++) {
					for(int j = 0; j < N; j++)
						cells_lines[i][j] = new BiList_NodeOpPos();
				}
				for(int d = 0; d < lines_per_dir.length; d++) {
					AlignmentsList line	= lines_per_dir[d];
					MovePair dir		= DIRECTIONS[lines_dirs[d]];
					for(int i = 0; i < line.size(); i++) {
						if(line.get(i) != null) {
							copyLineInCells(line.getFirst(CXCellState.P1, i), CXCellState.P1, dir);
							copyLineInCells(line.getFirst(CXCellState.P2, i), CXCellState.P2, dir);
						}
					}
				}
			}
			private void copyLineInCells(BiNode<OperatorPosition> line_node, CXCellState player, MovePair dir) {
				if(line_node != null) {
					copyLineInCells(line_node.next, player, dir);
					MovePair it = new MovePair(line_node.item.start), end = line_node.item.end;
					while(true) {
						cells_lines[it.i()][it.j()].add(player, line_node);
						if(!it.equals(end)) it.sum(dir);
						else break;
					}
				}
			}
		//#endregion COPY
	//#endregion INIT

}