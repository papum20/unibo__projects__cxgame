package connectx.pndb.threats;



public class ThreatApplied {
	
	public final Threat threat;
	public final int related_index;		// index used of threat's related
	public final byte attacker;

	public ThreatApplied(Threat threat, int related_index, byte attacker) {
		this.threat = threat;
		this.related_index = related_index;
		this.attacker = attacker;
	}

	@Override
	public String toString() {
		return threat.related[related_index] + " " + attacker;
	}
}
