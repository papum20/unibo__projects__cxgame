package pndb.delta;

import pndb.delta.constants.CellState;
import pndb.delta.structs.DbSearchResult;

public class testerDb {
	

	public static void main(String[] args) {

		BoardBit.M = 70;
		BoardBit.N = 47;
		BoardBit.X = 11;
		boolean first = true;
		

		DbSearch db = new DbSearch();
		db.init(BoardBit.M, BoardBit.N, BoardBit.X, first);

		BoardBit board = new BoardBit();


		String[] bb = {
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"...............................................",
			"xooxxxooxooxxxoooxxxooxxx.xxxxx.xxx.xxxx.oxx.xo",
			"xxooooxoxoxoooxxxooooxoxoooooooxoooxxooxooxo.oo",
			"oxxoxxxxooxooxxxooooooxoooooooxxooxxooxxxooooox",
			"xxxooxxoxxoxxoxxoooooxooxoooooooxxooooxxxxooxxo",
			"xxxooxxxooxooxxxxxoxoxxxxxooooooxoooooxxoxxoooo",
			"xxxxxxxoxxooxoxxxxxxoxxxxoooxoooxoooooxxxxoooox",
			"xxxoxoxxooxxoxxxxoxxxooxxxxxoxooxoooxoxoxxooooo",
			"xxooxoxoxxooxoxxoxxxoxxooxooxoooxoooooooxxooooo",
			"xxxoxoxxooxxoxxxxxxoxxooxxxoooooxoooooooxxooxoo",
			"xxxxxoxooxooxooxxxxxxoxxxxxxxoooxoooooxoxxoxxxo",
			"xxxxxooxooxxoxxxxoxxoxxxxxoxxoooxoooooxxxxooooo",
			"xoxxxoooxxooxoxoxooxxoxoxoooxoxxooxxoxxoxoooooo",
			"xxxxxooxooxxoxxxoxoxxxxxxxoxooxxxxxxooxoxoooooo",
			"oxxxoxxooxooxoxxxxoxooxxxxxxoooxoxxxoooooxxoooo",
			"xxxxxoxoooxxoxoxxooxxoxxxxxxooxooxxxooxooxxooox",
			"xoxoooxooxooxoxoxoooxooxxxxxoxoxoxxxxooooxxoooo",
			"ooooooxoooxxoxoxoooxxoxxoxxxooxoooxxxoxooxxoooo",
			"ooxoooxooxooxoxxxoooxooxxoxxooxxoxoooooooxxoxoo",
			"xoxoooxoooxxoxooooooxooxooxoooxxoxooooxoooxxxxo",
			"oxxoooxxoxooxoxxooxoxooxooooxxxxoxoooooxxoxxxxo",
			"xxxoooxoxoxxoxooooooxoooooooxxxxoxxooxxoxoxxxoo",
			"oxxoooxoooooxoxoooxoooxooxooxoxxxxooooxxooxxxxo",
			"xoxooxxoooxoxxoxoxxooxxxoxooxxxxxxooxxooxxxxxxo",
			"oxoxxxooooooxooooxxoxoxxxxoxxxxxxxoxxxooxxoxoxx",
			"xxoxoooooxooxooxxxxoxoxxxxxxxxoxxxoxxxooxxxxxxx",
			"xxoxoooooxooooxxxxxxxxoxxxxxoxxxxooxxxoxoxxxxxx",
			"xxxxoooooxoooxxxxxxxoooxoxxxoxooxooxxxoxoxoxxxx",
			"xxxxoooooxxxxxxxoxxxooooxxoooxoooooxxoxxoooxxxx",
			"xxxxooxxoxoxxxxxoxoxoooooooxxxoooxoxxooxoxooxxx",
			"xxxooooxxxooxxooooooooxooxoooxxxoxxxxooxxoooxox",
			"xxxxooxooxooxooooxoooooooooxooooxoxoxooxoxoooxo",
			"xoxxooxoxxoooxoxxxxxxoxoxoxxxoxoooooxxxooxxoxxo"		
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
