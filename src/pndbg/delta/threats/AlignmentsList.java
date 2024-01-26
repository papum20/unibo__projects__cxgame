package pndbg.delta.threats;

import java.util.ArrayList;

import pndbg.structures.BiList.BiNode;



public class AlignmentsList extends ArrayList<BiList_ThreatPos> {



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
			else add(new BiList_ThreatPos(copy.get(i)));
		}
	}

	public BiNode<ThreatPosition> add(byte player, int index, ThreatPosition f) {
		BiList_ThreatPos list = get(index);
		if(list == null) {
			list = new BiList_ThreatPos();
			set(index, list);
		}
		BiNode<ThreatPosition> res = list.add(player, f);
		return res;
	}
	public void remove(byte player, int index, BiNode<ThreatPosition> node) {
		get(index).remove(player, node);
	}
	public BiNode<ThreatPosition> getFirst(byte player, int index) {
		BiNode<ThreatPosition> res = get(index).getFirst(player);
		return res;
	}

}
