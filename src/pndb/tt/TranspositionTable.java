package pndb.tt;

import java.util.Random;

import pndb.constants.GameState;





public class TranspositionTable {

	private long[][][] moves;		//random hashes defined for each move
	private Element[] table;		//actual array where entries are stored
	private int size;

	private static final int PLAYERS_N = 2;



	public TranspositionTable(int M, int N) {
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

	public long getHash(long hash, int i, int j, int k) {
		long move_hash = moves[i][j][k];
		return (hash ^ move_hash);
	}

	public void insert(long key) {
		Element e = new Element(key);
		int index = Element.index(key);
		if(table[index] == null)
			table[index] = e;
		else
			table[index].addNext(e);
	}
	/**
	 * 
	 * @param key
	 * @param state
	 * @param idx: 0=attacker, 1=defender, 2=both
	 */
	public void insert(long key, byte state, int idx) {
		Element e = new Element(key);
		if(idx == 0) e.setState(state, GameState.NULL);
		else if(idx == 1) e.setState(GameState.NULL, state);
		else if(idx == 2) e.setState(state, state);
		int index = Element.index(key);
		if(table[index] == null)
			table[index] = e;
		else
			table[index].addNext(e);
	}
	public void insert(long key, byte state, int idx, short depth) {
		Element e = new Element(key);
		if(idx == 0) e.setState(state, GameState.NULL, depth);
		else if(idx == 1) e.setState(GameState.NULL, state, depth);
		else if(idx == 2) e.setState(state, state, depth);
		int index = Element.index(key);
		if(table[index] == null)
			table[index] = e;
		else
			table[index].addNext(e);
	}

	/**
	 * Remove element entry.
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
	 * Remove state from element, or remove the element entry if both states remain null.
	 * @param key
	 * @param idx
	 */
	public void removeState(long key, int idx) {
		setState(key, GameState.NULL, idx);

		TranspositionElementEntry entry = getState(key);
		if(entry.state[0] == GameState.NULL && entry.state[1] == GameState.NULL)
			remove(key);
	}

	public Boolean exists(long key) {
		int index = Element.index(key);
		if(table[index] == null) return false;
		else {
			Element compare = new Element(key);
			return (table[index].getNext(compare) != null);
		}
	}
	public TranspositionElementEntry getState(long key) {
		int index = Element.index(key);
		if(table[index] == null) return null;
		else {
			Element compare = new Element(key);
			Element e = table[index].getNext(compare);
			if(e == null) return null;
			else return e.getState();
		}
	}
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
	 * 
	 * @param key
	 * @param state
	 * @param idx: 0=attacker, 1=defender, 2=both
	 */
	public void setState(long key, byte state, int idx) {
		int index = Element.index(key);
		if(table[index] != null) {
			Element compare = new Element(key);
			Element e = table[index].getNext(compare);
			if(idx == 0) e.setState(state, GameState.NULL);
			else if(idx == 1) e.setState(GameState.NULL, state);
			else if(idx == 2) e.setState(state, state);
		}
	}
	public void setState(long key, byte state, int idx, short depth) {
		int index = Element.index(key);
		if(table[index] != null) {
			Element compare = new Element(key);
			Element e = table[index].getNext(compare);
			if(idx == 0) e.setState(state, GameState.NULL, depth);
			else if(idx == 1) e.setState(GameState.NULL, state, depth);
			else if(idx == 2) e.setState(state, state, depth);
		}
	}

	/**
	 * Try to set state, or insert if doesn't exist.
	 * @param key
	 * @param state
	 * @param idx
	 */
	public void setStateOrInsert(long key, byte state, int idx) {
		if(exists(key))
			setState(key, state, idx);
		else
			insert(key, state, idx);
	}
	public void setStateOrInsert(long key, byte state, int idx, short depth) {
		if(exists(key))
			setState(key, state, idx, depth);
		else
			insert(key, state, idx, depth);
	}

	public void clear(long key) {
		int index = Element.index(key);
		table[index] = null;
	}



	private class Element {
		//KEY = key1 + key2 + index = (16+32+16) bit = 64bit

		private short key1;
		private int key2;
		private byte state;
		private short depth;	// tree depth of endgame
		protected Element next;

		private static final int TABLE_SIZE = 16;
		private static final int MASK2_BITS = TABLE_SIZE + Integer.SIZE;
		private static final int MASK1 = 65535;		//2**16-1 = 16 ones

		protected Element(long key) {
			key2 = (int)(key >> TABLE_SIZE);
			key1 = (short)(key >> MASK2_BITS);
			state = 6;	//OPEN
		}

		protected void addNext(Element e) {
			if(next == null) next = e;
			else next.addNext(e);
		}

		protected void setState(byte state_a, byte state_d) {
			if(state_a == GameState.NULL) state_a = TranspositionElementEntry.ELEMENT_ENTRIES[state].state[0];
			if(state_d == GameState.NULL) state_d = TranspositionElementEntry.ELEMENT_ENTRIES[state].state[1];
			state = 0;
			if(state_a == GameState.OPEN) state += 5;
			else if(state_a == GameState.DRAW) state += 10;
			else if(state_a == GameState.WINP1) state += 15;
			else if(state_a == GameState.WINP2) state += 20;
			if(state_d == GameState.OPEN) state += 1;
			else if(state_d == GameState.DRAW) state += 2;
			else if(state_d == GameState.WINP1) state += 3;
			else if(state_d == GameState.WINP2) state += 4;
		}

		protected void setState(byte state_a, byte state_d, short depth) {
			setState(state_a, state_d);
			this.depth = depth;
		}

		protected TranspositionElementEntry getState() {
			return TranspositionElementEntry.ELEMENT_ENTRIES[state];
		}
		protected short getFinalDepth() {
			return depth;
		}

		//returns the element if cmp==this or a next element in the list (assuming the index is the same)
		protected Element getNext(Element cmp) {
			if (equals(cmp)) return this;
			else if(next == null) return null;
			else return next.getNext(cmp);
		}

		protected static int tableSize() {
			return TABLE_SIZE;
		}

		protected static int index(long key) {
			return (int)(key & MASK1);
		}

		protected boolean equals(Element cmp) {
			return (cmp.key1 == key1) && (cmp.key2 == key2);
		}
	}

}
