package pndb.delta;

import pndb.constants.CellState;

public class testerDb {
	

	public static void main(String[] args) {

		int	M = 9,
			N = 9,
			X = 5;
		boolean first = true;
		

		DbSearch db = new DbSearch();
		db.init(M, N, X, first);

		BoardBit board = new BoardBit(M, N, X);


		String[] bb = {
			".........",
			"xx...xx..",
			"xooxooo.o",
			"ooxooxxxo",
			"xoxoxooxo",
			"oxxoxoxxx",
			"oooxxoxox",
			"ooooxxxox",
			"oooxoxxxx"
			
		};

		for(int i = 0; i< M; i++) {
			for(int j = 0; j < N; j++) {
				if(bb[M-i-1].substring(j, j+1).equals("."))
					continue;
				else board.markCheck(j, (bb[M-i-1].substring(j, j+1).equals("x")) ? CellState.P1 : CellState.P2);
			}
		}
		

		
		DbSearchResult res = db.selectColumn(board, null, 10000, CellState.P1, Operators.MAX_TIER);

		System.out.println((res == null)? null : res.winning_col);
	}

}
