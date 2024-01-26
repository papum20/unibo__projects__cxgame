package pndbg.alpha;


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

	/**
	 * Complexity:
	 * 		with mc: O(3M + 10N + B.marked_threats.length + MN) = O(B.marked_threats.length + N**2 + 13N)
	 * 		no mc: O(3M + 10N + B.marked_threats.length) = O(B.marked_threats.length + 13N)
	 */
	public DbNode<BB, B> copy(B board, boolean is_combination, byte max_tier, boolean copy_threats) {
		return new DbNode<BB, B>(board.getCopy(copy_threats), is_combination, max_tier);
	}

	
}
