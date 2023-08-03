package connectx.pndb2;

import connectx.pndb2.Operators.ThreatCells;



public class ThreatApplied {
	public final ThreatCells threat;
	public final int related_index;		// index used of threat's related
	public final byte attacker;

	ThreatApplied(ThreatCells threat, int related_index, byte attacker) {
		this.threat = threat;
		this.related_index = related_index;
		this.attacker = attacker;
	}
}
