package pndb2.structures;

import pndb2.constants.CellState;
import pndb2.structures.BiList.BiNode;
import pndb2.threats.ThreatPosition;



public class BiList_ThreatPos {

	private BiList<ThreatPosition> p1;
	private BiList<ThreatPosition> p2;



	public BiList_ThreatPos() {
		p1 = new BiList<ThreatPosition>();
		p2 = new BiList<ThreatPosition>();
	}
	/**
	 * WARNING: doesn't create new instances of each OperatorPosition, just uses the same
	 * Complexity: O(2 * list_length)
	 */
	public BiList_ThreatPos(BiList_ThreatPos copy) {
		p1 = new BiList<ThreatPosition>();
		p2 = new BiList<ThreatPosition>();
		copy(p1, copy.p1.getFirst());
		copy(p2, copy.p2.getFirst());
	}

	/**
	 * Complexity: O(list_length)
	 * @param dest
	 * @param from_node
	 */
	private void copy(BiList<ThreatPosition> dest, BiNode<ThreatPosition> from_node) {
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
	public BiNode<ThreatPosition> add(byte player, ThreatPosition f) {
		BiList<ThreatPosition> list = (player == CellState.P1) ? p1 : p2;
		BiNode<ThreatPosition> res = list.addFirst(f);
		return res;
	}

	/**
	 * Complexity: O(1)
	 * @param player
	 * @param node
	 */
	public void remove(byte player, BiNode<ThreatPosition> node) {
		BiList<ThreatPosition> list = (player == CellState.P1) ? p1 : p2;
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
	public BiNode<ThreatPosition> getFirst(byte player) {
		return (player == CellState.P1) ? p1.getFirst() : p2.getFirst();
	}

}
