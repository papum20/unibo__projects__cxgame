package connectx.pndb;

import connectx.CXCell;
import connectx.CXCellState;
import connectx.CXGame;
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

	//swaps two elements in an array
	public static <T> void swap(T[] V, int a, int b) {
		T tmp = V[a];
		V[a] = V[b];
		V[b] = tmp;
	}

	public static CXCellState opponent(CXCellState player) {
		return (player == CXCellState.P1) ? CXCellState.P2 : CXCellState.P1;
	}
	public static boolean equalMNKCells(CXCell a, CXCell b) {
		return a.i == b.i && a.j == b.j;
	}
	//makes sense only assuming it's a win, and the cell is not empty
	public static CXGameState cellState2winState(CXCellState cell_state) {
		return (cell_state == CXCellState.P1) ? CXGameState.WINP1 : CXGameState.WINP2;
	}

	public static byte CX2cellState(CXCellState game_state) {
		switch(game_state) {
			case P1:	return CellState.ME;
			case P2:	return CellState.YOU;
			default:	return CellState.FREE;
		}
	}
	public static byte CX2gameState(CXGameState game_state) {
		switch(game_state) {
			case DRAW:	return GameState.DRAW;
			case WINP1:	return GameState.P1;
			case WINP2:	return GameState.P2;
			default:	return GameState.OPEN;
		}
	}
	
}
