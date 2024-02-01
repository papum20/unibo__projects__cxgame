package pndbtime;

import pndbtime.constants.Auxiliary;
import pndbtime.tt.TTElementProved;
import pndbtime.tt.TranspositionTable;



/**
 * Complexities use N both for M and N, so imagine N is the max/avg.
 */
public class BoardBitPn extends BoardBit {
	
	public static TranspositionTable<TTPnNode, TTPnNode.KeyDepth> TTdag;
	public static TranspositionTable<TTElementProved, TTElementProved.KeyDepth> TTproved;
	public long hash;
	private static TTElementProved.KeyDepth	key_proved	= new TTElementProved.KeyDepth();
	private static TTPnNode.KeyDepth key_dag			= new TTPnNode.KeyDepth();

	public byte player;	// player to move next


	/**
	 *	<p>	Complexity: O(3N)
	 *	<p>	-	O(5N), if M > 64
	 * @param M
	 * @param N
	 * @param X
	 * @param player CellState
	 */
	public BoardBitPn(int player) {
		super();
		hash = 0;
		this.player	= (byte)player;
	}

	/**
	 *	<p>	Complexity: O(3N)
	 *	<p>	-	O(5N), if M > 64
	 * @param B
	 */
	public BoardBitPn(BoardBitPn B) {
		copy(B);
	}

	/**
	 *	<p>	Complexity: O(3N)
	 *	<p>	-	O(5N), if M > 64
	 * @param B
	 */
	public void copy(BoardBitPn B) {
		super.copy(B);
		hash	= B.hash;
		player	= B.player;
	}

	/**
	 * Complexity: O(1)
	 * @param col
	 * @return GameState
	 */
	public void mark(int col) {
		hash	= TranspositionTable.getHash(hash, free[col], col, Auxiliary.getPlayerBit(player));
		super.mark(col, player);
		player = Auxiliary.opponent(player);
	}

	/**
	 *	<p>	Complexity (worst):	O(4X)
	 *	<p>	Complexity (best):	O(1)
	 * @param col
	 * @param attacker
	 * @return GameState
	 */
	public byte markCheck(int col) {
		mark(col);
		return check(free[col] - 1, col, player);
	}

	/**
	 * Complexity: O(1)
	 */
	@Override
	public void unmark(int col) {
		hash	= TranspositionTable.getHash(hash, free[col] - 1, col, _cellState(free[col] - 1, col));
		super.unmark(col);
		player = Auxiliary.opponent(player);
	}


	//#region TT

		/**
		 * <p>	Get entry from TTdag, for child obtained with move at `col`.
		 * <p>	Complexity: O(1 + alpha)
		 * @param hash node's hash
		 * @param col set to col for node's child if want entry for child, else TTElementProved.COL_NULL
		 * @param depth node's depth
		 * @param attacker current player at node (in case, who's making move col)
		 * @return entry
		 */
		public TTPnNode getEntry(int col, int depth) {
			return (col == TTElementProved.COL_NULL) ? 
			TTdag.get(TTPnNode.setKey(key_dag, hash, depth))
			: TTdag.get(TTPnNode.setKey(key_dag, TranspositionTable.getHash(hash, free[col], col, Auxiliary.getPlayerBit(player)), depth + 1));
		}

		/**
		 * <p>	Get entry from TTdag, for parent obtained undoing move at `col`.
		 * <p>	Complexity: O(1 + alpha)
		 * @param col
		 * @param depth
		 * @return
		 */
		public TTPnNode getEntryParent(int col, int depth) {
			return TTdag.get(TTPnNode.setKey(key_dag, TranspositionTable.getHash(hash, free[col] - 1, col, Auxiliary.getPlayerBit(Auxiliary.opponent(player))), depth - 1));
		}

		/**
		 * <p>	Add node to TTdag.
		 * <p>	Complexity: O(1)
		 * @param node
		 */
		public void addEntry(TTPnNode node) {
			TTdag.insert(hash, node);
			TTdag.count++;
		}

		/**
		 * <p>	Remove node from TTdag.
		 * <p>	Complexity: O(1 + alpha)
		 */
		public void removeEntry(int depth) {
			TTdag.remove(TTPnNode.setKey(key_dag, hash, depth));
			TTdag.count--;
		}

		/**
		 * <p>	Get entry from TTproved, for child obtained with move at `col`.
		 * <p>	Complexity: O(1 + alpha)
		 * @param col
		 * @param depth
		 * @return
		 */
		public TTElementProved getEntryProved(int col, int depth) {
			return (col == TTElementProved.COL_NULL) ? 
			TTproved.get(TTElementProved.setKey(key_proved, hash, depth))
			: TTproved.get(TTElementProved.setKey(key_proved, TranspositionTable.getHash(hash, free[col], col, Auxiliary.getPlayerBit(player)), depth + 1));
		}

		/**
		 * <p>	Add node to TTproved.
		 * <p>	Complexity: O(1)
		 * @param node
		 */
		public void addEntryProved(TTElementProved node) {
			TTproved.insert(hash, node);
			TTproved.count++;
		}
	
	//#endregion TT

	
}