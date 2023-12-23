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
			"ooxx...................ox......x....o.x.x..o...",
			"xxooo....x...........x.xooooo.xo....xxo.xooxxxx",
			"xoxoo...xo...........o.oxooooooox...oox.oxoxoxx",
			"ooxxo...xx...x.......o.xxoxxoooxo...ooxoxxoxxxo",
			"ooooxx..xx...x.......oxoxxooxoooo...ooooxoxxoox",
			"xxxoxx..xox.xxx......xoxoxxoooxxoo..xoxoxoxxxox",
			"oxxxxx..oxoxxxx.....oxxxxooooooooox.ooooxxoxoxx",
			"oxxxox..oxoxxxx..x..oxoxooxoooooxoo.oooooxxxoxo",
			"oxxoox..oxxxxoo.xx..xooxxxxxooooooo.oxoxxoxxxxx",
			"oxxxoo..xxxxxxo.xxxxxxoxxxxoxoxoooo.oooxxxxooxx",
			"oxxoxo..oxoxoxo.xoxoxoxxxooxoooxooooxoxoxxxxoxx",
			"xoxooo..xxxoxxoxxoxxoxxooxxxoxxoooooxoooxxoxxxx",
			"ooxooxx.oxxxooooxxoxoxooxooxoxxoxoxooxxxxxxxxox",
			"oxxxoxo.xxooxxxoxoxxxoxoxxxxxxooxxoooxxxxxxooxx",
			"oooooxo.oxxoxxooxxoxxxooooxooooxxooooxoooxxooxx",
			"xooxoxo.oxxoooxxooxooxoxxoxoooxoxoooxxooxxxxxox",
			"oooxooxxoooxxoxxxxooxxxooxoxooooxxooooxoxxooxoo",
			"xooxoooxxxoooxoxooooxooxxoxxoxoxoxxxoxooooxxxxx",
			"xoooxxooooxxoxxoxxxxxoxoxxoxoooxoxoxoxxoxoxxxoo",
			"oooooxoxxoxooooooxxoxxooxxoooxxxooooxoxxoxxxxxx",
			"xoooooooooxxxxxxxxxoxxxxxxxoxoxooooooxxxooooxxx",
			"ooxooxoooooxxxoxoxxooooooxooooooxoooxoxxxxxoxox",
			"oxoooooxxooooooxooxxxxxxxoxoxooxxxxxxoooxxxxxxo",
			"oxxoxoxxoxoooooxoooooxxooxxxooxxxxooxoooxxxxoxx",
			"xxxooxoxooxoooxxooooooxxxoooxxxxxoxxxooooxxxxoo",
			"xoooxxxoxoxxooooxoooxoxxoxoxxoooxoxooxxxxxxoxxx",
			"xooxxxxxoooxooooooxoooxoxxoxoxoxxoxxxooxoxxxxox",
			"oxxxoxooxxooxxxxoxoooooooxoxxoxxoxooxxxxxxxoxoo",
			"xxoxxoxooooxoxoooxxoxooooxooooooooooxxxoooxooxx",
			"ooxxoxxxoxxoxxoxooxoooooxoxxoxxoxooooxxooxoxoox",
			"oxxxxxoxxxxooxxxxxxoxoooooxooxoooxooooxxxxoooox",
			"xxoxoxxxoxoooooxxoxxooooxxooooooxoxxxooxxoxxxox"
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
		last_board.markColumn(0);

		// set the player to do next move(set in last_board)
		pn.board.player = CellState.P2;
		
		int res = pn.selectColumn(last_board);

		System.out.println(res);
	}

}
