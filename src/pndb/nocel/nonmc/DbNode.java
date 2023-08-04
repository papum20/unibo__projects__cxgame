package pndb.nocel.nonmc;

import pndb.constants.Constants.BoardsRelation;



/**
 * Node for DbSearch, including its own board.
 */
public class DbNode<B extends BoardBitDb> {

	public final BoardBitDb board;
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
	
	public DbNode(BoardBitDb board, boolean is_combination, int max_tier) {
		this.board = board;
		setData(is_combination, max_tier);
		this.first_child = null;
		this.sibling = null;
	}
	public DbNode<B> copy(BoardBitDb board, boolean is_combination, byte max_tier, boolean copy_threats) {
		return new DbNode<B>(board.getCopy(copy_threats), is_combination, max_tier);
	}



	//#region DB

		/*
		 * Check if a combination with node is valid, i.e. if they're not in conflict and both have a marked cell the other doesn't.
		 */
		public BoardsRelation validCombinationWith(DbNode<B> node, byte attacker) {

			return board.validCombinationWith(node.board, attacker);
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
