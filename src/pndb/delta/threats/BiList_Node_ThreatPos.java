package pndb.delta.threats;

import pndb.constants.CellState;
import pndb.structures.BiList;
import pndb.structures.BiList.BiNode;

public class BiList_Node_ThreatPos {
	


	private BiList<BiNode<ThreatPosition>> p1;
	private BiList<BiNode<ThreatPosition>> p2;



	public BiList_Node_ThreatPos() {
		p1 = new BiList<BiNode<ThreatPosition>>();
		p2 = new BiList<BiNode<ThreatPosition>>();
	}
	

	public BiNode<BiNode<ThreatPosition>> add(byte player, BiNode<ThreatPosition> node) {
		BiList<BiNode<ThreatPosition>> list = (player == CellState.P1) ? p1 : p2;
		BiNode<BiNode<ThreatPosition>> res = list.addFirst(node);
		return res;
	}
	public void remove(byte player, BiNode<BiNode<ThreatPosition>> node) {
		BiList<BiNode<ThreatPosition>> list = (player == CellState.P1) ? p1 : p2;
		list.remove(node);
	}
	public boolean isEmpty(byte player) {
		return (player == CellState.P1) ? p1.isEmpty() : p2.isEmpty();
	}
	public BiNode<BiNode<ThreatPosition>> getFirst(byte player) {
		return (player == CellState.P1) ? p1.getFirst() : p2.getFirst();
	}

}
