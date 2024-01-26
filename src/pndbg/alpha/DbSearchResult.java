package pndbg.alpha;



/**
 * Return object of a DbSearch.
 * 
 */
public class DbSearchResult {

	public int		winning_col;
	public int[]	related_squares_by_col;


	public DbSearchResult(int winning_col, int[] related_squares_by_col) {
		this.winning_col			= winning_col;
		this.related_squares_by_col	= related_squares_by_col;
	}
}
