package pndb.delta.tt;

import pndb.delta.tt.TranspositionTable.Element;
import pndb.delta.tt.TranspositionTable.Element.Key;
import pndb.delta.tt.TTElementProved.KeyDepth;



public class TTElementProved extends Element<TTElementProved, KeyDepth> {

	public static class KeyDepth extends Key {
		protected short depth;

		public KeyDepth() {
			super();
		}
		public KeyDepth(long key, long key1, long key2, int index, int depth) {
			super(key, key1, key2, index);
			this.depth = (short)depth;
		}
		public void set(long key, long key1, long key2, int index, int depth) {
			super.set(key, key1, key2, index);
			this.depth = (short)depth;
		}
	}
	
	//KEY = key1 + key2 + index = (16+32+16) bit = 64bit

	private	short	key1;
	private	int		key2;

	public	short	depth_cur;			// node depth, used "as key", to reduce ambiguity
	public	short	depth_reachable;	// max depth reachable from node (in game tree)
	public	byte	val;				// (from left, i.e. most significative) bit 0=true if p1 won; bit 1-7:move (col) to reach max depth

	private static final int TABLE_SIZE = 22;
	private static final int MASK2_BITS = TABLE_SIZE + Integer.SIZE;
	private static final int MASK_IDX	= 4194303;		//2**22-1 = 22 ones

	private static final byte	BIT_WON		= 7,
								MASK_WON	= -0x80,
								MASK_COL	= 0x7f;

	public static final byte COL_NULL = MASK_COL;

	/**
	 * Complexity: O(1)
	 */
	public TTElementProved() {

	}
	public TTElementProved(long key) {
		key2 = (int)(key >> TABLE_SIZE);
		key1 = (short)(key >> MASK2_BITS);
	}
	public TTElementProved(long key, int depth_cur, int depth_reachable, boolean won, int col) {
		key2 = (int)(key >> TABLE_SIZE);
		key1 = (short)(key >> MASK2_BITS);
		this.depth_cur			= (short)depth_cur;
		this.depth_reachable	= (short)depth_reachable;
		this.val = (byte)((won ? MASK_WON : 0) | (col & MASK_COL));
	}

	/**
	 * Complexity: O(n), with n length of the list
	 * @param e
	 */
	protected void listAppend(TTElementProved e) {
		if(next == null) next = e;
		else next.listAppend(e);
	}
	/**
	 * Returns the element if cmp==this or a next element in the list (assuming the index is the same)
	 * Complexity: O(n), with n length of the list
	 * @param cmp
	 * @return
	 */
	protected TTElementProved listGet(KeyDepth k) {
		if (compareKey(k)) return this;
		else if(next == null) return null;
		else return next.listGet(k);
	}

	public void set(boolean won, int col, short depth_reachable) {
		this.val = (byte)((val & ~MASK_WON & ~MASK_COL) | (won ? MASK_WON : 0) | (col & MASK_COL));
		this.depth_reachable = depth_reachable;
	}

	public static TTElementProved[] getTable() {
		return new TTElementProved[(int)(Math.pow(2, TABLE_SIZE))];
	}
	public static KeyDepth calculateKey(long key, int depth) {
		return new KeyDepth(key, key >> MASK2_BITS, key >> TABLE_SIZE, (int)(key & MASK_IDX), depth);
	}			
	public static KeyDepth setKey(KeyDepth k, long key, int depth) {
		k.set(key, key >> MASK2_BITS, key >> TABLE_SIZE, (int)(key & MASK_IDX), depth);
		return k;
	}			
	public int calculateIndex(long key) {
		return (int)(key & MASK_IDX);
	}
	protected boolean compareKey(KeyDepth k) {
		return ((short)k.key1 == this.key1) && ((int)k.key2 == this.key2) && depth_cur == k.depth;
	}

	public int col() {
		return val & MASK_COL;
	}
	public boolean won() {
		return (val & MASK_WON) == MASK_WON;
	}

}
