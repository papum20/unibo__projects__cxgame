package pndbg.dnull.tt;

import pndbg.dnull.tt.TranspositionTable.Element;
import pndbg.dnull.tt.TranspositionTable.Element.Key;



public class TTElementBool extends Element<TTElementBool, Key> {
		//KEY = key1 + key2 + index = (16+32+16) bit = 64bit

		private	short key1;
		private	int key2;
		public	byte val;

		private static final int TABLE_SIZE = 16;
		private static final int MASK2_BITS = TABLE_SIZE + Integer.SIZE;
		private static final int MASK_IDX	= 65535;		//2**16-1 = 16 ones

		/**
		 * Complexity: O(1)
		 */
		public TTElementBool() {

		}
		public TTElementBool(long key) {
			key2 = (int)(key >> TABLE_SIZE);
			key1 = (short)(key >> MASK2_BITS);
		}
		public TTElementBool(long key, int val) {
			key2 = (int)(key >> TABLE_SIZE);
			key1 = (short)(key >> MASK2_BITS);
			this.val = (byte)val;
		}

		/**
		 * Complexity: O(n), with n length of the list
		 * @param e
		 */
		protected void listAppend(TTElementBool e) {
			if(next == null) next = e;
			else next.listAppend(e);
		}
		/**
		 * Returns the element if cmp==this or a next element in the list (assuming the index is the same)
		 * Complexity: O(n), with n length of the list
		 * @param cmp
		 * @return
		 */
		protected TTElementBool listGet(Key k) {
			if (compareKey(k)) return this;
			else if(next == null) return null;
			else return next.listGet(k);
		}

		public static TTElementBool[] getTable() {
			return new TTElementBool[(int)(Math.pow(2, TABLE_SIZE))];
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

	}
