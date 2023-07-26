package connectx.pndb;

import java.util.ArrayList;

import connectx.CXCellState;
import connectx.pndb.BiList.BiNode;



public class AlignmentsList extends ArrayList<BiList_OpPos> {



	public AlignmentsList(int size) {
		super(size);
		for(int i = 0; i < size; i++)
			add(null);
	}
	public AlignmentsList(AlignmentsList copy) {
		super(copy.size());
		int size = copy.size();
		for(int i = 0; i < size; i++) {
			if(copy.get(i) == null) add(null);
			else add(new BiList_OpPos(copy.get(i)));
		}
	}

	public BiNode<ThreatPosition> add(CXCellState player, int index, ThreatPosition f) {
		BiList_OpPos list = get(index);
		if(list == null) {
			list = new BiList_OpPos();
			set(index, list);
		}
		BiNode<ThreatPosition> res = list.add(player, f);
		return res;
	}
	public void remove(CXCellState player, int index, BiNode<ThreatPosition> node) {
		get(index).remove(player, node);
	}
	public BiNode<ThreatPosition> getFirst(CXCellState player, int index) {
		BiNode<ThreatPosition> res = get(index).getFirst(player);
		return res;
	}

}
