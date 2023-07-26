package connectx.pndb;

import java.util.ArrayList;




/**
 * 
 * @param B own type
 */
public interface IBoard<B> {
	
	//#region CONSTANTS
	public static final int COL_NULL = -1;

	//#endregion CONSTANTS
	


	/**
	 * 
	 * @param col
	 * @param player
	 * @return GameState
	 */
	public byte mark(int col, byte player);

	/**
	 * 
	 * @param col
	 */
	public void unmark(int col);

	/**
	 * 
	 * @param i
	 * @param j
	 * @return
	 */
	// private boolean isWinningMove(int i, int j);


	//#region GET
		/**
		 * 
		 * @return ArrayList of number of free columns
		 */
		public ArrayList<Integer> freeCols();

	//#endregion GET


	//#region DEBUG

		// private void print();
		
	//#endregion DEBUG


}