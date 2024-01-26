package pndbg.deltold.threats;

import pndbg.constants.MovePair;



public class ThreatPosition {
	public final MovePair start;
	public final MovePair end;
	public final byte type;
	public final MovePair last_stacked;
	public final byte stacked;

	public ThreatPosition() {
		start	= null;
		end		= null;
		type	= '\0';
		last_stacked = null;
		stacked	= 0;
	}
	public ThreatPosition(MovePair start, MovePair end, byte type) {
		this.start		= new MovePair(start);
		this.end		= new MovePair(end);
		this.type		= type;
		this.last_stacked = null;
		this.stacked	= 0;
	}
	public ThreatPosition(MovePair start, MovePair end, byte type, MovePair last_stacked, byte stacked) {
		this.start		= new MovePair(start);
		this.end		= new MovePair(end);
		this.type		= type;
		this.last_stacked = new MovePair(last_stacked);
		this.stacked	= stacked;
	}

	public int length() {
		return Math.max(Math.abs(start.i -end.i) , Math.abs(start.j - end.j));
	}
	/*public void set(MovePair start, MovePair end, byte type) {
		this.start = new MovePair(start);
		this.end = new MovePair(end);
		this.type = type;
	}*/
	//returns the position at offset index from start towards end
	public MovePair at(int index) {
		int diff_i = end.i - start.i, diff_j = end.j - start.j;
		int len = (diff_i > diff_j) ? diff_i : diff_j;
		if(-diff_i > len) len = -diff_i;
		if(-diff_j > len) len = -diff_j;
		return start.getSum(diff_i / len * index, diff_j / len * index);
	}

	@Override public String toString() {
		return start + "->" + end + " : " + type + ";stack:" + stacked + " " + last_stacked;
	}
}
