package connectx.pndb;

import connectx.CXBoard;



public class testerPn {
	

	public static void main(String[] args) {

		int	M = 9,
			N = 9,
			X = 5;
		boolean first = true;
		
		
		PnSearch pn = new PnSearch();
		pn.initPlayer(M, N, X, first, 10);

		BoardBit board = pn.board;
		
		board.mark(2, CellState.P2);
		board.mark(4, CellState.P1);
		board.mark(4, CellState.P1);
		board.mark(4, CellState.P1);
		board.mark(7, CellState.P2);
		board.mark(7, CellState.P2);
		
		
		CXBoard last_board = new CXBoard(M, N, X);
		last_board.markColumn(5);

		pn.current_player = CellState.P2;
		
		int res = pn.selectColumn(last_board);

		System.out.println(res);
	}

}
