package pndb.tt;

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
			table[index].addNext(e);
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
			table[index].addNext(e);
	}
	/**
	 * Complexity: O(n), with n length of the list
	 * @param key
	 * @param state
	 * @param idx
	 * @param depth
	 */
	public void insert(long key, PnNode node, short depth) {
		Element e = new Element(key);
		e.set(node, depth);
		int index = Element.index(key);
		if(table[index] == null)
			table[index] = e;
		else
			table[index].addNext(e);
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
	 * @return
	 */
	public short getFinalDepth(long key) {
		int index = Element.index(key);
		if(table[index] == null) return -1;
		else {
			Element compare = new Element(key);
			Element e = table[index].getNext(compare);
			if(e == null) return -1;
			else return e.getFinalDepth();
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
	/**
	 * Complexity: O(n), with n length of the list
	 * @param key
	 * @param node
	 * @param depth
	 */
	public void setNode(long key, PnNode node, short depth) {
		int index = Element.index(key);
		if(table[index] != null) {
			Element compare = new Element(key);
			Element e = table[index].getNext(compare);
			e.set(node, depth);
		}
	}
	/**
	 * Complexity: O(n), with n length of the list
	 * @param key
	 * @param depth
	 */
	public void setDepth(long key, short depth) {
		int index = Element.index(key);
		if(table[index] != null) {
			Element compare = new Element(key);
			Element e = table[index].getNext(compare);
			e.setDepth(depth);
		}
	}

	/**
	 * Try to set state, or insert if doesn't exist.
	 * Complexity: O(2n), with n length of the list
	 * @param key
	 */
	public void setNodeOrInsert(long key, PnNode node) {
		if(exists(key))
			setNode(key, node);
		else
			insert(key, node);
	}
	/**
	 * Complexity: O(2n), with n length of the list
	 * @param key
	 * @param state
	 * @param idx
	 * @param depth
	 */
	public void setNodeOrInsert(long key, PnNode node, short depth) {
		if(exists(key))
			setNode(key, node, depth);
		else
			insert(key, node, depth);
	}

	/**
	 * Complexity: O(1)
	 * @param key
	 */
	public void clear(long key) {
		int index = Element.index(key);
		table[index] = null;
	}



	private class Element {
		//KEY = key1 + key2 + index = (16+32+16) bit = 64bit

		private short key1;
		private int key2;
		private PnNode node;
		private short depth;	// tree depth of endgame
		protected Element next;

		private static final int TABLE_SIZE = 16;
		private static final int MASK2_BITS = TABLE_SIZE + Integer.SIZE;
		private static final int MASK1 = 65535;		//2**16-1 = 16 ones

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
		protected void addNext(Element e) {
			if(next == null) next = e;
			else next.addNext(e);
		}

		/**
		 * Complexity: O(1)
		 */
		protected void setNode(PnNode node) {
			this.node = node;
		}
		/**
		 * Complexity: O(1)
		 */
		protected void setDepth(short depth) {
			this.depth = depth;
		}

		/**
		 * Complexity: O(1)
		 * @param state_a
		 * @param state_d
		 * @param depth
		 */
		protected void set(PnNode node, short depth) {
			this.node	= node;
			this.depth	= depth;
		}

		/**
		 * Complexity: O(1)
		 * @return
		 */
		protected PnNode getNode() {
			return node;
		}
		/**
		 * Complexity: O(1)
		 * @return
		 */
		protected short getFinalDepth() {
			return depth;
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
			return (int)(key & MASK1);
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
