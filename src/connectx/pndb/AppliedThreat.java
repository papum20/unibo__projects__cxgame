package connectx.pndb;

import connectx.CXCellState;
import connectx.pndb.Operators.Threat;



public class AppliedThreat {
	public final Threat threat;
	public final int atk;
	public final CXCellState attacker;

	AppliedThreat(Threat threat, int atk, CXCellState attacker) {
		this.threat = threat;
		this.atk = atk;
		this.attacker = attacker;
	}
}
