package pndbg.betha.scomb;

import pndbg.alpha.BoardBit;
import pndbg.alpha.Operators;
import pndbg.alpha._Operators;
import pndbg.constants.CellState;
import pndbg.nocel.nonmc.tryit.DbSearchResult;

public class testerDb {
	

	public static void main(String[] args) {

		int	M = 9,
			N = 9,
			X = 5;
		boolean first = true;
		

		_Operators OPERATORS = new Operators();
		DbSearch db = new DbSearch(OPERATORS);
		db.init(M, N, X, first);

		BoardBit board = new BoardBit(M, N, X);

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
			".........",
			".........",
			".........",
			".........",
			".........",
			"..o.o....",
			"oxo.oxx.o",
			"oxoxxoo.x",
			"xoooxxxoo"
		};




		for(int i = 0; i< M; i++) {
			for(int j = 0; j < N; j++) {
				if(bb[M-i-1].substring(j, j+1).equals("."))
					continue;
				else board.markCheck(j, (bb[M-i-1].substring(j, j+1).equals("x")) ? CellState.P1 : CellState.P2);
			}
		}
		board.print();

		
		DbSearchResult res = db.selectColumn(board, null, 10000, CellState.P2, OPERATORS.MAX_TIER);

		System.out.println((res == null)? null : res.winning_col);
	}

}
