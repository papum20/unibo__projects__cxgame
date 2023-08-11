package pndb.alpha.nonum;

import pndb.alpha.DbSearch;
import pndb.alpha.PnNode;
import pndb.constants.Constants;




/**
 * No heurisitc proof numbers initialization (1,1).
 */
public class Player extends pndb.alpha.Player {

	@Override
	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {

		dbSearch = new DbSearch(OPERATORS);
		super.initPlayer(M, N, X, first, timeout_in_secs);
	}

	@Override
	public String playerName() {
		return "pndb nonum";
	}



	//#region PN_SEARCH

		/**
		 * Init proof numbers to offset + current level in game tree.
		 * @param node
		 */
		@Override
		protected void initProofAndDisproofNumbers(PnNode node, short offset) {
			node.setProofAndDisproof(Constants.SHORT_1, pndb.constants.Constants.SHORT_1);
		}

	//#region PN_SEARCH

}