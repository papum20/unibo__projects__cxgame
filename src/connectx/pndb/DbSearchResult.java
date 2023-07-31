package connectx.pndb;

import java.util.ArrayList;



/**
 * Return object of a DbSearch.
 * It's like a union structure, where it's only defined either
 * moves_ordered
 * or both threat_scores and related_squares,
 * besides won always defined.
 */
public class DbSearchResult {

	public boolean won;

	public ArrayList<Integer> moves_ordered;
	
	public int[] threat_scores_by_col;
	public int[] related_squares_by_col;



	public DbSearchResult(boolean won, ArrayList<Integer> moves_ordered) {
		this.won					= won;
		this.moves_ordered			= moves_ordered;
		this.threat_scores_by_col	= null;
		this.related_squares_by_col	= null;
		
	}

	public DbSearchResult(boolean won, int[] threat_scores_by_col, int[] related_squares_by_col) {
		this.won					= won;
		this.moves_ordered			= null;
		this.threat_scores_by_col	= threat_scores_by_col;
		this.related_squares_by_col	= related_squares_by_col;
	}
}
