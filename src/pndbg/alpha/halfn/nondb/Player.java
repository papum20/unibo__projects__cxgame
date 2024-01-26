package pndbg.alpha.halfn.nondb;

import static pndbg.constants.Constants.SHORT_0;

import pndbg.alpha.PnNode;



/**
 * no db.
 */
public class Player extends pndbg.alpha.halfn.Player {

	@Override
	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {

		super.initPlayer(M, N, X, first, timeout_in_secs);
	}

	@Override
	public String playerName() {
		return "pndb nondb";
	}


	//#region PN_SEARCH

		/**
		 * Complexity: O(1)
		 */
		@Override
		protected boolean evaluateDb(PnNode node, byte player) {

			log += "evaluateDb\n";
			return false;
		}

		/**
		 * Complexity: O(DbSearch + getThreatCounts + children_generation + loop + sorting )
		 *		= O(DbSearch + O(6M + 10N + MN) + N + N(4X) + N**2 ) = O(DbSearch + MN + N**2 + 6M + 11N + 4XN )
		 		= O(DbSearch + 2N**2 + (17+4X)N ) if M similar to N
				note: setProofAndDisproofNumbers can't go in expanded case from here, so it's O(1)
		 * @param node
		 * @param threat_scores_by_col
		 */
		@Override
		public void generateAllChildren(PnNode node, byte player) {

			// debug
			log += "generateChildren\n";

			int		related_cols_n	= 0;
			int[]	related_cols;
			int current_child, j;

			related_cols = new int[board.N];
			for(j = 0; j < board.N; j++)
				if(board.freeCol(j)) related_cols[j] = 1;
			// count the columns, i.e. the number of new children
			
			for(int moves_n : related_cols)
				if(moves_n > 0) related_cols_n++;

			node.expand(related_cols_n);
			current_child = 0;

			// fill children with such columns, and sort by threat_scores at the same time
			for(j = 0; j < board.N && current_child < related_cols_n; j++) {
				if(related_cols[j] > 0) {
					// then insert
					node.children[current_child++] = new PnNode(j, node);

					// set proof numbers
					/* Heuristic: nodes without any threat should be considered less (or not at all).
					 */
					mark(j);
					setProofAndDisproofNumbers(node.children[current_child - 1], isMyTurn(), SHORT_0);
					unmark(j);
				}
			}

		}

		/**
		 * Init proof numbers to offset + current level in game tree.
		 * @param node
		 */
		@Override
		protected void initProofAndDisproofNumbers(PnNode node, short offset) {
			short number = (short)(offset + current_level / 2 + 1);		// +1 so never < 1
			node.setProofAndDisproof(number, number);
		}

	//#endregion PN_SEARCH

}