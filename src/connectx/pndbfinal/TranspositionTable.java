package connectx.pndbfinal;

import java.util.Random;

import connectx.CXGameState;





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
	public void insert(long key, CXGameState state, int idx) {
		Element e = new Element(key);
		if(idx == 0) e.setState(state, null);
		else if(idx == 1) e.setState(null, state);
		else if(idx == 2) e.setState(state, state);
		int index = Element.index(key);
		if(table[index] == null)
			table[index] = e;
		else
			table[index].addNext(e);
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
	/**
	 * 
	 * @param key
	 * @param state
	 * @param idx: 0=attacker, 1=defender, 2=both
	 */
	public void setState(long key, CXGameState state, int idx) {
		int index = Element.index(key);
		if(table[index] != null) {
			Element compare = new Element(key);
			Element e = table[index].getNext(compare);
			if(idx == 0) e.setState(state, null);
			else if(idx == 1) e.setState(null, state);
			else if(idx == 2) e.setState(state, state);
		}
	}

	/**
	 * Try to set state, or insert if doesn't exist.
	 * @param key
	 * @param state
	 * @param idx
	 */
	public void setStateOrInsert(long key, CXGameState state, int idx) {
		if(exists(key))
			setState(key, state, idx);
		else
			insert(key, state, idx);
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
		protected Element next;

		private static final int TABLE_SIZE = 16;
		private static final int MASK2_BITS = TABLE_SIZE + Integer.SIZE;
		private static final int MASK1 = 65535;		//2**16-1 = 16 ones

		protected Element(long key) {
			key2 = (int)(key >> TABLE_SIZE);
			key1 = (short)(key >> MASK2_BITS);
			state = 0;	//OPEN
		}
		protected void addNext(Element e) {
			if(next == null) next = e;
			else next.addNext(e);
		}
		protected void setState(CXGameState state_a, CXGameState state_d) {
			if(state_a == null) state_a = TranspositionElementEntry.ELEMENT_ENTRIES[state].state[0];
			if(state_d == null) state_d = TranspositionElementEntry.ELEMENT_ENTRIES[state].state[1];
			state = 0;
			if(state_a == CXGameState.OPEN) state += 5;
			else if(state_a == CXGameState.DRAW) state += 10;
			else if(state_a == CXGameState.WINP1) state += 15;
			else if(state_a == CXGameState.WINP2) state += 20;
			if(state_d == CXGameState.OPEN) state += 1;
			else if(state_d == CXGameState.DRAW) state += 2;
			else if(state_d == CXGameState.WINP1) state += 3;
			else if(state_d == CXGameState.WINP2) state += 4;
		}
		protected TranspositionElementEntry getState() {
			return TranspositionElementEntry.ELEMENT_ENTRIES[state];
		}
		//returns the element if cmp==this or a next element in the list (assuming the index is the same)
		protected Element getNext(Element cmp) {
			if ((cmp.key1 == key1) && (cmp.key2 == key2)) return this;
			else if(next == null) return null;
			else return next.getNext(cmp);
		}
		protected static int tableSize() {
			return TABLE_SIZE;
		}
		protected static int index(long key) {
			return (int)(key & MASK1);
		}
	}

}
