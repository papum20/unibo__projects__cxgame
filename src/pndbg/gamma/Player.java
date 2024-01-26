package pndbg.gamma;

import connectx.CXBoard;
import pndbg.alpha.Operators;
import pndbg.alpha.PnNode;
import pndbg.alpha._PnSearch;
import pndbg.constants.Auxiliary;
import pndbg.constants.CellState;
import pndbg.constants.Constants;
import pndbg.constants.GameState;
import pndbg.nocel.nonmc.tryit.DbSearchResult;
import pndbg.tt.TranspositionElementEntry;



/**
 * 1. (from betha)
 * 2. int for hasAlignments
 */
public class Player extends _PnSearch<DbSearchResult, DbSearch> {


	private short	level_root;		// tree depth starting from empty board

	private int		deepest_level;	// absolute (from empty board)
	private PnNode	deepest_node;


	
	public Player() {
		super(new Operators());
	}
	
	@Override
	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {

		dbSearch = new DbSearch(OPERATORS);
		level_root = first ? Constants.SHORT_0 : Constants.SHORT_1;
		super.initPlayer(M, N, X, first, timeout_in_secs);
	}

	@Override
	public int selectColumn(CXBoard B) {
		
		deepest_level	= -1;	// any level is > -1
		deepest_node	= null;
		level_root++;			// at each visit, it indicates root's level

		return super.selectColumn(B);
	}
	
	@Override
	public String playerName() {
		return "pndb gamma";
	}


	//#region PN_SEARCH

		/**
		 * Tryit ADD: while deepest_level for end-state can be updated in setProofAndDisproofNumbers,
		 * when an end-state is found in TT, it should be updated here.
		 * 
		 * @param node
		 * @param game_state
		 * @param player who has to move in node's board.
		 * @return true if evaluated the node, i.e. it's an ended state or db found a win sequence.
		 */
		@Override
		protected boolean evaluate(PnNode node, byte game_state, byte player) {

			// debug
			log += "evaluate\n";
		
			if(game_state == GameState.OPEN) {
				TranspositionElementEntry entry = TT.getState(board.hash);

				if(entry == null || entry.state[Auxiliary.getPlayerBit(player)] == GameState.NULL)
					return evaluateDb(node, player);

			short entry_depth = TT.getFinalDepth(board.hash);
			if(entry_depth > deepest_level) {
				deepest_level	= entry_depth; 
				deepest_node	= node;
			}
				node.prove( entry.state[Auxiliary.getPlayerBit(player)] == MY_WIN, false);
				return true;
			}
			else {
				
				if(game_state == GameState.WINP1)			// my win
					node.prove(true, true);		// root cant be ended, or the game would be
				else										// your win or draw
					node.prove(false, true);

				return true;
			}
		}

		/**
		 * Evaluate `node` according to a DbSearch.
		 * @param node
		 * @param player
		 * @return true if found a win.
		 */
		@Override
		protected boolean evaluateDb(PnNode node, byte player) {

			log += "evaluateDb\n";

			DbSearchResult res_db = dbSearch.selectColumn(board, node, timer_start + timer_duration - System.currentTimeMillis(), player, (byte)(OPERATORS.MAX_TIER - 1));
	
			if(res_db == null)
				return false;

			TT.setStateOrInsert(player, Auxiliary.cellState2winState(player), Auxiliary.getPlayerBit(player), (short)(level_root + current_level + res_db.threats_n * 2 + 1));
				
			/* note: probably, prune is useless now, as evaluate is only called when node hasn't been expanded yet.
				*/
			
			/* if a win is found without expanding, need to save the winning move somewhere (in a child)
			* (especially for the root, or you wouldn't know the correct move)
			*/
			if(node != root)
				node.prove(player == CellState.P1, false);
			
			// root is only evaluated once, before expanding
			node.expand(1);
			node.children[0] = new PnNode(res_db.winning_col, node);							// can overwrite a child
			node.children[0].prove(player == CellState.P1, false);

			/* Heuristic: update parent's children with iterated related squares.
				* If, in the current board, the current player has a winning sequence,
				* starting with a certain move `x` in column `X` involving certain cells `s` (thus certain columns `S`),
				* if the other player (who moved in the parent node) was to make a move not in any of `S`,
				* then the current player could play `x`, and apply his winning sequence as planned,
				* because the opponent's move is useless for such sequence.
				* As an additional proof, if current player could create a new threat or avoid the opponent's one with 
				* such different move, then `s` wouldn't represent a winning sequence (Db also checks defenses).
				*/
			//filterChildren(node.parent, res_db.related_squares_by_col);
			
			/* Update deepest node.
			 */
			if(level_root + current_level + (res_db.threats_n * 2 + 1) > deepest_level) {
				deepest_level	= level_root + current_level + (res_db.threats_n * 2 + 1);	// each threat counts as a move per each, except the last, winning move 
				deepest_node	= node;
			}
			
			return true;
		}

		/**
		 * Complexity: O(super.generateAllChildren + shuffle)
		 * 			= O(DbSearch + 2N**2 + 17N + 4XN + N )
		 * 			= O(DbSearch + N**2 + 18N + 4XN )
		 * @param node
		 * @param threat_scores_by_col
		 */
		@Override
		public void generateAllChildren(PnNode node, byte player) {

			// debug
			log += "generateChildren\n";

			/* Heuristic: implicit threat.
			 * Only inspect moves in an implicit threat, i.e. a sequence by which the opponent could win
			 * if the current player was to make a "null move".
			 * In fact, the opponent could apply such winning sequence, if the current player was to 
			 * make a move outside it, thus not changing his plans.
			 * Applied to CXGame: columns where the opponent has an immediate attacking move - which leads to a win for him -,
			 * i.e. where the attacker's move corresponds to the first free cell in the column, are for sure
			 * the most interesting (in fact, you would lose not facing them); however, other columns involved in the sequence are
			 * not ignored, since they could block the win too, and also to simplify the calculations by approximation.
			 */
			/* note: related_cols should already contain only available, not full, columns.
			 */

			DbSearchResult res_db = dbSearch.selectColumn(board, node, timer_start + timer_duration - System.currentTimeMillis(), Auxiliary.opponent(player), (byte)(OPERATORS.MAX_TIER - 1));

			/* Heuristic: sorting moves (previously selected from iterated related squares) by number/scores of own threats in them
			 * (i.e., for columns, the sum of the scores in the whole column).
			 */

			int		related_cols_n	= 0;
			int[]	related_cols,
					threats			= dbSearch.getThreatCounts(board, player);
			int current_child, j, k;

			/* Heuristic: nodes without any implicit threat should be considered less (or not at all),
			 * especially after a few moves (as, probably, after a few moves it's "guaranteed" there are always some).
			 * For now, not considered.
			 */
			if(res_db != null)
				related_cols = res_db.related_squares_by_col;
			else {
				related_cols = new int[board.N];
				for(j = 0; j < board.N; j++)
					if(board.freeCol(j)) related_cols[j] = 1;
			}
			
			// count the columns, i.e. the number of new children
			for(int moves_n : related_cols)
				if(moves_n > 0) related_cols_n++;

			node.expand(related_cols_n);
			current_child = 0;

			// fill children with such columns, and sort by threat_scores at the same time
			for(j = 0; j < board.N && current_child < related_cols_n; j++) {
				if(related_cols[j] > 0) {
					// then insert
					node.children[current_child++] = new PnNode(j, node);

					// set proof numbers
					/* Heuristic: nodes without any threat should be considered less (or not at all).
					 */
					mark(j);
					setProofAndDisproofNumbers(node.children[current_child - 1], isMyTurn(), (threats[j] == 0) ? 0 : (short)(board.N + 1) );
					unmark(j);

					// move back the new child in the right place
					for(k = current_child - 1; (k > 0) && (threats[node.children[k - 1].col] > threats[j]); k--)
						Auxiliary.swap(node.children, k, k - 1);
				}
			}

			// shuffle children with same priority
			int start, end;
			for(start = 0; start < related_cols_n; start++) {
				for(end = start + 1;
					end < related_cols_n && threats[node.children[end].col] == threats[node.children[start].col];
					end++
				) ;
				Auxiliary.shuffleArrayRange(node.children, start, end);
				start = end;
			}
			
		}

		/**
		 * set proof and disproof; update node.mostProving.
		 * assuming value=unknown, as nodes are evaluated in developNode.
		 * @param node
		 * @param my_turn
		 * @param offset offset for initProofAndDisproofNumbers, in case of `node` open and not expanded.
		 */
		@Override
		protected void setProofAndDisproofNumbers(PnNode node, boolean my_turn, short offset) {

			super.setProofAndDisproofNumbers(node, my_turn, offset);

			/* In case, update deepest node.
			 * No need to check node's value: if it's winning, the deepest_node won't be used;
			 * otherwise, it's not winning and it will be used.
			 * Also, this won't overwrite calculations in evaluateDb, as that adds moves.
			 */
			if(node.isProved() && (level_root + current_level > deepest_level)) {
				deepest_level	= level_root + current_level;
				deepest_node	= node;
			}

		}

		/**
		 * Init proof numbers to offset + current level in game tree.
		 * @param node
		 */
		@Override
		protected void initProofAndDisproofNumbers(PnNode node, short offset) {
			short number = (short)(offset + current_level / 2 + 1);		// +1 so never < 1
			node.setProofAndDisproof(number, number);
		}

	//#endregion PN_SEARCH

	//#region HELPER

		@Override
		protected PnNode bestNode() {
			
			// child with min proof/disproof ratio
			PnNode best = null;
			for(PnNode child : root.children) {
				if(child.n[DISPROOF] != 0 && (best == null || (float)child.n[PROOF] / child.n[DISPROOF] < (float)best.n[PROOF] / best.n[DISPROOF]) )
					best = child;
			}

			// if all moves are lost, get the first move to the deepest node.
			if(best == null) {
				PnNode p = deepest_node;
				while(p != root) {
					best = p;
					p = p.parent;
				}
			}

			return best;
		}

	//#endregion HELPER


}