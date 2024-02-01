package pndbtime.structures;

import pndbtime.constants.CellState;
import pndbtime.structures.BiList.BiNode;
import pndbtime.threats.Alignment;



public class BiList_Alignments {

	private BiList<Alignment> p1;
	private BiList<Alignment> p2;



	public BiList_Alignments() {
		p1 = new BiList<Alignment>();
		p2 = new BiList<Alignment>();
	}
	/**
	 * WARNING: doesn't create new instances of each OperatorPosition, just uses the same
	 * Complexity: O(2 * list_length)
	 */
	public BiList_Alignments(BiList_Alignments copy) {
		p1 = new BiList<Alignment>();
		p2 = new BiList<Alignment>();
		copy(p1, copy.p1.getFirst());
		copy(p2, copy.p2.getFirst());
	}

	/**
	 * Complexity: O(list_length)
	 * @param dest
	 * @param from_node
	 */
	private void copy(BiList<Alignment> dest, BiNode<Alignment> from_node) {
		if(from_node != null) {
			copy(dest, from_node.next);
			dest.addFirst(from_node.item);
		}
	}


	/**
	 * Complexity: O(1)
	 * @param player
	 * @param f
	 * @return
	 */
	public BiNode<Alignment> add(byte player, Alignment f) {
		BiList<Alignment> list = (player == CellState.P1) ? p1 : p2;
		BiNode<Alignment> res = list.addFirst(f);
		return res;
	}

	/**
	 * Complexity: O(1)
	 * @param player
	 * @param node
	 */
	public void remove(byte player, BiNode<Alignment> node) {
		BiList<Alignment> list = (player == CellState.P1) ? p1 : p2;
		list.remove(node);
	}

	/**
	 * Complexity: O(1)
	 * @param player
	 * @return
	 */
	public boolean isEmpty(byte player) {
		return (player == CellState.P1) ? p1.isEmpty() : p2.isEmpty();
	}

	/**
	 * Complexity: O(1)
	 * @param player
	 * @return
	 */
	public BiNode<Alignment> getFirst(byte player) {
		return (player == CellState.P1) ? p1.getFirst() : p2.getFirst();
	}

}
