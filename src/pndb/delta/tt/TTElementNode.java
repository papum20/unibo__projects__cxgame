package pndb.delta.tt;

import pndb.delta.PnNode;
import pndb.delta.tt.TranspositionTable.Element;



public class TTElementNode extends Element<TTElementNode> {
		//KEY = key1 + key2 + index = (16+32+16) bit = 64bit

		private		short key1;
		private		int key2;
		public		PnNode node;

		private static final int TABLE_SIZE = 22;
		private static final int MASK2_BITS = TABLE_SIZE + Integer.SIZE;
		private static final int MASK_IDX	= 4194303;		//2**22-1 = 22 ones

		/**
		 * Complexity: O(1)
		 */
		public TTElementNode() {

		}
		public TTElementNode(long key) {
			key2 = (int)(key >> TABLE_SIZE);
			key1 = (short)(key >> MASK2_BITS);
			node = null;
		}
		public TTElementNode(long key, PnNode node) {
			key2 = (int)(key >> TABLE_SIZE);
			key1 = (short)(key >> MASK2_BITS);
			this.node = node;
		}

		/**
		 * Complexity: O(n), with n length of the list
		 * @param e
		 */
		protected void listAppend(TTElementNode e) {
			if(next == null) next = e;
			else next.listAppend(e);
		}
		/**
		 * Returns the element if cmp==this or a next element in the list (assuming the index is the same)
		 * Complexity: O(n), with n length of the list
		 * @param cmp
		 * @return
		 */
		protected TTElementNode listGet(Key k) {
			if (compareKey(k)) return this;
			else if(next == null) return null;
			else return next.listGet(k);
		}

		public static TTElementNode[] getTable() {
			return new TTElementNode[(int)(Math.pow(2, TABLE_SIZE))];
		}
		public static Key calculateKey(long key) {
			return new Key(key, key >> MASK2_BITS, key >> TABLE_SIZE, (int)(key & MASK_IDX));
		}		
		public int calculateIndex(long key) {
			return (int)(key & MASK_IDX);
		}
		protected boolean compareKey(Key k) {
			return ((short)k.key1 == this.key1) && ((int)k.key2 == this.key2);
		}

	}
