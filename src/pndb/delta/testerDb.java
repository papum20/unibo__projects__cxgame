package pndb.delta;

import pndb.delta.constants.CellState;
import pndb.delta.structs.DbSearchResult;

public class testerDb {
	

	public static void main(String[] args) {

		BoardBit.M = 18;
		BoardBit.N = 20;
		BoardBit.X = 7;
		boolean first = true;
		

		DbSearch db = new DbSearch();
		db.init(BoardBit.M, BoardBit.N, BoardBit.X, first);

		BoardBit board = new BoardBit();


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
			"....................",
			"....................",
			"....................",
			"....................",
			"x...................",
			"x...................",
			"xo.....o............"
		   //012345678901234567890123456789012345678901234567
		};

		for(int i = 0; i< BoardBit.M; i++) {
			for(int j = 0; j < BoardBit.N; j++) {
				if(bb[BoardBit.M-i-1].substring(j, j+1).equals("."))
					continue;
				else board.markCheck(j, (bb[BoardBit.M-i-1].substring(j, j+1).equals("x")) ? CellState.P1 : CellState.P2);
			}
		}

		board.print();
		
		System.out.println("db");

		
		DbSearchResult res = db.selectColumn(board, null, 10000, CellState.P2, Operators.MAX_TIER);

		System.out.println((res == null)? null : res.winning_col);
	}

}
