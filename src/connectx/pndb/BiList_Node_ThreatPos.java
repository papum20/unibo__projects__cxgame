package connectx.pndb;

import connectx.CXCellState;
import connectx.pndb.BiList.BiNode;

public class BiList_Node_ThreatPos {
	


	private BiList<BiNode<ThreatPosition>> p1;
	private BiList<BiNode<ThreatPosition>> p2;



	public BiList_Node_ThreatPos() {
		p1 = new BiList<BiNode<ThreatPosition>>();
		p2 = new BiList<BiNode<ThreatPosition>>();
	}
	

	public BiNode<BiNode<ThreatPosition>> add(CXCellState player, BiNode<ThreatPosition> node) {
		BiList<BiNode<ThreatPosition>> list = (player == CXCellState.P1) ? p1 : p2;
		BiNode<BiNode<ThreatPosition>> res = list.addFirst(node);
		return res;
	}
	public void remove(CXCellState player, BiNode<BiNode<ThreatPosition>> node) {
		BiList<BiNode<ThreatPosition>> list = (player == CXCellState.P1) ? p1 : p2;
		list.remove(node);
	}
	public boolean isEmpty(CXCellState player) {
		return (player == CXCellState.P1) ? p1.isEmpty() : p2.isEmpty();
	}
	public BiNode<BiNode<ThreatPosition>> getFirst(CXCellState player) {
		return (player == CXCellState.P1) ? p1.getFirst() : p2.getFirst();
	}

}
