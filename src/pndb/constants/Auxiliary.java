package pndb.constants;

import java.util.Collections;
import java.util.LinkedList;

import connectx.CXCell;
import connectx.CXCellState;
import connectx.CXGameState;



public class Auxiliary {

	public static short abs_short(short a) {
		return (a > 0) ? a : (short)(-a);
	}
	public static short max_short(short a, short b) {
		return (a > b) ? a : b;
	}
	public static int clamp(int a, int min, int max) {
		if(a < min) return min;
		else if(a >= max) return max - 1;
		else return a;
	}

	// copies a CXCell
	public static CXCell copyCell(CXCell c) {
		return new CXCell(c.i, c.j, c.state);
	}
	

	//#region STATES
	
		public static CXCellState opponentCX(CXCellState player) {
			return (player == CXCellState.P1) ? CXCellState.P2 : CXCellState.P1;
		}
		public static byte opponent(byte player) {
			return (player == CellState.P1) ? CellState.P2 : CellState.P1;
		}

		public static int getPlayerBit(byte player) {
			return player - 1;
		}

		//public static boolean equalMNKCells(CXCell a, CXCell b) {
		//	return a.i == b.i && a.j == b.j;
		//}


		//makes sense only assuming it's a win, and the cell is not empty
		public static CXGameState CXcellState2winState(CXCellState cell_state) {
			return (cell_state == CXCellState.P1) ? CXGameState.WINP1 : CXGameState.WINP2;
		}
		public static byte cellState2winState(byte cell_state) {
			return (cell_state == CellState.P1) ? GameState.WINP1 : GameState.WINP2;
		}
		public static CXGameState cellState2winStateCX(byte cell_state) {
			return (cell_state == CellState.P1) ? CXGameState.WINP1 : CXGameState.WINP2;
		}

		public static byte CX2cellState(CXCellState game_state) {
			switch(game_state) {
				case P1:	return CellState.P1;
				case P2:	return CellState.P2;
				default:	return CellState.FREE;
			}
		}


		public static byte CX2gameState(CXGameState game_state) {
			switch(game_state) {
				case DRAW:	return GameState.DRAW;
				case WINP1:	return GameState.WINP1;
				case WINP2:	return GameState.WINP2;
				default:	return GameState.OPEN;
			}
		}
		public static CXGameState gameState2CX(byte game_state) {
			switch(game_state) {
				case GameState.DRAW:	return CXGameState.DRAW;
				case GameState.WINP1:		return CXGameState.WINP1;
				case GameState.WINP2:		return CXGameState.WINP2;
				default:				return CXGameState.OPEN;
			}
		}

	//#endregion STATES



	//#region ARRAYS

		//swaps two elements in an array
		public static <T> void swap(T[] V, int a, int b) {
			T tmp = V[a];
			V[a] = V[b];
			V[b] = tmp;
		}

		/**
		 * 
		 * @param <T> any array element type
		 * @param v array to shuffle
		 * @param start included
		 * @param end excluded
		 */
		public static <T> void shuffleArrayRange(T[] v, int start, int end) {

			LinkedList<T> shuffler = new LinkedList<T>();
			for(int i = start; i < end; i++) shuffler.add(v[i]);
			Collections.shuffle(shuffler);
			for(int i = start; i < end; i++) v[i] = shuffler.pop();

		}

	//#endregion ARRAYS
	
}
