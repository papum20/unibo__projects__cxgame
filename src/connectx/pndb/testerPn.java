package connectx.pndb;

import connectx.CXBoard;
import connectx.CXCell;

public class testerPn {
	

	public static void main(String[] args) {

		int	M = 9,
			N = 9,
			X = 5;
		boolean first = true;
		

		BoardBit board = new BoardBit(M, N, X);

		board.mark(0, CellState.P1);
		board.mark(0, CellState.P1);
		board.mark(1, CellState.P2);
		board.mark(1, CellState.P1);
		board.mark(2, CellState.P2);
		board.mark(2, CellState.P2);
		board.mark(2, CellState.P1);
		board.mark(2, CellState.P2);
		board.mark(3, CellState.P1);
		board.mark(3, CellState.P1);
		board.mark(3, CellState.P2);
		board.mark(4, CellState.P1);
		board.mark(4, CellState.P2);
		board.mark(5, CellState.P2);
		board.mark(5, CellState.P1);
		board.mark(8, CellState.P2);
		board.mark(8, CellState.P2);
		board.mark(8, CellState.P1);

		CXBoard last_board = new CXBoard(M, N, X);
		last_board.markColumn(1);

		PnSearch pn = new PnSearch();
		pn.initPlayer(M, N, X, first, 10);
		pn.board = board;
		pn.current_player = CellState.P2;
		
		int res = pn.selectColumn(last_board);

		System.out.println(res);
	}

}
