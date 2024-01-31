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
			"oooxoxxooooxooxoxxoxx.xx.x.xxx.x.xx.x....oxx..x...x....x..",
			"xoxooxxooxxxoxxoooooxxoxooxoooooxooxooxoxxooxoo.oxo.o.oo..",
			"xooxxxxxxxxxoxxooxxxooxxxxxxxoooxxxxxxxxxoxoxxxxxxxxxoxxxo",
			"xoxxooooooooxxxxxxxxxoxxxxxxooxxxxoxxxxoooxxxxxxxxxoxxxxxo",
			"xxxoxxoxxxxxxoxxoxxxxoxoxxoxoooooooxoxooxxxxxxxoxxxoxxoxxo",
			"oxooxooxooxooooxxooxoooooooxxxxxxxxxoxoxxooooxooxoxoxxxxox",
			"xxxoxoxxoxxooxoxxoxxoxoxoooooxxooooooxoxooxxxxooooooxxxoxo",
			"xxoxxxxxoooxxxoxxxoxoxoxoooooxoxxxxxxooxoxxooooxoooooxxxxo",
			"xxoxxxxxxxoooxxxxxxooxxxxxoxoxoxxxoxxxxxoxoxooxxoooooxxxxo",
			"xxoxoxooooxxoxxoxxooxxoxxxoxxooooxooooooxooxxoxxxxoooxxxxo",
			"xoxxxxxxxxooxxoxxxxxxxoxoxxooxoxxxoxxxxxxooxxxxxxooooxxoxo",
			"xxoxxxxoooxxoxxoxxoxoxoxxxooxxxooxooooooxooxxxxxxxoooxxxxo",
			"xxoxxxxxxxooxoxxxxxooxooxxoxoxxxxoxxxxxxxooxxxoxxoxxxoxxxo",
			"xxooxxooooxxoxxoxxoxoooxxooxxxxoooooooooxoxxxxoxxxxooxxxoo",
			"xxoxxoxxxxoooxxxoxxxoxoxooxxooxooooxxxxxooooxxooxoxxxxoxoo",
			"oxxoxxxoooxxxxxxxxoxoxxxxooxxoxoxoooooooxxooxxooxoxooooxox",
			"xxxxxooxxxoooxxoxoxxoxoxoxooooxoooxxxxxxoxoxxxoxxxxxxxoxox",
			"xxxxxxxoooxxxxxxxooxoxoxxxoxxxooxoooooooxxoxoxxoooxooooxox",
			"oxxxxxoxxxoooxxoxxxxoxoxoooxooxoxooxxxxxoxoxxxoooxxxxxoxoo",
			"xxxxoxooooxxxxxxxooxxxoxoxooooxxxoooooooxxoxoooooooxxooxxo",
			"xxxooxoxxxoooxxxxoxxoxoxooooooxoxxoxxxxxoxoxoooooxoxoxxooo",
			"xoxxoxooooxxxxooxooxxxoxooooooxoxooooxooxxoxooooooooxxoooo",
			"xxoxoxoxxxoooooxxoxooxoooooooxxoxooxxxxxoxoxoooooxoxoooooo",
			"xxxxoxooooxxxooxxooxxooxooooooxooooooxooxoxxooooxoxoxxoooo",
			"xxoxoxoxxxoooxooxoxooooxooxoooxoxooxxxxxoxoxooooxxxoooxoox",
			"xxoooxoxooxxxooooooxooxoooooxxxoxooooxooxooooxooxxxoxxoxox",
			"xxoxxoxoxxooooooxoooxxxxxoooooxoxoxxxxxxoxoxoxoxxoxoxxooox",
			"xxoxoooxooxxxooxoooooxxxxooxxxooxoxoooooxxoooxxxooxooxooox",
			"oxoxooooxxooooxoxxxxxxoxoxoxooooxoxoxoxxoxooxxxoooxoxxooox",
			"oxoxoooxooxxoxooooooooxxxxoooooxxoxxoxooxxooooxoooxoooooxx",
			"ooooooooxxooxoooooooooxxxxoooxoxxxxxoxxxoxooooooxoxoxxooxx",
			"xxoxooooooxxooooooooxxoxxooooxoxxxxxxxooxxoxooxoxoxoxoooxx",
			"xooxooooxxooxxoooooooxoooxoooxxxxxoxoxxxxxoxxxoooooxooooxo",
			"xxooooooooxxooooooooooxxoxoooxxooxxxxoxooxxxxoxoooxxxoooox",
			"xooooooooxooxoooooooxooxxxoooxooxooxooxooxoxooxoxooxxoxoox",
			"xxxooxoxooxxoxooooooxxxxooxxxoxoxxxooxxxooxoxxooxxxxxooxxo",
			"xooxxxxooxooxoooooooxxoooooxxxooxoooxooxxxxoooxoxooooxoxox",
			"xoooooxoxxxxoooxooooooxoooooxooooxxoxxxoxxxoxxoxxoooooooox"
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
