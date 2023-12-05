package pndb.delta;

import pndb.delta.constants.CellState;
import pndb.delta.structs.DbSearchResult;

public class testerDb {
	

	public static void main(String[] args) {

		BoardBit.M = 38;
		BoardBit.N = 58;
		BoardBit.X = 12;
		boolean first = true;
		

		DbSearch db = new DbSearch();
		db.init(BoardBit.M, BoardBit.N, BoardBit.X, first);

		BoardBit board = new BoardBit();


		String[] bb = {
			"..........................................................",
			"..........................................................",
			"..........................................................",
			"..........................................................",
			"..........................................................",
			"..........................................................",
			"..........................................................",
			"..........................................................",
			"..........................................................",
			"...o......................................................",
			"...o......................................................",
			"...o......................................................",
			"...o......................................................",
			"...o......................................................",
			"...o......................................................",
			"...o......................................................",
			"...o......................................................",
			"...o......................................................",
			"...x......................................................",
			"...o......................................................",
			"...o......................................................",
			".o.o......................................................",
			".o.o......................................................",
			".x.o......................................................",
			".x.o......................................................",
			".x.o......................................................",
			".x.o.....................................................x",
			".x.o.....................................................x",
			".x.o...........................x.........................x",
			"xxxx...........o...............x............o..o.........x",
			"xxox...........oo.o.x.o..o..x..x..x..x...x..x..o.........x",
			"xxox...........oo.ooo.o..o..o..xx.o..x..xx..xx.x.......o.x",
			"xxxo..........ooo.oooxoo.ox.oxxxx.xx.x..xx.xxx.x.......o.o",
			"xoox........o.oxo.oooooo.ox.oxxxxxoxxxxxxxoxxoox.o.....oox",
			"xxxox...x...ooxxo.oooooo.oxooxxooxoxxoxxxoxxooxx.o...o.xxo",
			"xxxxo...o...xxxxxxxxooxooooooxoooooxxooxxoxxoooxxo.o.ooxxo",
			"xxoxxx.xo..oooxxxoxxooxxoxxooooooooxoxxxoxxxxoooxooo.xoxxo",
			"xxooxooox.xxoxoxxooxoxxoooxxooxoxxoxoxoooxxxooxoooxxxxooxo"
		};

		for(int i = 0; i< BoardBit.M; i++) {
			for(int j = 0; j < BoardBit.N; j++) {
				if(bb[BoardBit.M-i-1].substring(j, j+1).equals("."))
					continue;
				else board.markCheck(j, (bb[BoardBit.M-i-1].substring(j, j+1).equals("x")) ? CellState.P1 : CellState.P2);
			}
		}

		board.print();
		board.markCheck(32, CellState.P1);
		board.markCheck(3, CellState.P2);
		board.print();
		

		
		DbSearchResult res = db.selectColumn(board, null, 10000, CellState.P2, Operators.MAX_TIER);

		System.out.println((res == null)? null : res.winning_col);
	}

}
