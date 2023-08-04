package pndb.nocel;

import pndb.alpha.BoardBit;
import pndb.alpha.DbSearchResult;
import pndb.constants.CellState;

public class testerDb {
	

	public static void main(String[] args) {

		int	M = 9,
			N = 9,
			X = 5;
		boolean first = true;
		

		DbSearch db = new DbSearch();
		db.init(M, N, X, first);

		BoardBit board = db.board;

		board.markCheck(0, CellState.P2);
		board.markCheck(0, CellState.P2);
		board.markCheck(0, CellState.P2);
		board.markCheck(0, CellState.P1);
		board.markCheck(0, CellState.P2);
		board.markCheck(0, CellState.P2);
		board.markCheck(0, CellState.P1);
		board.markCheck(0, CellState.P2);
		board.markCheck(0, CellState.P1);
		board.markCheck(2, CellState.P1);
		board.markCheck(2, CellState.P1);
		board.markCheck(2, CellState.P1);
		board.markCheck(2, CellState.P1);
		board.markCheck(2, CellState.P2);
		board.markCheck(2, CellState.P2);
		board.markCheck(2, CellState.P2);
		board.markCheck(2, CellState.P2);
		board.markCheck(2, CellState.P1);
		board.markCheck(3, CellState.P2);
		board.markCheck(3, CellState.P2);
		board.markCheck(3, CellState.P1);
		board.markCheck(3, CellState.P2);
		board.markCheck(3, CellState.P1);
		board.markCheck(3, CellState.P2);
		board.markCheck(4, CellState.P2);
		board.markCheck(4, CellState.P1);
		board.markCheck(4, CellState.P2);
		board.markCheck(4, CellState.P1);
		board.markCheck(4, CellState.P1);
		board.markCheck(4, CellState.P1);
		board.markCheck(5, CellState.P1);
		board.markCheck(5, CellState.P2);
		board.markCheck(5, CellState.P1);
		board.markCheck(5, CellState.P1);
		board.markCheck(5, CellState.P2);
		board.markCheck(5, CellState.P2);
		board.markCheck(6, CellState.P1);
		board.markCheck(6, CellState.P1);
		board.markCheck(6, CellState.P1);
		board.markCheck(6, CellState.P1);
		board.markCheck(6, CellState.P2);
		board.markCheck(6, CellState.P2);
		board.markCheck(7, CellState.P2);
		board.markCheck(7, CellState.P2);
		board.markCheck(7, CellState.P1);
		board.markCheck(7, CellState.P1);
		board.markCheck(7, CellState.P1);
		board.markCheck(8, CellState.P2);
		board.markCheck(8, CellState.P1);
		board.markCheck(8, CellState.P2);
		board.markCheck(8, CellState.P2);

		
		DbSearchResult res = db.selectColumn(board, null, 10000, CellState.P1);

		System.out.println((res == null)? null : res.winning_col);
	}

}
