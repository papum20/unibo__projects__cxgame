package connectx.pndb;

import java.util.ArrayList;

import connectx.CXBoard;
import connectx.CXCell;

public class tester {
	

	public static void main(String[] args) {

		int	M = 9,
			N = 9,
			X = 5;
		boolean first = true;
		

		BoardBit board = new BoardBit(M, N, X);

		board.mark(1, CellState.P1);
		board.mark(1, CellState.P1);
		board.mark(1, CellState.P1);
		board.mark(1, CellState.P1);
		board.mark(1, CellState.P2);
		board.mark(1, CellState.P2);
		board.mark(1, CellState.P1);
		board.mark(2, CellState.P1);
		board.mark(2, CellState.P1);
		board.mark(2, CellState.P2);
		board.mark(3, CellState.P2);
		board.mark(3, CellState.P2);
		board.mark(3, CellState.P1);
		board.mark(4, CellState.P1);
		board.mark(4, CellState.P2);
		board.mark(4, CellState.P1);
		board.mark(4, CellState.P1);
		board.mark(5, CellState.P1);
		board.mark(5, CellState.P2);
		board.mark(5, CellState.P1);
		board.mark(5, CellState.P1);
		board.mark(5, CellState.P2);
		board.mark(7, CellState.P2);
		board.mark(7, CellState.P1);
		board.mark(7, CellState.P2);
		board.mark(8, CellState.P2);
		board.mark(8, CellState.P2);
		board.mark(8, CellState.P2);
		board.mark(8, CellState.P1);

		DbSearch db = new DbSearch();
		db.init(M, N, X, first);
		
		DbSearchResult res = db.selectColumn(board, null, 10000, CellState.P1);

		System.out.println((!res.won)? null : res.moves_ordered.get(0));
	}

}
