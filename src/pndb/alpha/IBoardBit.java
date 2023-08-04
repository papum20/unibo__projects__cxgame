package pndb.alpha;

import java.util.ArrayList;

import connectx.CXCellState;
import connectx.CXGameState;
import pndb.constants.MovePair;




/**
 * @type S : self
 */
public interface IBoardBit {


	public byte markCheck(int col, byte player);

	public void unmark(int col);

	
	//#region GET_SET

		/*
		 * return board's value, i.e. 1 if occupied by first player.
		 */
		public byte _cellState(int i, int j);

		/*
		 * return board_mask's value, i.e. 1 if occupied by someone.
		 */
		public byte _cellMaskState(int i, int j);

		public byte cellState(int i, int j);
		public CXCellState cellStateCX(int i, int j);
		/**
		 * 
		 * @param c
		 * @return the cell's state, as CellState
		 */
		public byte cellState(MovePair c);
		public CXCellState cellStateCX(MovePair c);
		public boolean cellFree(int i, int j);

		/*
		 * convert cell to GameState, assuming cell is occupied by someone.
		 */
		public byte cell2GameState(int i, int j);
		public byte cell2GameState(byte cell_state);
		public CXGameState cell2GameStateCX(int i, int j);

		public boolean freeCol(int j);

		public ArrayList<Integer> freeCols();

		public byte gameState();
		public CXGameState gameStateCX();
		public void setGameState(CXGameState state);
		
	//#endregion GET_SET



		public void print();

		/**
		 * don't print, return as a string.
		 */
		public String printString(int indentation);

	//#endregion DEBUG


}