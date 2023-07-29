package connectx.pndbfinal;

import connectx.CXGameState;

public class TranspositionElementEntry {

	public CXGameState[] state;	//0=attacker, 1=defender
	
	private TranspositionElementEntry(CXGameState state_a, CXGameState state_d) {
			this.state = new CXGameState[]{state_a, state_d};
	}

	public static final TranspositionElementEntry ELEMENT_ENTRIES[] = new TranspositionElementEntry[]{
		new TranspositionElementEntry(null, null),			//0
		new TranspositionElementEntry(null, CXGameState.OPEN),
		new TranspositionElementEntry(null, CXGameState.DRAW),
		new TranspositionElementEntry(null, CXGameState.WINP1),
		new TranspositionElementEntry(null, CXGameState.WINP2),
		new TranspositionElementEntry(CXGameState.OPEN, null),		//5
		new TranspositionElementEntry(CXGameState.OPEN, CXGameState.OPEN),
		new TranspositionElementEntry(CXGameState.OPEN, CXGameState.DRAW),
		new TranspositionElementEntry(CXGameState.OPEN, CXGameState.WINP1),
		new TranspositionElementEntry(CXGameState.OPEN, CXGameState.WINP2),
		new TranspositionElementEntry(CXGameState.DRAW, null),		//10
		new TranspositionElementEntry(CXGameState.DRAW, CXGameState.OPEN),
		new TranspositionElementEntry(CXGameState.DRAW, CXGameState.DRAW),
		new TranspositionElementEntry(CXGameState.DRAW, CXGameState.WINP1),
		new TranspositionElementEntry(CXGameState.DRAW, CXGameState.WINP2),
		new TranspositionElementEntry(CXGameState.WINP1, null),		//15
		new TranspositionElementEntry(CXGameState.WINP1, CXGameState.OPEN),
		new TranspositionElementEntry(CXGameState.WINP1, CXGameState.DRAW),
		new TranspositionElementEntry(CXGameState.WINP1, CXGameState.WINP1),
		new TranspositionElementEntry(CXGameState.WINP1, CXGameState.WINP2),
		new TranspositionElementEntry(CXGameState.WINP2, null),		//20
		new TranspositionElementEntry(CXGameState.WINP2, CXGameState.OPEN),
		new TranspositionElementEntry(CXGameState.WINP2, CXGameState.DRAW),
		new TranspositionElementEntry(CXGameState.WINP2, CXGameState.WINP1),
		new TranspositionElementEntry(CXGameState.WINP2, CXGameState.WINP2)

	};
	
}
