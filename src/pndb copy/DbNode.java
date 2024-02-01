package pndb;

import pndb.constants.Constants.BoardsRelation;

/**
 * Node for DbSearch, including its own board.
 */
public class DbNode {

	public final BoardBitDb board;
	/* 
	 * (byte) max tier to find;
	 * (boolean) is_combination;
	 */
	private byte data;

	protected DbNode first_child;
	protected DbNode sibling;



	/**
	 * Complexity: O(1)
	 */
	public DbNode() {
		board = null;
	}
	
	/**
	 * Complexity: O(1)
	 */
	public DbNode(BoardBitDb board, boolean is_combination, int max_tier) {
		this.board = board;
		setData(is_combination, max_tier);
		this.first_child = null;
		this.sibling = null;
	}

	/**
	 * <p>	Complexity: O(6N)
	 * <p>	-	O(10N), if M > 64
	 */
	public static DbNode copy(BoardBitDb board, boolean is_combination, byte max_tier, boolean copy_threats) {
		return new DbNode(board.getCopy(copy_threats), is_combination, max_tier);
	}


	//#region DB

		/*
		 * <p>	Check if a combination with node is valid, i.e. if they're not in conflict and both have a marked cell the other doesn't.
		 * <p>	Complexity: O(N)
		 * <p>	-	O(2N), if M > 64
		 */
		public BoardsRelation validCombinationWith(DbNode node) {

			return board.validCombinationWith(node.board);
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
	public DbNode getFirstChild() {return first_child; }
	public DbNode getSibling() {return sibling; }

	/**
	 * Complexity: O(children_n),		ususally only a few
	 * @param child
	 */
	public void addChild(DbNode child) {
		if(first_child == null) first_child = child;
		else first_child.addSibling(child);
	}

	public void addSibling(DbNode sibling) {
		if(sibling == this) return;
		else if(this.sibling == null) this.sibling = sibling;
		else this.sibling.addSibling(sibling);
	}

	//#endregion GET_SET
	
}
