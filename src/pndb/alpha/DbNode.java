package pndb.alpha;


/**
 * Node for DbSearch, including its own board.
 */
public class DbNode<BB extends IBoardBit, B extends IBoardBitDb<B, BB>> extends _DbNode<DbNode<BB, B>, BB, B> {


	public DbNode() {
		super();
	}
	
	/**
	 * Complexity: O(1)
	 * @param B
	 * @param is_combination
	 * @param max_tier
	 */
	public DbNode(B B, boolean is_combination, int max_tier) {
		super(B, is_combination, max_tier);
	}

	public DbNode<BB, B> copy(B board, boolean is_combination, byte max_tier, boolean copy_threats) {
		return new DbNode<BB, B>(board.getCopy(copy_threats), is_combination, max_tier);
	}

	
}
