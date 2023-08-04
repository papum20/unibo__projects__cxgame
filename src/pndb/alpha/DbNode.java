package pndb.alpha;

import connectx.CXCell;
import pndb.constants.Auxiliary;
import pndb.constants.Constants.BoardsRelation;

/**
 * Node for DbSearch, including its own board.
 */
public class DbNode<B extends IBoardBitDb<B>> {

	public final B board;
	/* 
	 * (byte) max tier to find;
	 * (boolean) is_combination;
	 */
	private byte data;

	protected DbNode<B> first_child;
	protected DbNode<B> sibling;



	public DbNode() {
		board = null;
	}
	
	public DbNode(B board, boolean is_combination, int max_tier) {
		this.board = board;
		setData(is_combination, max_tier);
		this.first_child = null;
		this.sibling = null;
	}
	public DbNode<B> copy(B board, boolean is_combination, byte max_tier, boolean copy_threats) {
		return new DbNode<B>(board.getCopy(copy_threats), is_combination, max_tier);
	}



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


		/*
		 * helper, only one direction
		 */
		private BoardsRelation _validCombinationWith(DbNode<B> node, byte attacker) {

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

		/*
		 * Check if a combination with node is valid, i.e. if they're not in conflict and both have a marked cell the other doesn't.
		 */
		public BoardsRelation validCombinationWith(DbNode<B> node, byte attacker) {

			BoardsRelation this_valid = _validCombinationWith(node, attacker);

			if(this_valid == BoardsRelation.USEFUL)
				return node._validCombinationWith(this, attacker);
			else
				return this_valid;
		}

	//#endregion DB

	//#region GET_SET

	private void setData(final boolean is_combination, final int max_tier) {
		data = (byte)( (max_tier << 1) | (is_combination? 1:0) );
	}
	public int getMaxTier() {
		return (data >> 1);
	}
	public int isCombination() {
		return (data & 1);
	}
	public DbNode<B> getFirstChild() {return first_child; }
	public DbNode<B> getSibling() {return sibling; }

	public void addChild(DbNode<B> child) {
		if(first_child == null) first_child = child;
		else first_child.addSibling(child);
	}
	public void addSibling(DbNode<B> sibling) {
		if(sibling == this) return;
		else if(this.sibling == null) this.sibling = sibling;
		else this.sibling.addSibling(sibling);
	}

	//#endregion GET_SET
	
}
