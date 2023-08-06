package pndb.tt;

import pndb.constants.GameState;



public class TranspositionElementEntry {

	public byte[] state;	//0=attacker, 1=defender
	
	private TranspositionElementEntry(byte state_a, byte state_d) {
			this.state = new byte[]{state_a, state_d};
	}

	public static final TranspositionElementEntry ELEMENT_ENTRIES[] = new TranspositionElementEntry[]{
		new TranspositionElementEntry(GameState.NULL,	GameState.NULL),		//0
		new TranspositionElementEntry(GameState.NULL,	GameState.OPEN),
		new TranspositionElementEntry(GameState.NULL,	GameState.DRAW),
		new TranspositionElementEntry(GameState.NULL,	GameState.WINP1),
		new TranspositionElementEntry(GameState.NULL,	GameState.WINP2),
		new TranspositionElementEntry(GameState.OPEN,	GameState.NULL),		//5
		new TranspositionElementEntry(GameState.OPEN,	GameState.OPEN),
		new TranspositionElementEntry(GameState.OPEN,	GameState.DRAW),
		new TranspositionElementEntry(GameState.OPEN,	GameState.WINP1),
		new TranspositionElementEntry(GameState.OPEN,	GameState.WINP2),
		new TranspositionElementEntry(GameState.DRAW,	GameState.NULL),		//10
		new TranspositionElementEntry(GameState.DRAW,	GameState.OPEN),
		new TranspositionElementEntry(GameState.DRAW,	GameState.DRAW),
		new TranspositionElementEntry(GameState.DRAW,	GameState.WINP1),
		new TranspositionElementEntry(GameState.DRAW,	GameState.WINP2),
		new TranspositionElementEntry(GameState.WINP1,	GameState.NULL),		//15
		new TranspositionElementEntry(GameState.WINP1,	GameState.OPEN),
		new TranspositionElementEntry(GameState.WINP1,	GameState.DRAW),
		new TranspositionElementEntry(GameState.WINP1,	GameState.WINP1),
		new TranspositionElementEntry(GameState.WINP1,	GameState.WINP2),
		new TranspositionElementEntry(GameState.WINP2,	GameState.NULL),		//20
		new TranspositionElementEntry(GameState.WINP2,	GameState.OPEN),
		new TranspositionElementEntry(GameState.WINP2,	GameState.DRAW),
		new TranspositionElementEntry(GameState.WINP2,	GameState.WINP1),
		new TranspositionElementEntry(GameState.WINP2,	GameState.WINP2)

	};
	
}
