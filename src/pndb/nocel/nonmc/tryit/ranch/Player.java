package pndb.nocel.nonmc.tryit.ranch;

import pndb.alpha.PnNode;
import pndb.constants.Auxiliary;
import pndb.nocel.nonmc.tryit.DbSearchResult;




/**
 * Randomly shuffles children with same priority, so they are analyzed in a different order.
 * Also uses `halfn` heuristic.
 */
public class Player extends pndb.nocel.nonmc.tryit.Player {



	@Override
	public String playerName() {
		return "pndb ranch";
	}


	//#region PN_SEARCH

		/**
		 * 
		 * @param node
		 * @param threat_scores_by_col
		 */
		@Override
		public void generateAllChildren(PnNode node, byte player) {

			// debug
			log += "generateChildren\n";

			/* Heuristic: implicit threat.
			 * Only inspect moves in an implicit threat, i.e. a sequence by which the opponent could win
			 * if the current player was to make a "null move".
			 * In fact, the opponent could apply such winning sequence, if the current player was to 
			 * make a move outside it, thus not changing his plans.
			 * Applied to CXGame: columns where the opponent has an immediate attacking move - which leads to a win for him -,
			 * i.e. where the attacker's move corresponds to the first free cell in the column, are for sure
			 * the most interesting (in fact, you would lose not facing them); however, other columns involved in the sequence are
			 * not ignored, since they could block the win too, and also to simplify the calculations by approximation.
			 */
			/* note: related_cols should already contain only available, not full, columns.
			 */

			DbSearchResult res_db = dbSearch.selectColumn(board, node, timer_end - System.currentTimeMillis(), Auxiliary.opponent(player));

			/* Heuristic: sorting moves (previously selected from iterated related squares) by number/scores of own threats in them
			 * (i.e., for columns, the sum of the scores in the whole column).
			 */

			int		related_cols_n	= 0;
			int[]	related_cols,
					threats			= dbSearch.getThreatCounts(board, player);
			int current_child, j, k;

			/* Heuristic: nodes without any implicit threat should be considered less (or not at all),
			 * especially after a few moves (as, probably, after a few moves it's "guaranteed" there are always some).
			 * For now, not considered.
			 */
			if(res_db != null)
				related_cols = res_db.related_squares_by_col;
			else {
				related_cols = new int[board.N];
				for(j = 0; j < board.N; j++)
					if(board.freeCol(j)) related_cols[j] = 1;
			}
			
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
					setProofAndDisproofNumbers(node.children[current_child - 1], isMyTurn(), (threats[j] == 0) ? 0 : (short)(board.N + 1) );
					unmark(j);

					// move back the new child in the right place
					for(k = current_child - 1; (k > 0) && (threats[node.children[k - 1].col] > threats[j]); k--)
						Auxiliary.swap(node.children, k, k - 1);
				}
			}

			// shuffle children with same priority
			int start, end;
			for(start = 0; start < related_cols_n; start++) {
				for(end = 0;
					end < related_cols_n && threats[node.children[end].col] == threats[node.children[start].col];
					end++
				) ;
				Auxiliary.shuffleArrayRange(node.children, start, end);
				start = end;
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