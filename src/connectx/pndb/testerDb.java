package connectx.pndb;


public class testerDb {
	

	public static void main(String[] args) {

		int	M = 9,
			N = 9,
			X = 5;
		boolean first = true;
		

		DbSearch db = new DbSearch();
		db.init(M, N, X, first);

		BoardBit board = db.board;

		board.mark(0, CellState.P2);
		board.mark(0, CellState.P2);
		board.mark(0, CellState.P2);
		board.mark(0, CellState.P1);
		board.mark(0, CellState.P2);
		board.mark(0, CellState.P2);
		board.mark(0, CellState.P1);
		board.mark(0, CellState.P2);
		board.mark(0, CellState.P1);
		board.mark(2, CellState.P1);
		board.mark(2, CellState.P1);
		board.mark(2, CellState.P1);
		board.mark(2, CellState.P1);
		board.mark(2, CellState.P2);
		board.mark(2, CellState.P2);
		board.mark(2, CellState.P2);
		board.mark(2, CellState.P2);
		board.mark(2, CellState.P1);
		board.mark(3, CellState.P2);
		board.mark(3, CellState.P2);
		board.mark(3, CellState.P1);
		board.mark(3, CellState.P2);
		board.mark(3, CellState.P1);
		board.mark(3, CellState.P2);
		board.mark(4, CellState.P2);
		board.mark(4, CellState.P1);
		board.mark(4, CellState.P2);
		board.mark(4, CellState.P1);
		board.mark(4, CellState.P1);
		board.mark(4, CellState.P1);
		board.mark(5, CellState.P1);
		board.mark(5, CellState.P2);
		board.mark(5, CellState.P1);
		board.mark(5, CellState.P1);
		board.mark(5, CellState.P2);
		board.mark(5, CellState.P2);
		board.mark(6, CellState.P1);
		board.mark(6, CellState.P1);
		board.mark(6, CellState.P1);
		board.mark(6, CellState.P1);
		board.mark(6, CellState.P2);
		board.mark(6, CellState.P2);
		board.mark(7, CellState.P2);
		board.mark(7, CellState.P2);
		board.mark(7, CellState.P1);
		board.mark(7, CellState.P1);
		board.mark(7, CellState.P1);
		board.mark(8, CellState.P2);
		board.mark(8, CellState.P1);
		board.mark(8, CellState.P2);
		board.mark(8, CellState.P2);

		
		DbSearchResult res = db.selectColumn(board, null, 10000, CellState.P1);

		System.out.println((res == null)? null : res.winning_col);
	}

}
