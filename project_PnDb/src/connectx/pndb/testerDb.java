package connectx.pndb;

import connectx.pndb.constants.CellState;
import connectx.pndb.structs.DbSearchResult;
import connectx.pndb.tt.TranspositionTable;

public class testerDb {
	

	public static void main(String[] args) {

		BoardBit.M = 38;
		BoardBit.N = 58;
		BoardBit.X = 12;
		boolean first = true;
		

		TranspositionTable.initMovesHashes(BoardBit.M, BoardBit.N);

		DbSearch db = new DbSearch();
		db.init(BoardBit.M, BoardBit.N, BoardBit.X, first);

		BoardBit board = new BoardBit();


		String[] bb = {
			".......................................x..................",
			".......................................x..................",
			".......................................x.....x...........o",
			".......................................x.....x....o......o",
			".......................................x.....x....o......o",
			"......................................xx.....xx...o......o",
			"......................................xx.....xx...o......o",
			"......................................xx.....ox...o......o",
			"......................................xx.....xx...x......o",
			"......................................xx.....ox..ooo.....x",
			"......................................xx.....ox..ooo.....o",
			"......................................xox....ox..ooo...o.o",
			"......................................oxx...xxo.xooo..xx.o",
			"......................................xox...xox.oxox..ooxo",
			"......................................xxx...xxx.oooo..ooxo",
			"......................................oxx...xxo.oooo..xoxo",
			"......................................xxx...xox.oooo.ooooo",
			"......................................xxx.x.xxo.oooxoooooo",
			"......................................xxx.x.xxx.ooxooooooo",
			"......................................xxx.x.xxx.ooooooooxx",
			"......................................xxx.xxxxx.ooxooooooo",
			"....................................x.xxo.xxoxx.ooxoxxoxoo",
			"....................................x.xxx.xxoxx.xoxooooooo",
			"....................................o.xox.xxxxx.oxxooxoxoo",
			"x...................................x.xxx.xxxxx.ooxxxooooo",
			"o...........o.......................x.oxx.xxxxxoxoxxoxxooo",
			"ox..........o.......................x.xxx.xxxoxxxxxooooooo",
			"oo..........o......................xx.xxx.oxxxoxxxxxxooxoo",
			"oo..........o......................xxxxxx.xxoxxxxxxxxxoooo",
			"xo.....o...oo......................oxxxxx.xoxxxxxxoxxxooxx",
			"ox...o.oo..oo......................xoxxxxxxxxxxxooxxxxooxo",
			"oo...o.oo..oo...........o..o.....o.xoxxxoxxxxxxxxxxoxxxooo",
			"oox.oo.ox.oooo.o..o.....o..o.....o.xxxxxxxoxxxxoxoxoxxxxox",
			"oox.oo.oxooooo.o..oo....o.ox.....o.oxoxoxxoxxxxxxxoxoxoxox",
			"oxo.oo.oxooxxoooo.xoo..xooooo.xooo.ooxxxxxoxxxxxxooxooxoxx",
			"oxo.oo.oooxxoooxx.ooo..ooooox.oooo.ooxoxooxooxxxxoooxooxoo",
			"xox.xxooxoxooxoooooooooooxooooxoxoooooxoooxxxoxxxxxoooxoxx",
			"ooxxxxoxoxooxxxxooxoxoooxoxxoxxxooooxooooxxoxoooxoxxxoxoxo"
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
