package pndb.nonmc;

import pndb.alpha.BoardBit;
import pndb.alpha._BoardBitDb;




public class BoardBitDb extends _BoardBitDb<BoardBitDb, BoardBit> {
	


	public BoardBitDb(int M, int N, int X) {
		super(M, N, X);
	}

	public BoardBitDb(BoardBit B) {
		super(B);
	}
	
	private BoardBitDb(BoardBitDb B, boolean copy_threats) {
		super(B, copy_threats);
	}

	public BoardBitDb getCopy(boolean copy_threats) {
		return new BoardBitDb(this, copy_threats);
	}

	

}