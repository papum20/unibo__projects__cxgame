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

		//board.markCheck(0, CellState.P2);
		/*
		String[] bb = {
		"x..oxx...",
		"x..ooo...",
		"x..xoo...",
		"o.xooo..x",
		"o.xoxx.ox",
		"xooxxo.xx",
		"oxxoxxoxo",
		"ooooxxxxo",
		"oooxoxoxx"
		};
		*/

		String[] bb = {
		"oo.x....o",
		"xxxo....o",
		"xooo.xx.x",
		"xoxo.oo.o",
		"xooo.xo.o",
		"ooxxoox.x",
		"oxxxxoxxx",
		"oxxxoxxoo",
		"oxoxxxxoo"
		};



		for(int i = 0; i< M; i++) {
			for(int j = 0; j < N; j++) {
				if(bb[M-i-1].substring(j, j+1).equals("."))
					continue;
				else board.markCheck(j, (bb[M-i-1].substring(j, j+1).equals("x")) ? CellState.P1 : CellState.P2);
			}
		}
		board.print();

		board.markCheck(8, CellState.P1);

		
		DbSearchResult res = db.selectColumn(board, null, 10000, CellState.P1);

		System.out.println((res == null)? null : res.winning_col);
	}

}
