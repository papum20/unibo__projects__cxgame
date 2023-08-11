package pndb.betha.scomb;

import java.util.LinkedList;

import pndb.alpha.BoardBit;
import pndb.alpha.Operators;
import pndb.alpha._DbSearch;
import pndb.alpha._Operators;
import pndb.alpha.threats.ThreatApplied;
import pndb.alpha.threats.ThreatCells;
import pndb.alpha.threats.ThreatCells.USE;
import pndb.constants.CellState;
import pndb.nocel.nonmc.DbNode;
import pndb.nocel.nonmc.tryit.DbSearchResult;
import pndb.tt.TranspositionTable;



/**
 * note:
 * -	never unmarks, as always creates a copy of a board.
 * -	TT: always used with MY_PLAYER = 0, YOUR = 1 (for state).
 * -	Combination stage uses TT with open states, in order to only search for already done combinations once.
 * 		However, being this specific for the current dbSearch, the TT entries are then removed, unless proved to another state.
 * -	For the previous point, boards with a state not open, in case, are added to TT as open, so they are not mistaken
 * 		in the combination stage (for implementation, because of the return type, boolean, of the functions).
 */
public class DbSearch extends _DbSearch<DbSearchResult, BoardBit, BoardBitDb, DbNode<BoardBit, BoardBitDb>> {



	public DbSearch(_Operators operators) {
		super(new DbNode<BoardBit, BoardBitDb>(), operators);
	}
	
	public void init(int M, int N, int X, boolean first) {
		
		this.M = M;
		this.N = N;

		MY_PLAYER	= CellState.P1;
		BoardBitDb.MY_PLAYER = MY_PLAYER;
		
		board = new BoardBitDb(M, N, X, OPERATORS);
		TT = new TranspositionTable(M, N);
		
		BoardBitDb.TT = TT;

		GOAL_SQUARES = new boolean[M][N];
		// initialized to false
	}


	@Override
	public int[] getThreatCounts(BoardBit B, byte player) {

		board = new BoardBitDb(B, OPERATORS);
		return board.getThreatCounts(player);
	}


		//#region ALGORITHM


		@Override
		protected DbNode<BoardBit, BoardBitDb> createNode(BoardBitDb board, boolean is_combination, int max_tier) {
			return new DbNode<BoardBit, BoardBitDb>(board, is_combination, max_tier);
		}

		@Override
		protected BoardBitDb createBoardDb(int M, int N, int X) {
			return new BoardBitDb(M, N, X, OPERATORS);
		}

		@Override
		protected BoardBitDb createBoardDb(BoardBit BB) {
			return new BoardBitDb(BB, OPERATORS);
		}

		/**
		 * sets child's game_state if entry exists in TT
		 */
		@Override
		protected DbNode<BoardBit, BoardBitDb> addDependentChild(DbNode<BoardBit, BoardBitDb> node, ThreatCells threat, int atk, LinkedList<DbNode<BoardBit, BoardBitDb>> lastDependency, byte attacker) {
			
			// debug
			log += "addDepChild\n";

			BoardBitDb new_board	= node.board.getDependant(threat, atk, USE.BTH, node.getMaxTier(), true);
			DbNode<BoardBit, BoardBitDb> newChild 		= new DbNode<BoardBit, BoardBitDb>(new_board, false, node.getMaxTier());

			node.addChild(newChild);
			lastDependency.add(newChild);

			return newChild;
		}

	//#endregion CREATE


	//#region GET_SET

		@Override
		protected DbSearchResult getReturnValue(byte player) {

			int		winning_col;
			int[]	related_squares_by_col;
			
			// the winning move is the player's move in the first threat in the sequence
			ThreatApplied winning_threat = win_node.board.markedThreats.getFirst();
			winning_col = winning_threat.threat.related[winning_threat.related_index].j;
			
			/* fill the related_squares_by_column with the number of newly made moves for each column
			*/
			related_squares_by_col = new int[N];
			for(int j = 0; j < N; j++)
				related_squares_by_col[j] = win_node.board.free[j] - board.free[j];
			
			return new DbSearchResult(winning_col, related_squares_by_col, win_node.board.markedThreats.size());
		}
		
	//#endregion HELPER

}
