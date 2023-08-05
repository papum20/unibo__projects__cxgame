package pndb.alpha;


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

	
}
