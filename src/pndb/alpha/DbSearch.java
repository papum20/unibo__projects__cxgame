package pndb.alpha;

import java.util.LinkedList;

import pndb.alpha.threats.ThreatApplied;
import pndb.alpha.threats.ThreatCells;
import pndb.alpha.threats.ThreatCells.USE;



public class DbSearch extends _DbSearch<DbSearchResult, BoardBit, BoardBitDb, DbNode<BoardBit, BoardBitDb>> {



	
	public DbSearch(_Operators operators) {
		super(new DbNode<BoardBit, BoardBitDb>(), operators);
	}


	/**
	 * Complexity: O(3M+7N+MN + 3(M+N) * AVG_THREATS_PER_DIR_PER_LINE)
	 * 		= O(6M + 10N + MN)
	 */
	public int[] getThreatCounts(BoardBit B, byte player) {

		board = new BoardBitDb(B, OPERATORS);
		return board.getThreatCounts(player);
	}
	
	//#region CREATE

		/**
		 * Complexity: O(1)
		 */
		@Override
		protected DbNode<BoardBit, BoardBitDb> createNode(BoardBitDb board, boolean is_combination, int max_tier) {
			return new DbNode<BoardBit, BoardBitDb>(board, is_combination, max_tier);
		}

		@Override
		protected BoardBitDb createBoardDb(int M, int N, int X) {
			return new BoardBitDb(M, N, X, OPERATORS);
		}

		@Override
		protected BoardBitDb createBoardDb(BoardBit BB) {
			return new BoardBitDb(BB, OPERATORS);
		}

		/**
		 * sets child's game_state if entry exists in TT.
		 * Complexity: 
		 * 		O(node.getDependant) + O(node.children_n)
		 * 		= O(64 + 4X + CheckAlignments) + O(node.applicable_threats_n)
		 * 		= O(64 + 4X + 18(2X+second-first)**2 ) + O(node.applicable_threats_n)
		 * 		= O(64 + 4X + 18(2X+X)**2 ) + O(node.applicable_threats_n)
		 * 		= O(64 + 4X + 27X**2 ) + O(node.applicable_threats_n)
		 * 		= O(27X**2 + 4X + 64) + O(node.applicable_threats_n)
		 * @param first
		 */
		protected DbNode<BoardBit, BoardBitDb> addDependentChild(DbNode<BoardBit, BoardBitDb> node, ThreatCells threat, int atk, LinkedList<DbNode<BoardBit, BoardBitDb>> lastDependency, byte attacker) {
			
			// debug
			log += "addDepChild\n";

			BoardBitDb new_board					= node.board.getDependant(threat, atk, USE.BTH, node.getMaxTier(), true);
			DbNode<BoardBit, BoardBitDb> newChild 	= new DbNode<BoardBit, BoardBitDb>(new_board, false, node.getMaxTier());

			node.addChild(newChild);
			lastDependency.add(newChild);

			return newChild;
		}

	//#endregion CREATE
	

	//#region HELPER
		
		/**
		 * Complexity: O(N)
		 */
		@Override
		protected DbSearchResult getReturnValue(byte player) {

			int		winning_col;
			int[]	related_squares_by_col;
			
			// the winning move is the player's move in the first threat in the sequence
			ThreatApplied winning_threat = win_node.board.markedThreats.getFirst();
			winning_col = winning_threat.threat.related[winning_threat.related_index].j;
			
			/* fill the related_squares_by_column with the number of newly made moves for each column
			*/
			related_squares_by_col = new int[N];
			for(int j = 0; j < N; j++)
				related_squares_by_col[j] = win_node.board.free[j] - board.free[j];
			
			return new DbSearchResult(winning_col, related_squares_by_col);
		}

	//#endregion HELPER

}
