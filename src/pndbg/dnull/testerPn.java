package pndbg.dnull;

import connectx.CXBoard;
import pndbg.delta.constants.CellState;



public class testerPn {
	

	public static void main(String[] args) {

		int	M = 38,
			N = 58,
			X = 12;
		boolean first = true;
		
		
		PnSearch pn = new PnSearch();
		pn.initPlayer(M, N, X, first, 10);

		BoardBit board = pn.board;
		CXBoard last_board = new CXBoard(M, N, X);
		

		String[] bb = {
			"..........................................................",
			"..........................................................",
			"..........................................................",
			"..........................................................",
			"x.........................................................",
			"ox........................................................",
			"ox..................x.....................................",
			"xooo..........o.....x.....................................",
			"oxxx..........x.....o.....................................",
			"xoox..........x.....o.....................................",
			"ooox..........x.....o.....................................",
			"xxoo..........x.....o.o...................................",
			"oxxxx.........x.....o.o...................................",
			"xoxxx.........x.....o.o...................................",
			"oooxx.........x.....o.x...................................",
			"oxoxo.........x.....xoo...................................",
			"xooxo.........x.....ooo...................................",
			"xoxox.........x.....ooo...................................",
			"xoxxx......x.xo....oooo...................................",
			"xoxxo......x.xxo...oooo....o..............................",
			"xooxx......x.xoo.o.oooo....o..............................",
			"xooxx......x.ooooo.oooo....o....x.........................",
			"ooxxx......xxxxoxo.oooo.o..x....x.......................x.",
			"xooxx......oxxoxox.oooo.o..x....x.......................x.",
			"xoxxx..x...oxxoooo.ooxoox..o....x.............x.........x.",
			"xooxx..x...oxoooxx.oooxoo..o....x.............x.........x.",
			"xoxxxx.x...oxxoooo.oxoooo..o....o...o.........o.......x.x.",
			"xxoxxx.x...oxxoxoo.xoooooo.o....x..xx.........x......xx.o.",
			"xoxoxo.o...oxooooo.ooooooo.oo...x..xx.........x......xx.o.",
			"xoxooxxoxx.xxooooo.ooooxooooo...x..xx....x....x.....xxx.ox",
			"xoxoxxxoxooxooxoooooooooooxoo...x..xx...xx....x...x.oxx.ox",
			"xooxxoxooooxooxoooxooooxooooooo.x.xxxxo.xx....xx.xx.oox.xx",
			"oxxxxoooooxxxoxoooxooooooxoooooxxxxxxxxxxxo.xoxx.xxxooo.xo",
			"xxoxooooooxxxoxxooxooooooxooxoxoxxxoxxxxxxx.xxxo.xxooox.xo",
			"oooxoxooooxxxoxxxooooxoooooxooooxxxoxxxxxxxxxxxo.xxxooo.xx",
			"xoxxoxoxxooxoxxxxxoooxoxxooxooxoxxooxxoxoxxxxxxooxxxxxo.xx",
			"ooooxxoxxxxoxooxxooxxxoxxooxoxxxoxooxooxoooooxxoxxoxoxoxxx",
			"oxxxooxoxxooxooxoxxoxxxoxooxoxxxoxxxxxooxoxxxxxxoxxoxxxooo"
		};

		for(int i = 0; i< M; i++) {
			for(int j = 0; j < N; j++) {
				if(bb[M-i-1].substring(j, j+1).equals("."))
					continue;
				else {
					board.markCheck(j, (bb[M-i-1].substring(j, j+1).equals("x")) ? CellState.P1 : CellState.P2);
					//last_board.markColumn(j);
				}
			}
		}
		
		//last_board.markColumn(1);
		last_board.markColumn(26);

		// set the player to do next move(set in last_board)
		pn.board.player = CellState.P1;
		
		int res = pn.selectColumn(last_board);

		System.out.println(res);
	}

}
