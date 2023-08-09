package pndb.nonmc;

import pndb.alpha.BoardBit;
import pndb.alpha._BoardBitDb;
import pndb.alpha._Operators;
import pndb.constants.Constants.BoardsRelation;




public class BoardBitDb extends _BoardBitDb<BoardBitDb, BoardBit> {
	


	public BoardBitDb(int M, int N, int X, _Operators operators) {
		super(M, N, X, operators);
	}

	public BoardBitDb(BoardBit B, _Operators operators) {
		super(B, operators);
	}
	
	private BoardBitDb(BoardBitDb B, boolean copy_threats, _Operators operators) {
		super(B, copy_threats, operators);
	}

	public BoardBitDb getCopy(boolean copy_threats) {
		return new BoardBitDb(this, copy_threats, OPERATORS);
	}

	
	//#region DB_SEARCH

		/**
		 * Check if a combination with node is valid, i.e. if they're not in conflict and both have a marked cell the other doesn't.
		 * Assumes both boards have the same `MY_PLAYER` (i.e. the same bit-byte association for players).
		 * @param B
		 * @param attacker
		 * @return
		 */
		@Override
		public BoardsRelation validCombinationWith(BoardBitDb B, byte attacker) {

			long flip = (attacker == MY_PLAYER) ? 0 : -1;
			boolean useful_own = false, useful_other = false;

			for(int i = 0; i < COL_SIZE(M); i++) {
				for(int j = 0; j < N; j++) {
					// check conflict
					if( (board_mask[j][i] & B.board_mask[j][i] & (board[j][i] ^ B.board[j][i])) != 0 )
						return BoardsRelation.CONFLICT;
					// check own board adds something for attaacker
					if( (~(board_mask[j][i] & B.board_mask[j][i]) & (board[j][i] ^ flip)) != 0 )
						useful_own = true;
					// check other board adds something for attaacker
					if( (~(board_mask[j][i] & B.board_mask[j][i]) & (B.board[j][i] ^ flip)) != 0 )
						useful_own = true;
					
					if(useful_own && useful_other)
						return BoardsRelation.USEFUL;
				}
			}
			return BoardsRelation.USELESS;

		}
	
	//#endregion DB_SEARCH	

}