package connectx.pndb2;

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
		
		board.markCheck(2, CellState.P1);
		board.markCheck(2, CellState.P1);
		board.markCheck(2, CellState.P1);
		board.markCheck(2, CellState.P1);
		board.markCheck(2, CellState.P2);
		board.markCheck(3, CellState.P2);
		board.markCheck(3, CellState.P2);
		board.markCheck(3, CellState.P2);
		board.markCheck(3, CellState.P1);
		board.markCheck(4, CellState.P2);
		board.markCheck(4, CellState.P2);
		board.markCheck(4, CellState.P1);
		board.markCheck(5, CellState.P2);
		board.markCheck(5, CellState.P2);
		board.markCheck(5, CellState.P2);
		board.markCheck(5, CellState.P1);
		board.markCheck(6, CellState.P2);
		board.markCheck(6, CellState.P1);
		board.markCheck(6, CellState.P2);
		board.markCheck(6, CellState.P2);
		board.markCheck(6, CellState.P2);
		board.markCheck(7, CellState.P1);
		board.markCheck(7, CellState.P2);
		board.markCheck(7, CellState.P2);
		board.markCheck(7, CellState.P1);
		board.markCheck(7, CellState.P1);
		board.markCheck(8, CellState.P1);
		board.markCheck(8, CellState.P1);
		board.markCheck(8, CellState.P1);
		board.markCheck(8, CellState.P1);

		
		
		CXBoard last_board = new CXBoard(M, N, X);
		last_board.markColumn(8);

		pn.current_player = CellState.P2;
		
		int res = pn.selectColumn(last_board);

		System.out.println(res);
	}

}
