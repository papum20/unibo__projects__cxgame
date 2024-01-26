package pndbg.alpha;

import connectx.CXCell;
import pndbg.constants.Auxiliary;
import pndbg.constants.Constants.BoardsRelation;

/**
 * Node for DbSearch, including its own board.
 */
public abstract class _DbNode<S extends _DbNode<S,BB,B>, BB extends IBoardBit, B extends IBoardBitDb<B, BB>> {

	public final B board;
	/* 
	 * (byte) max tier to find;
	 * (boolean) is_combination;
	 */
	private byte data;

	protected S first_child;
	protected S sibling;



	public _DbNode() {
		board = null;
	}
	
	/**
	 * Complexity: O(1)
	 */
	public _DbNode(B board, boolean is_combination, int max_tier) {
		this.board = board;
		setData(is_combination, max_tier);
		this.first_child = null;
		this.sibling = null;
	}

	/**
	 * Complexity: O(B.getCopy) = O(marked_threats.length + N**2 + 13N)
	 * @param board
	 * @param is_combination
	 * @param max_tier
	 * @param copy_threats
	 * @return
	 */
	public abstract S copy(B board, boolean is_combination, byte max_tier, boolean copy_threats);



	//#region DB

			/*public boolean equals(NodeBoard node) {
			return board.equals(node.board);
		}*/
		//check the two boards, return true if the same cell is occupied by a player in one board, the other player in the other board
		//public boolean inConflict(NodeBoard node) {
		//	for(int i = 0; i < board.MC_n; i++) {
		//		MNKCell cell = board.getMarkedCell(i);
		//		if(cell.state != node.board.cellState(cell.i, cell.j) && node.board.cellState(cell.i, cell.j) != MNKCellState.FREE)
		//			return true;
		//	}
		//	return false;
		//}


		/**
		 * helper, only one direction.
		 * Complexity: worst: O(mc_n)
		 */
		protected BoardsRelation _validCombinationWith(_DbNode<S,BB,B> node, byte attacker) {

			boolean added_own = false;
			CXCell cell;

			for(int i = 0; i < board.getMC_n(); i++) {
				cell = board.getMarkedCell(i);
				if(cell.state != node.board.cellStateCX(cell.i, cell.j)) {
					if(!node.board.cellFree(cell.i, cell.j))
						return BoardsRelation.CONFLICT;			//conflict: two different marks on same cell
					else if(Auxiliary.CX2cellState(cell.state) == attacker)
						added_own = true;
				}
			}

			return added_own? BoardsRelation.USEFUL : BoardsRelation.USELESS;
		}

		/**
		 * Check if a combination with node is valid, i.e. if they're not in conflict and both have a marked cell the other doesn't.
		 * Complexity:
		 * 		with mc - worst (both not conflict): O(this.mc_n + node.mc_n)
		 * 		no mc - worst: O(N)
		 */
		public BoardsRelation validCombinationWith(S node, byte attacker) {

			BoardsRelation this_valid = _validCombinationWith(node, attacker);

			if(this_valid == BoardsRelation.USEFUL)
				return node._validCombinationWith(this, attacker);
			else
				return this_valid;
		}

	//#endregion DB

	//#region GET_SET

	protected void setData(final boolean is_combination, final int max_tier) {
		data = (byte)( (max_tier << 1) | (is_combination? 1:0) );
	}
	public int getMaxTier() {
		return (data >> 1);
	}
	public int isCombination() {
		return (data & 1);
	}
	public S getFirstChild() {return first_child; }
	public S getSibling() {return sibling; }

	/**
	 * Complexity: O(children_n)
	 * @param child
	 */
	public void addChild(S child) {
		if(first_child == null) first_child = child;
		else first_child.addSibling(child);
	}
	public void addSibling(S sibling) {
		if(sibling == this) return;
		else if(this.sibling == null) this.sibling = sibling;
		else this.sibling.addSibling(sibling);
	}

	//#endregion GET_SET
	
}
