package pndb.nocel.nonmc;

import pndb.alpha.IBoardBitDb;
import pndb.alpha._DbNode;
import pndb.constants.Constants.BoardsRelation;



/**
 * Node for DbSearch, including its own board.
 */
public class DbNode<B extends IBoardBitDb<B>> extends _DbNode<DbNode<B>, B> {

	
	public DbNode() {
		super();
	}
	
	public DbNode(B B, boolean is_combination, int max_tier) {
		super(B, is_combination, max_tier);
	}

	public DbNode<B> copy(B board, boolean is_combination, byte max_tier, boolean copy_threats) {
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

}
