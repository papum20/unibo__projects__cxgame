package pndb.constants;



public class Constants {
	
	//#region SHORT

	public static final short SHORT_0	= (short)0;
	public static final short SHORT_1	= (short)1;
	public static final byte BYTE_1		= (byte)1;

	//#endregion SHORT


	//#region STRUCTS

	public static final MovePair DIRECTIONS[] = {
		new MovePair(-1, 0),
		new MovePair(-1, 1),
		new MovePair(0, 1),
		new MovePair(1, 1),
		new MovePair(1, 0),
		new MovePair(1, -1),
		new MovePair(0, -1),
		new MovePair(-1, -1)
	};

	public static enum BoardsRelation {
		CONFLICT, USELESS, USEFUL
	}
	
	//#endregion STRUCTS



	//#region FUNCTIONS
	
	public static int opponent(byte player) {
		return 3 - player;
	}

	//#endregion FUNCTIONS
	
	
}
