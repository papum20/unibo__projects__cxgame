package connectx.pndb;



/**
 * Node for DbSearch, including its own board.
 */
public class DbNode {

	public final BoardBitDb board;
	/* 
	 * (byte) max tier to find;
	 * (boolean) is_combination;
	 */
	private byte data;
	

	DbNode(BoardBitDb B, final boolean is_combination, final int max_tier) {
		board = new BoardBitDb(B);
		setData(is_combination, max_tier);
	}


	//#region GET_SET

	private void setData(final boolean is_combination, final int max_tier) {
		data = (byte)( (max_tier << 1) | (is_combination? 1:0) );
	}
	public int getMaxTier() {
		return (data >> 1);
	}
	public int isCombination() {
		return (data & 1);
	}

	//#endregion GET_SET
	
}
