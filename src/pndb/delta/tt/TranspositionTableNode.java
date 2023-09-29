package pndb.delta.tt;

import java.util.Random;
import pndb.delta.PnNode;





public class TranspositionTableNode {

	private long[][][] moves;		//random hashes defined for each move
	private Element[] table;		//actual array where entries are stored
	private int size;

	private static final int PLAYERS_N = 2;



	/**
	 * Complexity: O(M*N*PLAYERS_N + size + M*N*PLAYERS_N) = O(2MN + 2**16 + 2MN) = O(4MN + 2**16)
	 * @param M
	 * @param N
	 */
	public TranspositionTableNode(int M, int N) {
		Random random = new Random();
		moves = new long[M][N][PLAYERS_N];
		size = (int)(Math.pow(2, Element.tableSize()));
		table = new Element[size];

		for(int i = 0; i < M; i++) {
			for(int j = 0; j < N; j++) {
				for(int k = 0; k < PLAYERS_N; k++) {
					moves[i][j][k] = random.nextLong();
				}
			}
		}
	}

	/**
	 * Complexity: O(1)
	 * @param hash
	 * @param i
	 * @param j
	 * @param k
	 * @return
	 */
	public long getHash(long hash, int i, int j, int k) {
		long move_hash = moves[i][j][k];
		return (hash ^ move_hash);
	}

	/**
	 * Complexity: O(n), with n length of the list
	 * @param key
	 */
	public void insert(long key) {
		Element e = new Element(key);
		int index = Element.index(key);
		if(table[index] == null)
			table[index] = e;
		else
			table[index].listAppend(e);
	}
	/**
	 * Complexity: O(n), with n length of the list
	 * @param key
	 * @param state
	 * @param idx: 0=attacker, 1=defender, 2=both
	 */
	public void insert(long key, PnNode node) {
		Element e = new Element(key);
		e.setNode(node);
		int index = Element.index(key);
		if(table[index] == null)
			table[index] = e;
		else
			table[index].listAppend(e);
	}

	/**
	 * Remove element entry.
 	 * Complexity: O(n), with n length of the list
	 * @param key
	 */
	  public void remove(long key) {
		int index = Element.index(key);
		Element compare = new Element(key);
		Element e = table[index];
		
		if(e != null) {
			while(e.next != null) {
				if(e.next.equals(compare)) {
					e.next = e.next.next;
					return;
				}
				e = e.next;
			}
		}
	}

	/**
	 * Complexity: O(n), with n length of the list
	 * @param key
	 * @return
	 */
	public Boolean exists(long key) {
		int index = Element.index(key);
		if(table[index] == null) return false;
		else {
			Element compare = new Element(key);
			return (table[index].getNext(compare) != null);
		}
	}
	/**
	 * Complexity: O(n), with n length of the list
	 * @param key
	 * @return
	 */
	public PnNode getNode(long key) {
		int index = Element.index(key);
		if(table[index] == null) return null;
		else {
			Element compare = new Element(key);
			Element e = table[index].getNext(compare);
			if(e == null) return null;
			else return e.getNode();
		}
	}

	/**
	 * Complexity: O(n), with n length of the list
	 * @param key
	 * @param node
	 */
	public void setNode(long key, PnNode node) {
		int index = Element.index(key);
		if(table[index] != null) {
			Element compare = new Element(key);
			Element e = table[index].getNext(compare);
			e.setNode(node);
		}
	}



	private class Element {
		//KEY = key1 + key2 + index = (16+32+16) bit = 64bit

		private short key1;
		private int key2;
		private PnNode node;
		protected Element next;

		private static final int TABLE_SIZE = 22;
		private static final int MASK2_BITS = TABLE_SIZE + Integer.SIZE;
		private static final int MASK_IDX	= 4194303;		//2**22-1 = 22 ones

		/**
		 * Complexity: O(1)
		 */
		protected Element(long key) {
			key2 = (int)(key >> TABLE_SIZE);
			key1 = (short)(key >> MASK2_BITS);
			node = null;
		}

		/**
		 * Complexity: O(n), with n length of the list
		 * @param e
		 */
		protected void listAppend(Element e) {
			if(next == null) next = e;
			else next.listAppend(e);
		}

		/**
		 * Complexity: O(1)
		 */
		protected void setNode(PnNode node) {
			this.node = node;
		}

		/**
		 * Complexity: O(1)
		 * @return
		 */
		protected PnNode getNode() {
			return node;
		}

		/**
		 * Returns the element if cmp==this or a next element in the list (assuming the index is the same)
		 * Complexity: O(n), with n length of the list
		 * @param cmp
		 * @return
		 */
		protected Element getNext(Element cmp) {
			if (equals(cmp)) return this;
			else if(next == null) return null;
			else return next.getNext(cmp);
		}

		/**
		 * Complexity: O(1)
		 * @return
		 */
		protected static int tableSize() {
			return TABLE_SIZE;
		}

		/**
		 * Complexity: O(1)
		 * @param key
		 * @return
		 */
		protected static int index(long key) {
			return (int)(key & MASK_IDX);
		}

		/**
		 * Complexity: O(1)
		 * @param cmp
		 * @return
		 */
		protected boolean equals(Element cmp) {
			return (cmp.key1 == key1) && (cmp.key2 == key2);
		}
	}

}
