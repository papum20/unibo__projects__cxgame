package pndb2.structures;

import java.util.ArrayList;

import pndb2.structures.BiList.BiNode;
import pndb2.threats.ThreatPosition;



public class AlignmentsRows extends ArrayList<BiList_ThreatPos> {

	/**
	 * Complexity: O(size)
	 * @param size
	 */
	public AlignmentsRows(int size) {
		super(size);
		for(int i = 0; i < size; i++)
			add(null);
	}

	/**
	 * Complexity: O(copy.size + copy_elements_n)
	 * @param copy
	 */
	public AlignmentsRows(AlignmentsRows copy) {
		super(copy.size());
		int size = copy.size();
		for(int i = 0; i < size; i++) {
			if(copy.get(i) == null) add(null);
			else add(new BiList_ThreatPos(copy.get(i)));
		}
	}

	/**
	 * Complexity: O(1)
	 * @param player
	 * @param index
	 * @param f
	 * @return
	 */
	public BiNode<ThreatPosition> add(byte player, int index, ThreatPosition f) {
		BiList_ThreatPos list = get(index);
		if(list == null) {
			list = new BiList_ThreatPos();
			set(index, list);
		}
		BiNode<ThreatPosition> res = list.add(player, f);
		return res;
	}

	/**
	 * Complexity: O(1)
	 * @param player
	 * @param index
	 * @param node
	 */
	public void remove(byte player, int index, BiNode<ThreatPosition> node) {
		get(index).remove(player, node);
	}

	/**
	 * Complexity: O(1)
	 * @param player
	 * @param index
	 * @return
	 */
	public BiNode<ThreatPosition> getFirst(byte player, int index) {
		BiNode<ThreatPosition> res = get(index).getFirst(player);
		return res;
	}

}
