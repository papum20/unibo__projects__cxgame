package pndb.dnull.tt;

import java.util.Random;
import pndb.dnull.tt.TranspositionTable.Element.Key;



public class TranspositionTable<E extends TranspositionTable.Element<E,K>, K extends Key> {

	public static long[][][] moves = null;	//random hashes defined for each move
	private E[] table;						//actual array where entries are stored

	private static final int PLAYERS_N = 2;

	public int count;	// debug



	/**
	 * Complexity: O(M*N*PLAYERS_N + size + M*N*PLAYERS_N) = O(2MN + 2**16 + 2MN) = O(4MN + 2**16)
	 * @param M
	 * @param N
	 */
	public TranspositionTable(E[] table) {

		this.table = table;
		count = 0;
	}
	
	public static void initMovesHashes(int M, int N) {
		
		Random random = new Random();
		moves = new long[M][N][PLAYERS_N];
		
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
	public static long getHash(long hash, int i, int j, int k) {
		long move_hash = moves[i][j][k];
		return (hash ^ move_hash);
	}

	/**
	 * Complexity: O(n), with n length of the list
	 * @param key
	 */
	public void insert(long key, E e) {
		int index = e.calculateIndex(key);
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
	  public void remove(K k) {
		E e = table[k.index];
		if(e != null && e.listRemove(k) == e)
			table[k.index] = e.next;
	}

	/**
	 * Complexity: O(n), with n length of the list
	 * @param key
	 * @return the element if found, else null
	 */
	public E get(K k) {
		if(table[k.index] == null)
			return null;
		else
			return table[k.index].listGet(k);
	}



	/**
	 * @param S self
	 */
	public static abstract class Element<S extends Element<S, K>, K extends Key> {

		protected S next;


		public static class Key {
			protected long	key,
							key1,
							key2;
			int index;

			public Key() {
			
			}
			public Key(long key, long key1, long key2, int index) {
				this.key	= key;
				this.key1	= key1;
				this.key2	= key2;
				this.index	= index;
			}
			public void set(long key, long key1, long key2, int index) {
				this.key	= key;
				this.key1	= key1;
				this.key2	= key2;
				this.index	= index;
			}
		}


		public Element() {

		}

		/**
		 * Complexity: O(n), with n length of the list
		 * @param e
		 */
		protected abstract void listAppend(S e);
		protected abstract S listGet(K k);
		protected Element<S,K> listRemove(K k) {
			if(compareKey(k))
				return this;
			else if(next == null)
				return null;
			else {
				Element<S,K> to_remove = next.listRemove(k);
				if(to_remove == next)
					// remove
					next = to_remove.next;
				return to_remove;
			}
		}

		// public static abstract S[] getTable();
		// public static Key calculateKey(long key);
		// public static Key setKey(Key k, long key);
		public abstract int calculateIndex(long key);
		protected abstract boolean compareKey(K k);

	}
	

}