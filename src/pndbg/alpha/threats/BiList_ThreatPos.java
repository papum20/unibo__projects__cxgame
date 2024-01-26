package pndbg.alpha.threats;

import pndbg.constants.CellState;
import pndbg.structures.BiList;
import pndbg.structures.BiList.BiNode;

public class BiList_ThreatPos {
	


	private BiList<ThreatPosition> p1;
	private BiList<ThreatPosition> p2;



	public BiList_ThreatPos() {
		p1 = new BiList<ThreatPosition>();
		p2 = new BiList<ThreatPosition>();
	}
	// WARNING: doesn't create new instances of each OperatorPosition, just uses the same
	public BiList_ThreatPos(BiList_ThreatPos copy) {
		p1 = new BiList<ThreatPosition>();
		p2 = new BiList<ThreatPosition>();
		copy(p1, copy.p1.getFirst());
		copy(p2, copy.p2.getFirst());
	}

	private void copy(BiList<ThreatPosition> dest, BiNode<ThreatPosition> from_node) {
		if(from_node != null) {
			copy(dest, from_node.next);
			dest.addFirst(from_node.item);
		}
	}


	public BiNode<ThreatPosition> add(byte player, ThreatPosition f) {
		BiList<ThreatPosition> list = (player == CellState.P1) ? p1 : p2;
		BiNode<ThreatPosition> res = list.addFirst(f);
		return res;
	}
	public void remove(byte player, BiNode<ThreatPosition> node) {
		BiList<ThreatPosition> list = (player == CellState.P1) ? p1 : p2;
		list.remove(node);
	}
	public boolean isEmpty(byte player) {
		return (player == CellState.P1) ? p1.isEmpty() : p2.isEmpty();
	}
	public BiNode<ThreatPosition> getFirst(byte player) {
		return (player == CellState.P1) ? p1.getFirst() : p2.getFirst();
	}

}
