package pndb.structures;



/**
 * Bi-directional linked list.
 */
public class BiList<T> {

	private BiNode<T> head;


	/**
	 * Complexity: O(1)
	 */
	public BiList() {
		head = null;
	}

	/**
	 * Complexity: O(1)
	 * @param item
	 * @return
	 */
	public BiNode<T> addFirst(T item) {
		BiNode<T>  node = new BiNode<T>(item, null, head);
		if(head != null) head.prev = node;
		head = node;
		return node;
	}

	/**
	 * Complexity: O(1)
	 * @param node
	 */
	public void remove(BiNode<T> node) {
		if(node.prev == null) head = node.next;
		else node.prev.next = node.next;
		if(node.next != null) node.next.prev = node.prev;
	}
	public boolean isEmpty() {
		return head == null;
	}
	public BiNode<T> getFirst() {
		return head;
	}


	
	public static class BiNode<T> {
		public T item;
		public BiNode<T> prev;
		public BiNode<T> next;

		public BiNode() {
			item = null;
			prev = null;
			next = null;
		}
		public BiNode(T item, BiNode<T> prev, BiNode<T> next) {
			this.item = item;
			this.prev = prev;
			this.next = next;
		}
	}
}
