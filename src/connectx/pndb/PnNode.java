package connectx.pndb;

import java.util.ArrayList;



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
	protected final short[] n;				// n_proof, n_disproof
	
	public final PnNode parent;
	public PnNode[] children;
	public PnNode most_proving;
	

	/**
	 * 
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

		public Value value() {
			if(n[PROOF] == N_ZERO && n[DISPROOF] == N_INFINITE)			return Value.TRUE;
			else if(n[PROOF] == N_INFINITE && n[DISPROOF] == N_ZERO)	return Value.FALSE;
			else return Value.UNKNOWN;
		}
		/**
		 * find and return the child with min proof/disproof number
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
		 * sum all proof/disproof number of children
		 * @param ind : index of n[] = PROOF | DISPROOF
		 * @return the sum
		 */
		public short sumChildren(byte ind) {
			short sum = 0;
			for (PnNode child : children)
				sum += child.n[ind];
			return sum;
		}

		public PnNode findChild(byte ind, byte n) {
			for(PnNode child : children)
				if(child.n[ind] == n) return child;
			return null;
		}

	//#endregion GET


	//#region SET
		/**
		 * 
		 * @param p
		 * @param d
		 */
		public void setProofAndDisproof(short p, short d) {
			n[PROOF] = p;
			n[DISPROOF] = d;
		}
		/**
		 * 
		 * @param children_n
		 */
		public void generateAllChildren(ArrayList<Integer> free_cols) {
			for(int i = 0; i < free_cols.size(); i++)
				children[i] = new PnNode(free_cols.get(i), this);
		}

		/**
		 * expand node,
		 * i.e. create array of children (uninitialized!)
		 * @param children_n : number of children
		 */
		public void expand(int children_n) {
			children = new PnNode[children_n];
		}
		/**
		 * prove node, i.e. assign value
		 * @param value : value to assing (True/False for binary trees)
		 */
		public void prove(boolean value) {
			if(value) setProofAndDisproof(N_ZERO, N_INFINITE);
			else setProofAndDisproof(N_INFINITE, N_ZERO);
			children = null;
		}

	//#endregion SET

	//#region BOOL
		public boolean isExpanded() {
			return children != null;
		}
		public boolean isProved() {
			return n[PROOF] == N_ZERO || n[DISPROOF] == N_ZERO;
		}
	//#endregion BOOL
	
	
}