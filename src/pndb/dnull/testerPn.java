package pndb.dnull;

import connectx.CXBoard;
import pndb.delta.constants.CellState;



public class testerPn {
	

	public static void main(String[] args) {

		int	M = 70,
			N = 47,
			X = 11;
		boolean first = true;
		
		
		PnSearch pn = new PnSearch();
		pn.initPlayer(M, N, X, first, 10);

		BoardBit board = pn.board;
		CXBoard last_board = new CXBoard(M, N, X);
		

		String[] bb = {
			".......................................x..................",
			".......................................x..................",
			".......................................x.....x............",
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
		last_board.markColumn(57);

		// set the player to do next move(set in last_board)
		pn.board.player = CellState.P2;
		
		int res = pn.selectColumn(last_board);

		System.out.println(res);
	}

}
