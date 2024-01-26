package pndb.delta;

import pndb.delta.constants.CellState;
import pndb.delta.structs.DbSearchResult;
import pndb.delta.tt.TranspositionTable;

public class testerDb {
	

	public static void main(String[] args) {

		BoardBit.M = 9;
		BoardBit.N = 9;
		BoardBit.X = 5;
		boolean first = true;
		

		TranspositionTable.initMovesHashes(BoardBit.M, BoardBit.N);

		DbSearch db = new DbSearch();
		db.init(BoardBit.M, BoardBit.N, BoardBit.X, first);

		BoardBit board = new BoardBit();


		String[] bb = {
			".........",
			".........",
			".........",
			".........",
			"..oo.oo..",
			"..xo.ox..",
			"..ooxoo..",
			"..xxxxx..",
			"..xoxxx.."
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

		
		DbSearchResult res = db.selectColumn(board, null, 10000, CellState.P1, Operators.MAX_TIER);

		System.out.println((res == null)? null : res.winning_col);
	}

}
