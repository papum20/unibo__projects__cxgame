package pndb.delta.tt;

import pndb.delta.tt.TranspositionTable.Element;



public class TTElementProved extends Element<TTElementProved> {
		//KEY = key1 + key2 + index = (16+32+16) bit = 64bit

		private	short	key1;
		private	int		key2;

		public	short	depth;			// max depth reachable from node (in game tree)
		public	byte	val;			// (from left, i.e. most significative) bit 0=true if p1 won; bit 1-7:move (col) to reach max depth

		private static final int TABLE_SIZE = 22;
		private static final int MASK2_BITS = TABLE_SIZE + Integer.SIZE;
		private static final int MASK_IDX	= 4194303;		//2**22-1 = 22 ones

		private static final byte	BIT_WON		= 7,
									MASK_WON	= -0x80,
									MASK_COL	= 0x7f;

		/**
		 * Complexity: O(1)
		 */
		public TTElementProved() {

		}
		public TTElementProved(long key) {
			key2 = (int)(key >> TABLE_SIZE);
			key1 = (short)(key >> MASK2_BITS);
		}
		public TTElementProved(long key, int depth, boolean won, int col) {
			key2 = (int)(key >> TABLE_SIZE);
			key1 = (short)(key >> MASK2_BITS);
			this.depth = (short)depth;
			this.val = (byte)((won ? MASK_WON : 0) | col);
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
		protected TTElementProved listGet(Key k) {
			if (compareKey(k)) return this;
			else if(next == null) return null;
			else return next.listGet(k);
		}

		public static TTElementProved[] getTable() {
			return new TTElementProved[(int)(Math.pow(2, TABLE_SIZE))];
		}
		public static Key calculateKey(long key) {
			return new Key(key, key >> MASK2_BITS, key >> TABLE_SIZE, (int)(key & MASK_IDX));
		}			
		public static Key setKey(Key k, long key) {
			k.set(key, key >> MASK2_BITS, key >> TABLE_SIZE, (int)(key & MASK_IDX));
			return k;
		}			
		public int calculateIndex(long key) {
			return (int)(key & MASK_IDX);
		}
		protected boolean compareKey(Key k) {
			return ((short)k.key1 == this.key1) && ((int)k.key2 == this.key2);
		}

		public int col() {
			return val & MASK_COL;
		}
		public boolean won() {
			return (val & MASK_WON) == MASK_WON;
		}

	}
