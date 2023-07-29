package connectx.pndbfinal;

import connectx.CXCell;

public class tester {
	

	public static void main(String[] args) {

		int	M = 10,
			N = 10,
			X = 5;
		boolean first = true;
		

		BoardBit board = new BoardBit(M, N, X);

		board.mark(0, CellState.P2);
		board.mark(1, CellState.P1);
		board.mark(2, CellState.P2);
		board.mark(3, CellState.P1);

		DbSearch db = new DbSearch();
		db.init(M, N, X, first);
		
		CXCell res = db.selectColumn(board, null, 10000, CellState.P1);

		System.out.println(res);
	}

}
