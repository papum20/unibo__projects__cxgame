package pndb.delta;

import pndb.delta.constants.Constants;
import pndb.delta.constants.GameState;
import pndb.delta.TTPnNode.KeyDepth;
import pndb.delta.tt.TTElementProved;
import pndb.delta.tt.TranspositionTable.Element;
import pndb.delta.tt.TranspositionTable.Element.Key;



public class TTPnNode extends Element<TTPnNode, KeyDepth> {
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
		protected boolean compareKey(short key1, int key2, short depth) {
			return ((short)this.key1 == key1) && ((int)this.key2 == key2) && this.depth == depth;
		}
	}

	/*
	 * TT fields
	 */

	private		short key1;
	private		int key2;
	public		short depth;			// node depth, used "as key", to reduce ambiguity

	private static final int TABLE_SIZE = 22;
	private static final int MASK2_BITS = TABLE_SIZE + Integer.SIZE;
	private static final int MASK_IDX	= 4194303;		//2**22-1 = 22 ones

	/*
	 * PnNode fields
	 */

	//#region CONSTANTS
		public static final int N_ZERO		= 0;
		public static final int N_INFINITE	= Constants.INFINITE;

		public static final byte PROOF		= 0;	// proof index
		public static final byte DISPROOF	= 1;	// disproof index
	//#endregion CONSTANTS

	public static BoardBitPn board;

	public final int[] n;		// n_proof, n_disproof
	public short most_proving_col;

	private byte bits;			// implementative, accessed by functions
	private byte MASK_TAG		= 0x01;
	private byte MASK_EXPANDED	= 0x02;
	


	/**
	 * Complexity: O(1)
	 */
	public TTPnNode(long key, short depth, int tag) {
		key2 = (int)(key >> TABLE_SIZE);
		key1 = (short)(key >> MASK2_BITS);
		this.depth = (short)depth;

		this.n					= new int[2];
		this.most_proving_col	= -1;
		this.bits				= 0;
		setTag(tag);

		board.addEntry(this);
	}
	

	//#region TT

		/**
		 * Complexity: O(n), with n length of the list
		 * @param e
		 */
		protected void listAppend(TTPnNode e) {
			if(next == null) next = e;
			else next.listAppend(e);
		}
		/**
		 * Returns the element if cmp==this or a next element in the list (assuming the index is the same)
		 * Complexity: O(n), with n length of the list
		 * @param cmp
		 * @return
		 */
		protected TTPnNode listGet(KeyDepth k) {
			if (compareKey(k)) return this;
			else if(next == null) return null;
			else return next.listGet(k);
		}

		public static TTPnNode[] getTable() {
			return new TTPnNode[(int)(Math.pow(2, TABLE_SIZE))];
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
			return k.compareKey(key1, key2, depth);
		}

	//#endregion TT

	
	public TTPnNode createChild() {
		return new TTPnNode(board.hash, (short)(depth + 1), getTag());
	}
	
	/**
	 * Copy this tag to all descendants.
	 * Complexity: O(tree.size)
	 * 			= O(N**(M*N-d) )
	 */
	public void tagTree() {
		for(int j = 0; j < BoardBit.N; j++) {
			if(board.freeCol(j)) {
				TTPnNode child = getChild(j);

				if(child != null && child.getTag() != getTag()) {
					child.setTag(getTag());
					board.mark(j);
					child.tagTree();
					board.unmark(j);
				}
			}
		}
	}
	/**
	 * remove descendants from dag if have tag!=tag
	 * @param tag not to unmark
	 */
	public void removeUnmarkedTree(int tag) {
		if(getTag() == tag)
			return;
		else {
			board.removeEntry(depth);
			
			for(int j = 0; j < BoardBit.N; j++) {
				if(board.freeCol(j)) {
					TTPnNode child = getChild(j);
					if(child != null) {
						board.mark(j);
						child.removeUnmarkedTree(tag);
						board.unmark(j);
					}
				}
			}
		}
	}

	//#region GET

		public boolean hasParents() {
			for(int j = 0; j < BoardBit.N; j++) {
				if(board.free[j] > 0 && board.getEntryParent(j, depth) != null)
					return true;
			}
			return false;
		}
		public TTPnNode getChild(int col) {
			return (col == TTElementProved.COL_NULL) ? null : board.getEntry(col, depth);
		}
		/**
		 * if possible, get the best move, i.e. best proof/disproof ratio;
		 * else, get the move to the deepest sequence found.
		 * @return the column
		 */
		public int getMoveToBestChild() {

			int best_col = -1;
			if(n[DISPROOF] > 0) {	// if not disproved
				TTPnNode best = null, child;
				for(int j = 0; j < BoardBit.N; j++) {
					if(board.freeCol(j)) {
						child = getChild(j);
						if(child != null && (best == null || (float)child.n[PROOF] / child.n[DISPROOF] < (float)best.n[PROOF] / best.n[DISPROOF]) ) {
							best		= child;
							best_col	= j;
						}
					}
				}
			} else {	// if disproved
				TTElementProved best = null, child;
				for(int j = 0; j < BoardBit.N; j++) {
					if(board.freeCol(j)) {
						child = board.getEntryProved(j, depth);
						if(child != null && (best == null || child.depth_reachable > best.depth_reachable)) {
							best		= child;
							best_col	= j;
						}
					}
				}
			}
			return best_col;
		}

		/**
		 * return the tag, which is either 0 or 1.
		 */
		public int getTag() {
			return bits & MASK_TAG;
		}

	//#endregion GET


	//#region SET
		/**
		 * update proof numbers using children in dag, considering this node is minimizing n[idx].
		 * @param idx PROOF or DISPROOF
		 * @return entry if proved, else null
		 */
		public TTElementProved updateProofAndDisproof(int idx) {

			long disproof = 0;
			TTPnNode most_proving = null, child;
			TTElementProved entry;
			
			for(int j = 0; j < BoardBit.N; j++)
			{
				if(board.freeCol(j)) {
					child = getChild(j);
					entry = board.getEntryProved(j, depth);

					if(entry != null && entry.won() == (idx == PROOF))
						return prove(idx == PROOF);
					else if(child == null)
						continue;
					else if(most_proving == null || child.n[idx] < most_proving.n[idx]) {
						most_proving = child;
						most_proving_col = (short)j;
					}
					disproof = Math.min(disproof + (long)child.n[1 - idx], N_INFINITE);
				}
			}
			
			if(most_proving == null)	// disprove
				return prove(idx != PROOF);
			else {
				n[idx]		= most_proving.n[idx];
				n[1 - idx]	= (int)disproof;
				return null;
			}
		}
		
		/**
		 * Prove node, i.e. move from dag to proved tt.
		 * 
		 * Complexity: O(1)
		 * @param value value to assing (True/False for binary trees)
		 * @param depth_reachable best depth reachable (min if won, max if lost)
		 * @param col col to get to best position reachable
		 */
		public TTElementProved prove(boolean value, short depth_reachable, int col) {
			

			if(value) setProofAndDisproof(0, N_INFINITE);
			else setProofAndDisproof(N_INFINITE, 0);

			TTElementProved entry = new TTElementProved(board.hash, depth, depth_reachable, value, col);
			board.addEntryProved(entry);
			board.removeEntry(depth);
			return entry;
		}
		/**
		 * Prove node, i.e. move from dag to proved tt.
		 * 
		 * Complexity: O(1)
		 * @param value value to assing (True/False for binary trees)
		 */
		public TTElementProved prove(boolean value) {
			return prove(value, (board.game_state == GameState.DRAW) ? (short)(depth + 1) : depth, TTElementProved.COL_NULL);
		}

		public void setProofAndDisproof(int p, int d) {
			n[PROOF]	= p;
			n[DISPROOF]	= d;
		}
		/**
		 * identify this node es expanded, i.e. was developed.
		 */
		public void setExpanded() {
			bits |= MASK_EXPANDED;
		}
		/**
		 * tag is either 0 or 1.
		 */
		public void setTag(int val) {
			bits = (byte) ((bits & ~MASK_TAG) | (val & MASK_TAG));
		}

	//#endregion SET

	//#region BOOL
		/**
		 * Complexity: O(1)
		 */
		public boolean isExpanded() {
			return (bits & MASK_EXPANDED) != 0;
		}
		public boolean isProved() {
			return n[PROOF] == 0 || n[DISPROOF] == 0;
		}
		public boolean isWon() {
			return n[PROOF] == 0;
		}
		public boolean isLost() {
			return n[DISPROOF] == 0;
		}
	//#endregion BOOL
	
	//#region DEBUG
	
	public void debug(TTPnNode root) {
		System.out.println(debugString(root));
	}

	public String debugString(TTPnNode root) {
		String s = "node with col " + ", node==root? " + (this==root) + "; numbers: " + n[0] + ", " + n[1] + "\n";
		return s;
	}

	//#endregion DEBUG
	
}