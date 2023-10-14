package pndb.delta;

import pndb.delta.constants.Auxiliary;
import pndb.delta.tt.TTElementProved;
import pndb.delta.tt.TranspositionTable;




public class BoardBitPn extends BoardBit {
	
	public static TranspositionTable<PnTTnode, PnTTnode.KeyDepth> TTdag;
	public static TranspositionTable<TTElementProved, TTElementProved.KeyDepth> TTproved;
	public long hash;

	public byte player;


	/**
	 *  Complexity: O(3N) if M <= 64 else O(5N)
	 * @param M
	 * @param N
	 * @param X
	 * @param player CellState
	 */
	public BoardBitPn(int player) {
		super();
		hash = 0;
		this.player	= (byte)player;
	}
	public BoardBitPn(BoardBitPn B) {
		copy(B);
	}
	public void copy(BoardBitPn B) {
		super.copy(B);
		hash	= B.hash;
		player	= B.player;
	}

	/**
	 * Complexity: O(1)
	 * @param col
	 * @return GameState
	 */
	public void mark(int col) {
		hash	= TTdag.getHash(hash, free[col], col, Auxiliary.getPlayerBit(player));
		super.mark(col, player);
		player = Auxiliary.opponent(player);
	}
	/**
	 * Complexity: O(4X)
	 * @param col
	 * @param player
	 * @return GameState
	 */
	public byte markCheck(int col) {
		mark(col);
		return check(free[col] - 1, col, player);
	}

	/**
	 * Complexity: O(1)
	 */
	@Override
	public void unmark(int col) {
		hash	= TTdag.getHash(hash, free[col] - 1, col, _cellState(free[col] - 1, col));
		super.unmark(col);
		player = Auxiliary.opponent(player);
	}


}