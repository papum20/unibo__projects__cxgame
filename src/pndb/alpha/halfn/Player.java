package pndb.alpha.halfn;

import pndb.alpha.DbSearch;
import pndb.alpha.PnNode;




/**
 * Heurisitc proof numbers initialization to LEVEL / 2 + 1.
 */
public class Player extends pndb.alpha.Player {

	@Override
	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {

		dbSearch = new DbSearch();
		super.initPlayer(M, N, X, first, timeout_in_secs);
	}

	@Override
	public String playerName() {
		return "pndb halfn";
	}


	//#region PN_SEARCH

		/**
		 * Init proof numbers to offset + current level in game tree.
		 * @param node
		 */
		protected void initProofAndDisproofNumbers(PnNode node, short offset) {
			short number = (short)(offset + current_level / 2 + 1);		// +1 so never < 1
			node.setProofAndDisproof(number, number);
		}

	//#endregion PN_SEARCH

}