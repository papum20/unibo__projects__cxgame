package pndb.nocel;

import pndb.alpha.DbSearchResult;
import pndb.alpha.PnNode;
import pndb.alpha._PnSearch;
import pndb.constants.Auxiliary;
import pndb.constants.CellState;




/**
 * Removes alignments_by_cell and their uses.
 */
public class Player extends _PnSearch<DbSearchResult, DbSearch> {
	

	
	@Override
	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
		
		dbSearch = new DbSearch();		
		super.initPlayer(M, N, X, first, timeout_in_secs);
	}

	@Override
	public String playerName() {
		return "pndb nocel";
	}


	//#region PN_SEARCH

		/**
		 * Evaluate `node` according to a DbSearch.
		 * @param node
		 * @param player
		 * @return true if found a win.
		 */
		@Override
		protected boolean evaluateDb(PnNode node, byte player) {

			log += "evaluateDb\n";

			DbSearchResult res_db = dbSearch.selectColumn(board, node, timer_end - System.currentTimeMillis(), player);
	
			if(res_db == null)
				return false;

			TT.setStateOrInsert(player, Auxiliary.cellState2winState(player), Auxiliary.getPlayerBit(player));
				
			/* note: probably, prune is useless now, as evaluate is only called when node hasn't been expanded yet.
				*/
			
			/* if a win is found without expanding, need to save the winning move somewhere (in a child)
			* (especially for the root, or you wouldn't know the correct move)
			*/
			if(node != root)
				node.prove(player == CellState.P1, false);
			
			// root is only evaluated once, before expanding
			node.expand(1);
			node.children[0] = new PnNode(res_db.winning_col, node);							// can overwrite a child
			node.children[0].prove(player == CellState.P1, false);

			/* Heuristic: update parent's children with iterated related squares.
				* If, in the current board, the current player has a winning sequence,
				* starting with a certain move `x` in column `X` involving certain cells `s` (thus certain columns `S`),
				* if the other player (who moved in the parent node) was to make a move not in any of `S`,
				* then the current player could play `x`, and apply his winning sequence as planned,
				* because the opponent's move is useless for such sequence.
				* As an additional proof, if current player could create a new threat or avoid the opponent's one with 
				* such different move, then `s` wouldn't represent a winning sequence (Db also checks defenses).
				*/
			//filterChildren(node.parent, res_db.related_squares_by_col);
			
			return true;
		}
		
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

		}
	
	//#endregion PN_SEARCH

}
