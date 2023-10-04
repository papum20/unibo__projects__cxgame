package pndb.delta;

import connectx.CXBoard;
import pndb.delta.constants.CellState;



public class testerPn {
	

	public static void main(String[] args) {

		int	M = 18,
			N = 20,
			X = 7;
		boolean first = true;
		
		
		PnSearch pn = new PnSearch();
		pn.initPlayer(M, N, X, first, 10);

		BoardBit board = pn.board;
		

		String[] bb = {
			"....................",
			"....................",
			"....................",
			"....................",
			"....................",
			"....................",
			"....................",
			"....................",
			"....................",
			"....................",
			"....................",
			"...............o....",
			"x..............x....",
			"o..............o....",
			"o..............o....",
			"o.x............o....",
			"o.x..x....x....o.x..",
			"oxxoxxxxo.o..x.o.x.."
			
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
		last_board.markColumn(15);

		// set the player to do next move(set in last_board)
		pn.current_player = CellState.P2;
		
		int res = pn.selectColumn(last_board);

		System.out.println(res);
	}

}
