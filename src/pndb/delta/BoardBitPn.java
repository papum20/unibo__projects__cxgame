package pndb.delta;

import pndb.delta.constants.Auxiliary;
import pndb.delta.constants.GameState;
import pndb.delta.tt.TTElementNode;
import pndb.delta.tt.TTElementProved;
import pndb.delta.tt.TranspositionTable;




public class BoardBitPn extends BoardBit {
	
	public static TranspositionTable<TTElementNode, TTElementNode.KeyDepth> TTdag;
	public static TranspositionTable<TTElementProved, TTElementProved.KeyDepth> TTproved;
	public long hash_dag;
	public long hash_proved;



	/**
	 *  Complexity: O(3N) if M <= 64 else O(5N)
	 * @param M
	 * @param N
	 * @param X
	 */
	public BoardBitPn(int M, int N, int X) {
		super(M, N, X);
		hash_dag	= 0;
		hash_proved = 0;
	}

	/**
	 * Complexity: O(1)
	 * @param col
	 * @param player
	 * @return GameState
	 */
	@Override
	public void mark(int col, byte player) {
		hash_dag	= TTdag.getHash(hash_dag, free[col], col, Auxiliary.getPlayerBit(player));
		hash_proved	= TTproved.getHash(hash_proved, free[col], col, Auxiliary.getPlayerBit(player));
		super.mark(col, player);
	}
	/**
	 * Complexity: O(4X)
	 * @param i
	 * @param j
	 * @param player
	 * @return
	 */
	protected byte check(int i, int j, byte player) {
		if(isWinningMove(i, j)) game_state = cell2GameState(player);
		else if(free_n == 0) game_state = GameState.DRAW;
		else game_state = GameState.OPEN;
		
		return game_state;

	}
	/**
	 * Complexity: O(4X)
	 * @param col
	 * @param player
	 * @return GameState
	 */
	public byte markCheck(int col, byte player) {
		mark(col, player);
		return check(free[col] - 1, col, player);
	}

	/**
	 * Complexity: O(1)
	 */
	@Override
	public void unmark(int col) {
		hash_dag	= TTdag.getHash(hash_dag, free[col] - 1, col, _cellState(free[col] - 1, col));
		hash_proved	= TTproved.getHash(hash_proved, free[col] - 1, col, _cellState(free[col] - 1, col));
		super.unmark(col);
	}


}