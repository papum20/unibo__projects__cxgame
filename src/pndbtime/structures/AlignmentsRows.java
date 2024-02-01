package pndbtime.structures;

import java.util.ArrayList;

import pndbtime.structures.BiList.BiNode;
import pndbtime.threats.Alignment;



public class AlignmentsRows extends ArrayList<BiList_Alignments> {

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
			else add(new BiList_Alignments(copy.get(i)));
		}
	}

	/**
	 * Complexity: O(1)
	 * @param player
	 * @param index
	 * @param f
	 * @return
	 */
	public BiNode<Alignment> add(byte player, int index, Alignment f) {
		BiList_Alignments list = get(index);
		if(list == null) {
			list = new BiList_Alignments();
			set(index, list);
		}
		BiNode<Alignment> res = list.add(player, f);
		return res;
	}

	/**
	 * Complexity: O(1)
	 * @param player
	 * @param index
	 * @param node
	 */
	public void remove(byte player, int index, BiNode<Alignment> node) {
		get(index).remove(player, node);
	}

	/**
	 * Complexity: O(1)
	 * @param player
	 * @param index
	 * @return
	 */
	public BiNode<Alignment> getFirst(byte player, int index) {
		BiNode<Alignment> res = get(index).getFirst(player);
		return res;
	}

}
