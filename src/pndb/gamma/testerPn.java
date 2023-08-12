package pndb.gamma;

import connectx.CXBoard;
import pndb.alpha.BoardBit;
import pndb.constants.CellState;



public class testerPn {
	

	public static void main(String[] args) {

		int	M = 9,
			N = 9,
			X = 5;
		boolean first = true;
		
		
		Player pn = new Player();
		pn.initPlayer(M, N, X, first, 10);

		BoardBit board = pn.board;
		
		//board.markCheck(2, CellState.P1);

		String[] bb = {
			".........",
			".........",
			".........",
			".........",
			".........",
			".........",
			".........",
			".........",
			"........."
		};



		for(int i = 0; i< M; i++) {
			for(int j = 0; j < N; j++) {
				if(bb[M-i-1].substring(j, j+1).equals("."))
					continue;
				else board.markCheck(j, (bb[M-i-1].substring(j, j+1).equals("x")) ? CellState.P1 : CellState.P2);
			}
		}
		board.print();

		
		
		CXBoard last_board = new CXBoard(M, N, X);
		last_board.markColumn(6);

		pn.current_player = CellState.P2;
		
		int res = pn.selectColumn(last_board);

		System.out.println(res);
	}

}
