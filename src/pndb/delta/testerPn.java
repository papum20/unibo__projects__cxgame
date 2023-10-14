package pndb.delta;

import connectx.CXBoard;
import pndb.delta.constants.CellState;



public class testerPn {
	

	public static void main(String[] args) {

		int	M = 9,
			N = 9,
			X = 5;
		boolean first = true;
		
		
		PnSearch pn = new PnSearch();
		pn.initPlayer(M, N, X, first, 10);

		BoardBit board = pn.board;
		

		String[] bb = {
			
			/* selected 1, which loses in 1 */
			"...o.oo..",
			"..xx.xx..",
			"..ox.xo..",
			"o.oxoxo..",
			"o.oxooo..",
			"x.xoxxx..",
			"o.oxoxx..",
			"o.oxoxxxo",
			"oxoxxxxoo"
			
		};

		for(int i = 0; i< M; i++) {
			for(int j = 0; j < N; j++) {
				if(bb[M-i-1].substring(j, j+1).equals("."))
					continue;
				else board.markCheck(j, (bb[M-i-1].substring(j, j+1).equals("x")) ? CellState.P1 : CellState.P2);
			}
		}
		
		
		CXBoard last_board = new CXBoard(M, N, X);
		//last_board.markColumn(1);
		last_board.markColumn(0);

		// set the player to do next move(set in last_board)
		pn.board.player = CellState.P2;
		
		int res = pn.selectColumn(last_board);

		System.out.println(res);
	}

}
