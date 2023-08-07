package pndb.alpha;



/**
 * Node for PnSearch, with simple fields.
 */
public class PnNode {

	//#region CONSTANTS
	public static final short N_ZERO = 0;
	public static final short N_INFINITE = 32767;

	public static final byte PROOF = 0;		// proof index
	public static final byte DISPROOF = 1;	// disproof index
	//#endregion CONSTANTS

	public static enum Value {
		TRUE,
		FALSE,
		UNKNOWN
	}
	

	public final byte col;					// move (column)
	public final short[] n;				// n_proof, n_disproof
	
	public final PnNode parent;
	public PnNode[] children;
	/*
	 * note: using pointers to child/sibling (maybe child=most proving) would reduce memory
	 */
	public PnNode most_proving;
	

	/**
	 * Complexity: O(1)
	 * @param col : column
	 * @param n : proof and disproof
	 */
	public PnNode(int col, PnNode parent) {
		this.col			= (byte)col;
		this.n				= new short[2];
		this.parent			= parent;
		this.children		= null;
		this.most_proving	= null;
	}


	
	

	//#region GET

		/**
		 * Complexity: O(1)
		 * @return
		 */
		public Value value() {
			if(n[PROOF] == N_ZERO && n[DISPROOF] == N_INFINITE)			return Value.TRUE;
			else if(n[PROOF] == N_INFINITE && n[DISPROOF] == N_ZERO)	return Value.FALSE;
			else return Value.UNKNOWN;
		}
		/**
		 * find and return the child with min proof/disproof number.
		 * Complexity: O(children_n)
		 * 	-	worst: O(N)
		 * @param ind : index of n[], = PROOF | DISPROOF
		 * @return child with min n[ind]
		 */
		public PnNode minChild(byte ind) {
			PnNode mn = children[0];
			for (PnNode child : children)
				if (child.n[ind] < mn.n[ind]) mn = child;
			return mn;
		}
		/**
		 * sum all proof/disproof number of children.
		 * Complexity: O(children_n)
		 * 	-	worst: O(N)
		 * @param ind : index of n[] = PROOF | DISPROOF
		 * @return the sum
		 */
		public short sumChildren(byte ind) {
			short sum = 0;
			for (PnNode child : children)
				sum = (short)Math.min(sum + child.n[ind], N_INFINITE);
			return sum;
		}

		/**
		 * Complexity: O(children_n)
		 * 	-	worst: O(N)
		 * @param ind
		 * @param n
		 * @return
		 */
		public PnNode findChild(byte ind, byte n) {
			for(PnNode child : children)
				if(child.n[ind] == n) return child;
			return null;
		}

	//#endregion GET


	//#region SET
		/**
		 * Complexity: O(1)
		 * @param p
		 * @param d
		 */
		public void setProofAndDisproof(short p, short d) {
			n[PROOF]	= p;
			n[DISPROOF]	= d;
		}
		/**
		 * 
		 * @param children_n
		 */
		//public void generateAllChildren(ArrayList<Integer> free_cols) {
		//	for(int i = 0; i < free_cols.size(); i++)
		//		children[i] = new PnNode(free_cols.get(i), this);
		//}

		/**
		 * expand node,
		 * i.e. create array of children (uninitialized).
		 * Complexity: O(children_n)
		 * 	-	worst: O(N)
		 * @param children_n : number of children
		 */
		public void expand(int children_n) {
			children = new PnNode[children_n];
		}
		/**
		 * prove node, i.e. assign value.
		 * Complexity: O(1)
		 * @param value value to assing (True/False for binary trees)
		 * @param prune if true, prune children (i.e. set to null)
		 */
		public void prove(boolean value, boolean prune) {
			if(value) setProofAndDisproof(N_ZERO, N_INFINITE);
			else setProofAndDisproof(N_INFINITE, N_ZERO);
			if(prune) children = null;
		}

	//#endregion SET

	//#region BOOL
		/**
		 * Complexity: O(1)
		 */
		public boolean isExpanded() {
			return children != null;
		}
		/**
		 * Complexity: O(1)
		 */
		public boolean isProved() {
			return n[PROOF] == N_INFINITE || n[DISPROOF] == N_INFINITE;
		}
	//#endregion BOOL
	
	//#region DEBUG
	
	public void debug(PnNode root) {
		String s = "node with col " + col + ", node==root? " + (this==root) + "; numbers: " + n[0] + ", " + n[1] + "\n";
		s += "children\n";
		for(PnNode child : children)
			s += child.col + ":" + child.n[PROOF] + "," + child.n[DISPROOF] + "\n";
		System.out.println(s);
	}

	//#endregion DEBUG
	
}