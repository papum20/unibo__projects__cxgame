package pndb.alpha;




public abstract class IDbSearch<RES> extends Object {
	
	protected BoardBitDb board;
	

	public abstract void init(int M, int N, int X, boolean first);


	public abstract RES selectColumn(BoardBit B, PnNode root_pn, long time_remaining, byte player);

	public abstract int[] getThreatCounts(BoardBit B, byte player);


}