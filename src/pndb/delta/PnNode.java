package pndb.delta;

import java.util.LinkedList;

/**
 * Node for PnSearch, with simple fields.
 */
public class PnNode {

	//#region CONSTANTS
	public static final short N_ZERO		= 0;
	public static final short N_INFINITE	= 32767;

	public static final byte PROOF		= 0;	// proof index
	public static final byte DISPROOF	= 1;	// disproof index
	//#endregion CONSTANTS

	public static enum Value {
		TRUE,
		FALSE,
		UNKNOWN
	}
	

	public byte[] cols;			// move (column), for each child
	public final short[] n;		// n_proof, n_disproof
	
	public final LinkedList<PnNode> parents;
	public PnNode[] children;
	/*
	 * note: using pointers to child/sibling (maybe child=most proving) would reduce memory
	 */
	public PnNode most_proving;

	public short depth;
	

	/**
	 * Complexity: O(1)
	 * @param cols column
	 */
	public PnNode() {
		this.n				= new short[2];
		this.parents		= new LinkedList<PnNode>();
		this.children		= null;
		this.most_proving	= null;
	}
	/**
	 * Complexity: O(1)
	 * @param cols column
	 * @param parent != null
	 */
	public PnNode(PnNode parent, short depth) {
		this.n				= new short[2];
		this.parents		= new LinkedList<PnNode>();
		this.parents.add(parent);
		this.children		= null;
		this.most_proving	= null;
		this.depth			= depth;
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
		 * Complexity: O(1)
		 * @param ind
		 * @return
		 */
		public void addChild(int idx, PnNode child, int col) {
			children[idx]	= child;
			cols[idx]		= (byte)col;
			// add first, to keep the same order when traversing the tree
			child.parents.addFirst(this);
		}
		/**
		 * Like addChild, also create the child.
		 * <p> Complexity: O(1)
		 * @param ind
		 * @return
		 */
		public PnNode createChild(int idx, int col) {
			children[idx]	= new PnNode(this, (short)(depth + 1));
			cols[idx]		= (byte)col;
			return children[idx];
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

		/**
		 * Get the last col marked from th first parent to this node.
		 * <p>	Complexity: O(N)
		 * @return
		 */
		public byte lastMoveFromFirstParent() {
			int i = 0;
			PnNode parent = parents.getFirst();
			for(PnNode child : parent.children) {
				if(child == this) return parent.cols[i];
				else i++;
			}
			return -1;
		}
		/**
		 * Get the col to mark to get from this node to the specified child.
		 * <p>	Complexity: O(N)
		 * @param child
		 * @return
		 */
		public byte lastMoveForChild(PnNode child) {
			for(int i = 0; i < children.length; i++) {
				if(children[i] == child) return cols[i];
			}
			return -1;
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
			children	= new PnNode[children_n];
			cols		= new byte[children_n];
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
		/**
		 * Complexity: O(1)
		 */
		public boolean isWon() {
			return n[PROOF] == 0;
		}
		/**
		 * Complexity: O(1)
		 */
		public boolean isLost() {
			return n[DISPROOF] == 0;
		}
	//#endregion BOOL
	
	//#region DEBUG
	
	public void debug(PnNode root) {
		System.out.println(debugString(root));
	}

	public String debugString(PnNode root) {
		String s = "node with col " + ((this == root) ? -1 : lastMoveFromFirstParent()) + ", node==root? " + (this==root) + "; numbers: " + n[0] + ", " + n[1] + "\n";
		s += "children\n";
		for(PnNode child : children)
			s += lastMoveForChild(child) + ":" + child.n[PROOF] + "," + child.n[DISPROOF] + "\n";
		return s;
	}

	//#endregion DEBUG
	
}