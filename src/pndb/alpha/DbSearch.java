package pndb.alpha;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

import pndb.alpha.threats.ThreatApplied;
import pndb.alpha.threats.ThreatCells;
import pndb.alpha.threats.ThreatCells.USE;
import pndb.constants.CellState;
import pndb.tt.TranspositionTable;



public class DbSearch extends _DbSearch<DbSearchResult, BoardBitDb, DbNode<BoardBitDb>> {



	
	public DbSearch() {
		super(new DbNode<BoardBitDb>());
	}

	public void init(int M, int N, int X, boolean first) {
		
		this.M = M;
		this.N = N;

		MY_PLAYER	= CellState.P1;
		BoardBitDb.MY_PLAYER = MY_PLAYER;
		
		board = new BoardBitDb(M, N, X);
		TT = new TranspositionTable(M, N);
		
		BoardBitDb.TT = TT;

	}

	/**
	 * 
	 * @param B
	 * @param root_pn
	 * @param time_remaining
	 * @return a DbSearchResult structure, filled as follows:  
	 * 1.	if found a winning sequence, winning_col is the first winning move,
	 * 		and related_squares_by_col contains, for each column j, the number of squares related to the winning sequence, in column j;
	 * 2.	otherwise, it's null.
	 */
	public DbSearchResult selectColumn(BoardBit B, PnNode root_pn, long time_remaining, byte player) {
		
		// debug
		log = "__\ndbSearch\n";

		DbNode<BoardBitDb> root = null;;

		try {

			GOAL_SQUARES = new boolean[M][N];
			// initialized to false
			
			// timer
			timer_start	= System.currentTimeMillis();
			timer_end	= timer_start + time_remaining;

			// update own board instance
			board = new BoardBitDb(B);
			board.setPlayer(player);

			board.findAllAlignments(player, Operators.TIER_MAX, true, "selCol_");
			
			// debug
			if(DEBUG_ON && board.hasAlignments(player)) {
				file = new FileWriter("debug/db1main/main" + (counter++) + "_" + debugRandomCode() + "_" + board.getMC_n() + "-" + (board.getMC_n() > 0 ? board.MC[board.getMC_n()-1] : "_") + ".txt");
				file.write("root board:\n" + board.printString(0) + board.printAlignmentsString(0));
				file.close();
			}
			
			// db init
			root = createRoot(board);
			win_node 	= null;
			found_win_sequences = 0;
			
			// recursive call for each possible move
			visit(root, player, true, Operators.TIER_MAX);
			root = null;

			// debug
			if(DEBUG_ON) file.close();
			if(foundWin()) {
				if(DEBUG_PRINT)
					System.out.println("found win: " + foundWin() );
				log += "found win: " + foundWin() + "\n";
				log += "win node \n";
				log += win_node.board.printString(0);
			}
			

			if(foundWin())
				return getReturnValue(player);

			return null;

		} catch (IOException io) {
			return null;
		} catch (ArrayIndexOutOfBoundsException e) {
			root.board.print();
			System.out.println(log + "\n");
			throw e;
		} catch (Exception e) {
			System.out.println(log);
			if(DEBUG_ON) try {file.close();} catch(IOException io) {}
			throw e;
		}

	}

	public int[] getThreatCounts(BoardBit B, byte player) {

		board = new BoardBitDb(B);
		return board.getThreatCounts(player);
	}
	
	//#region CREATE

		@Override
		protected DbNode<BoardBitDb> createNode(BoardBitDb board, boolean is_combination, int max_tier) {
			return new DbNode<BoardBitDb>(board, is_combination, max_tier);
		}

		/**
		 * sets child's game_state if entry exists in TT
		 */
		protected DbNode<BoardBitDb> addDependentChild(DbNode<BoardBitDb> node, ThreatCells threat, int atk, LinkedList<DbNode<BoardBitDb>> lastDependency, byte attacker) {
			
			// debug
			log += "addDepChild\n";

			BoardBitDb new_board			= node.board.getDependant(threat, atk, USE.BTH, node.getMaxTier(), true);
			DbNode<BoardBitDb> newChild 	= new DbNode<BoardBitDb>(new_board, false, node.getMaxTier());

			node.addChild(newChild);
			lastDependency.add(newChild);

			return newChild;
		}

	//#endregion CREATE
	

	//#region HELPER
		
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
			
			return new DbSearchResult(winning_col, related_squares_by_col);
		}

	//#endregion HELPER

}
