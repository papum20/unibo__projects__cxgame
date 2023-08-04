package pndb.alpha;



public class Player extends PnSearch<DbSearch> {
	
	@Override
	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
		
		dbSearch = new DbSearch();		
		super.initPlayer(M, N, X, first, timeout_in_secs);
	}

}
