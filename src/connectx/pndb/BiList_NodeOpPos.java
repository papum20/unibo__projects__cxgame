package connectx.pndb;

import connectx.CXCellState;
import connectx.pndb.BiList.BiNode;

public class BiList_NodeOpPos {
	


	private BiList<BiNode<OperatorPosition>> p1;
	private BiList<BiNode<OperatorPosition>> p2;



	public BiList_NodeOpPos() {
		p1 = new BiList<BiNode<OperatorPosition>>();
		p2 = new BiList<BiNode<OperatorPosition>>();
	}
	

	public BiNode<BiNode<OperatorPosition>> add(CXCellState player, BiNode<OperatorPosition> node) {
		BiList<BiNode<OperatorPosition>> list = (player == CXCellState.P1) ? p1 : p2;
		BiNode<BiNode<OperatorPosition>> res = list.addFirst(node);
		return res;
	}
	public void remove(CXCellState player, BiNode<BiNode<OperatorPosition>> node) {
		BiList<BiNode<OperatorPosition>> list = (player == CXCellState.P1) ? p1 : p2;
		list.remove(node);
	}
	public boolean isEmpty(CXCellState player) {
		return (player == CXCellState.P1) ? p1.isEmpty() : p2.isEmpty();
	}
	public BiNode<BiNode<OperatorPosition>> getFirst(CXCellState player) {
		return (player == CXCellState.P1) ? p1.getFirst() : p2.getFirst();
	}

}
