package pndb.dnull;

import pndb.dnull.constants.CellState;
import pndb.dnull.structs.DbSearchResult;

public class testerDb {
	

	public static void main(String[] args) {

		BoardBit.M = 9;
		BoardBit.N = 9;
		BoardBit.X = 5;
		boolean first = true;
		

		DbSearch db = new DbSearch();
		db.init(BoardBit.M, BoardBit.N, BoardBit.X, first);

		BoardBit board = new BoardBit();


		String[] bb = {
			".........",
			".........",
			".........",
			"..x.o....",
			".xo.o....",
			".oxxx....",
			".oxox.o..",
			".oxxxoxo.",
			"oxxoxxooo"
		};

		for(int i = 0; i< BoardBit.M; i++) {
			for(int j = 0; j < BoardBit.N; j++) {
				if(bb[BoardBit.M-i-1].substring(j, j+1).equals("."))
					continue;
				else board.markCheck(j, (bb[BoardBit.M-i-1].substring(j, j+1).equals("x")) ? CellState.P1 : CellState.P2);
			}
		}
		

		
		DbSearchResult res = db.selectColumn(board, null, 10000, CellState.P1, Operators.MAX_TIER);

		System.out.println((res == null)? null : res.winning_col);
	}

}
