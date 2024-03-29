package pndbg.betha.scomb;



/**
 * board.getCombined only search for attacker's threats, cell by cell (for those added).
 *
 * also: board.findAlignmentsInDiretion for a cell in O(X), being linear although the double loop.
 * In fact, (probably) it's not necessary to check for lower tier threats - only involving certain cells C-,
 * if there are any involving all C plus one ore more other.
 * 
 * also: board.getCombined: already checks isUseful(threat), so useless to check in findAlignments
 */
public class Player extends pndbg.betha.Player {



	@Override
	public String playerName() {
		return "pndb scomb";
	}



}