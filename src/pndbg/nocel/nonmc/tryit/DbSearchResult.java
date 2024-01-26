package pndbg.nocel.nonmc.tryit;



/**
 * Return object of a DbSearch.
 * 
 */
public class DbSearchResult {

	public int		winning_col;
	public int[]	related_squares_by_col;
	public int		threats_n;				// number of threats in winning sequence


	public DbSearchResult(int winning_col, int[] related_squares_by_col, int threats_n) {
		this.winning_col			= winning_col;
		this.related_squares_by_col	= related_squares_by_col;
		this.threats_n				= threats_n;
	}
}
