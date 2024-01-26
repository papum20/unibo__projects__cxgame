package pndbg.old.delta.tt;

import pndbg.old.delta.PnNode;
import pndbg.old.delta.tt.TTElementNode.KeyDepth;
import pndbg.old.delta.tt.TranspositionTable.Element;
import pndbg.old.delta.tt.TranspositionTable.Element.Key;



public class TTElementNode extends Element<TTElementNode, KeyDepth> {
		//KEY = key1 + key2 + index = (16+32+16) bit = 64bit

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

		private		short key1;
		private		int key2;
		public		PnNode node;
		public	short	depth;			// node depth, used "as key", to reduce ambiguity

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
		public TTElementNode(long key, int depth, PnNode node) {
			key2 = (int)(key >> TABLE_SIZE);
			key1 = (short)(key >> MASK2_BITS);
			this.node = node;
			this.depth = (short)depth;
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
		protected TTElementNode listGet(KeyDepth k) {
			if (compareKey(k)) return this;
			else if(next == null) return null;
			else return next.listGet(k);
		}

		public static TTElementNode[] getTable() {
			return new TTElementNode[(int)(Math.pow(2, TABLE_SIZE))];
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
			return ((short)k.key1 == this.key1) && ((int)k.key2 == this.key2) && depth == k.depth;
		}

	}
