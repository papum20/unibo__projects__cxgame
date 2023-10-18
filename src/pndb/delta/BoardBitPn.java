package pndb.delta;

import pndb.delta.constants.Auxiliary;
import pndb.delta.tt.TTElementProved;
import pndb.delta.tt.TranspositionTable;




public class BoardBitPn extends BoardBit {
	
	public static TranspositionTable<PnTTnode, PnTTnode.KeyDepth> TTdag;
	public static TranspositionTable<TTElementProved, TTElementProved.KeyDepth> TTproved;
	public long hash;
	private static TTElementProved.KeyDepth	key_proved	= new TTElementProved.KeyDepth();
	private static PnTTnode.KeyDepth key_dag			= new PnTTnode.KeyDepth();

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
	 * @param attacker
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


	//#region TT

		/**
		 * Get entry from TTdag.
		 * @param hash node's hash
		 * @param col set to col for node's child if want entry for child, else TTElementProved.COL_NULL
		 * @param depth node's depth
		 * @param attacker current player at node (in case, who's making move col)
		 * @return entry
		 */
		public PnTTnode getEntry(int col, int depth) {
			return (col == TTElementProved.COL_NULL) ? 
			TTdag.get(PnTTnode.setKey(key_dag, hash, depth))
			: TTdag.get(PnTTnode.setKey(key_dag, TTdag.getHash(hash, free[col], col, Auxiliary.getPlayerBit(player)), depth + 1));
		}
		public PnTTnode getEntryParent(int col, int depth) {
			return TTdag.get(PnTTnode.setKey(key_dag, TTdag.getHash(hash, free[col] - 1, col, Auxiliary.getPlayerBit(Auxiliary.opponent(player))), depth - 1));
		}
		public void addEntry(PnTTnode node) {
			TTdag.insert(hash, node);
			TTdag.count++;
		}
		public void removeEntry(int depth) {
			TTdag.remove(PnTTnode.setKey(key_dag, hash, depth));
			TTdag.count--;
		}
		public TTElementProved getEntryProved(int col, int depth) {
			return (col == TTElementProved.COL_NULL) ? 
			TTproved.get(TTElementProved.setKey(key_proved, hash, depth))
			: TTproved.get(TTElementProved.setKey(key_proved, TTdag.getHash(hash, free[col], col, Auxiliary.getPlayerBit(player)), depth + 1));
		}
		public void addEntryProved(TTElementProved node) {
			TTproved.insert(hash, node);
			TTproved.count++;
		}
	
	//#endregion TT

	
}