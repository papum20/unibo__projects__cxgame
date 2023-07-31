package connectx.pndb;

import java.util.ArrayList;



/**
 * Return object of a DbSearch.
 */
public class DbSearchResult {

	public boolean won;
	public ArrayList<Integer> moves_ordered;

	public DbSearchResult(boolean won) {
		this.won = won;
	}
}
