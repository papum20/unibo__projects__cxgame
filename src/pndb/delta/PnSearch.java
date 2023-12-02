package pndb.delta;

import java.util.LinkedList;
import java.util.ListIterator;

import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXPlayer;
import pndb.delta.constants.Auxiliary;
import pndb.delta.constants.CellState;
import pndb.delta.constants.GameState;
import pndb.delta.structs.DbSearchResult;
import pndb.delta.tt.TranspositionTable;
import pndb.delta.tt.TTElementProved;
import pndb.delta.tt.TTElementProved.KeyDepth;



/**
 * notes:
 * <p>	-	i'm always CellState.ME, GameState.P1.
 * <p>	-	TT: always used with MY_PLAYER = 0, YOUR = 1 (for state).
 * <p>	-	TT is used for positions evaluated and verified by db, so it contains certain values.
 * <p>	-	(tryit)	TT uses global depths.
 * <P>	-	tt.doppia entry: scopo: in base a chi tocca si usa indice
 * 
 * <p>Enhancements:
 * <p>	1.	(tryit) Saves all levels for endgames (in TT) and, for each visit, saves the deepest one,
 * 			so it can return it in case all moves are losing.
 * <p>	2.	(nonmc) no BoardBit.MC
 * <p>	3.	(nocel) no BoardBitDb.alignments_by_cell.
 * <p>	4.	(gamma) int for BoardBitDb.hasAlignments
 * <p>	5.	(halfn) use half number proof init
 * <p>	6.	(ranch) Randomly shuffles children with same priority, so they are analyzed in a different order
 * <p>	7.	() only create BoardBitDb.alignemts_by_dir when created first element
 * <p>	8.	(scomb) board.getCombined only search for attacker's threats, cell by cell (for those added).
 * <p>	9.	(scomb) board.getCombined: already checks isUseful(threat), so useless to check in findAlignments
 * <p>	10.	(delta) db correctly: cxgame is not like mnkgame, where one more move never gives any disadvantage
 * 			(but usually an advantage), so db performing all defending moves at the same time isn't (always)
 * 			correct. Idea for the correct implementation, which also mixes pn and db: you have to try each
 * 			defending move, individually, and make massive use of TT, as db will also need to check many
 * 			configurations multiple times; when player A creates a X-1, B is forced to reply (so all other moves
 * 			can be removed from pn); when creates a X-2, B is forced to reply (unless he has a better threat,
 * 			enhancement already used in db) - alternatively, he could make another move and be forced in the next
 * 			trun (when it's became a X-1)... (thinking in progress)... so, conclusion: can I just add a new operator,
 * 			for this case?? i.e. add a piece, and the empty cell above is part of a threat (also not in vertical 
 * 			direction).
 * <p>	11.	(delta) draw=max depth + 1, so is preferred in case of loss
 * <p>	12. db not correct, but almost: 1. only tier3 are not correct (involve more responses);
 * 			2. you could reach that position also putting attacker's moves, so wrong cases are rare.
 * <p>	13. (delta) new threat (stacked)... see note Operators.makeStacked()
 * 			...dubious
 * <p>	14. problem with transpositions in dags (thesis), ambiguity
 * <p>	15.	(delta) PnTTnode: more parents (dag)
 * <p>	16. (delta) no prune, keep nodes for next visits (using tt for each node)
 * <p>	17.	M*N < 2**15, because of short type for numbers
 * <p>	18.	dbSearch: note about wins by mistake, with threats of tier > 1 (intersecting with tier 1 ones)
 * <p>	19. why deleting previous nodes: i guess if you managed to prove a node from a previous position, which was
 * 			more generical, you'll probably be able to re-do it now, with less nodes.
 * <p>	20.	TT.remove at start of each selectColumn, to remove entries from previous rounds
 * <p>	21.	each node is inserted in dag at creation time
 * <p>	22.	in list nodes to remove, same node could be put more times: so use set
 * <p>	23.	PnTTnodes need children as array/list, bc of dag (each node has its own parents/children)
 * <p>	24.	updateAncestors could start updating proved nodes unreachable from root
 * <p>	25.	a node, if existing, is always either in tt dag or proved.
 * <p>	26. had to remove current_node enhancement: with dag it's very expensive keeping track of last updated, while selectMostProving isn't actually very expensive.
 * <p>	27. note on setProofAndDisproofNumbers->updateProved():
 *				If proved, set the final best move.
 *				Note that if current player won, knowing that at least one move led to a win, choose the one which wins first;
 *				otherwise, choose the deepest path.
 *				This also assures the correct precedence for draws (which have assigned max depth + 1) for both players.
 *				Finally, note that this could not be the deepest/least deep move, because you don't have to analyze the full tree.
 *				However, it's certain that, in every moment, a proved node is updated with the best move considering all the existsing tree in that instant.
 *				
 * TODO;
 * retry genChildren with null-move euristic and db 
 * 
 * .merge 2 threat classes, remove appliers
 * .(alla fine) aumenta tempo (tanto basta), ma metti comunque i check per interromperlo nel caso durante l'esecuzione (parti più costose: develop, ancestors, db)
 * .count mem, n of nodes
 * .remove time checks (in db): useless?
 * 
 * ENHANCEMENTS TO TRY (VS):
 * .make proved db node add proved child? so when other parents find it in dag, its proved already
 * .initProof: relative or absolute depth?
 * .db for (iterated) related squares with null move
 *	(same).Heuristic: update parent's children with iterated related squares.
 *	If, in the current board, the current player has a winning sequence,
 *	starting with a certain move `x` in column `X` involving certain cells `s` (thus certain columns `S`),
 *	if the other player (who moved in the parent node) was to make a move not in any of `S`,
 *	then the current player could play `x`, and apply his winning sequence as planned,
 *	because the opponent's move is useless for such sequence.
 *	
 *	As an additional proof, if current player could create a new threat or avoid the opponent's one with 
 *	such different move, then `s` wouldn't represent a winning sequence (Db also checks defenses).
 *	
 *	Probably that's already taken in count in parent's null-move dbSearch (generateAllChildren).
 *
 * NOT DONE (just ideas):
 * .db corretto: db ricorsivo su tier3 (con più risposte)
 * ..oppure: provale tutte, con accortezze: 1. usa tutte minacce di 1atk 1def (saranno molte combinazioni in più);
 * 2. a questo punto in combine, stai attento che da ognuno si aggiunga l'intera minaccia (1atk e 1def, non solo atk) (se no 
 * rischi di aggiungere più atk che def e la situazione non sarebbe raggiungibile)
 * 
 * QUESTIONS:
 * .stacked 13 dubious (are these new operators well made?)
 *	
 * 
 */
public class PnSearch implements CXPlayer {

	//#region CONSTANTS
		protected static final byte PROOF		= TTPnNode.PROOF;
		protected static final byte DISPROOF	= TTPnNode.DISPROOF;
		protected static final byte COL_NULL	= TTElementProved.COL_NULL;

		protected byte MY_PLAYER	= CellState.P1;
		protected byte YOUR_PLAYER	= CellState.P2;
		protected byte MY_WIN		= GameState.WINP1;
		protected byte YOUR_WIN		= GameState.WINP2;
	//#endregion CONSTANTS

	public BoardBitPn board;			// public for debug
	protected DbSearch dbSearch;
	
	protected TTPnNode root;
	
	// time / memory
	protected long timer_start;			//turn start (milliseconds)
	protected long timer_duration;		//time (millisecs) at which to stop timer
	protected Runtime runtime;
	
	// implementation
	protected TTPnNode		lastIt_root;
	protected BoardBitPn	lastIt_board;

	// debug
	private int created_n;

	
	

	public PnSearch() {}
	
	/**
	 * Complexity: O(5N + 4MN + 2**16 + (5MN + 3M+4N + 2**16) ) = O(9MN + 3M+9N + 2**17)
	 */
	@Override
	public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {

		BoardBit.M = (byte)M;
		BoardBit.N = (byte)N;
		BoardBit.X = (byte)X;

		board = new BoardBitPn(first ? MY_PLAYER : YOUR_PLAYER);
		BoardBitPn.TTdag	= new TranspositionTable<TTPnNode, TTPnNode.KeyDepth>(M, N, TTPnNode.getTable());
		BoardBitPn.TTproved	= new TranspositionTable<TTElementProved, KeyDepth>(M, N, TTElementProved.getTable());
		TTPnNode.board = board;
		
		dbSearch = new DbSearch();		
		dbSearch.init(M, N, X, first);

		timer_duration = (timeout_in_secs - 1) * 1000;
		runtime = Runtime.getRuntime();
		lastIt_board = new BoardBitPn(first ? MY_PLAYER : YOUR_PLAYER);
		created_n = 0;

		System.out.println("\n-_-\nSTART GAME\n-_-\n");
	}

	/**
	 * Complexity: O(4X + )
	 */
	@Override
	public int selectColumn(CXBoard B) {

		timer_start = System.currentTimeMillis();
		
		// update own board.
		CXCell[] MC = B.getMarkedCells();
		if(MC.length > 0)
			board.markCheck(B.getLastMove().j);
			
		// see if new root was already visited, otherwise create it
		root = board.getEntry(COL_NULL, MC.length);
		if(root == null) {
			root = new TTPnNode(board.hash, (short)MC.length, (MC.length / 2) % 2 );
			root.setProofAndDisproof(1, 1);

			created_n++;
		}

		// debug
		System.out.println("---\n" + playerName());
		System.out.println("Opponent: " + ((B.getLastMove() == null) ? null : B.getLastMove().j) );
		System.out.println("root hash:" + board.hash + "\tdepth " + root.depth );
		board.print();
		debugVisit("before tagTree");

		// remove unreachable nodes from previous rounds
		root.setTag((root.depth / 2) % 2);		// unique tag for each round
		if(lastIt_root != null) {
			root.tagTree();

			debugVisit("before removeUnmarkedTree");

			TTPnNode.board = lastIt_board;
			lastIt_root.removeUnmarkedTree(root.getTag());
			TTPnNode.board = board;
		}

		debugVisit("before gc");
		
		runtime.gc();
		
		debugVisit("before visit");
		
		// visit
		TTElementProved root_eval = board.getEntryProved(COL_NULL, root.depth);
		if(root_eval == null) {
			visit();
			root_eval = board.getEntryProved(COL_NULL, root.depth);
		}

		lastIt_root	= root;
		lastIt_board.copy(board);
		
		int move;
		if(root_eval != null)
			move = root_eval.col();
		else {
			move = root.getMoveToBestChild();
		}
		board.markCheck(move);
		
		// debug
		System.out.println("dag_n = " + BoardBitPn.TTdag.count + "\tproved_n = " + BoardBitPn.TTproved.count + "\tcreated_n = " + created_n + "\n");
		if(root_eval != null) System.out.println("root eval: " + root_eval.col() + " "+root_eval.won()+" " +root_eval.depth_reachable + "\n");
		System.out.println("\nMy move: " + move + "\n");
		System.out.println(board.printString(0) + root.debugString(root));
		System.out.println("time,mem before return: " + (System.currentTimeMillis() - timer_start) + " " + Auxiliary.freeMemory(runtime) + "\n");
		
		return move;

	}

	@Override
	public String playerName() {
		return "PnDb delta";
	}


	//#region PN-SEARCH

		/**
		 * iterative loop for visit.
		 * Complexity: 
		 * 		one iteration:
		 * 			O( (h + 4X) + (2DbSearch + 2N**2 + (17+4X)N) + (Nh) )
		 * 			= O( 2DbSearch + (N+1)h + 2N**2 + (17+8X)N )
		 * 			= O( 2DbSearch + 2N**2 + Nh + 17N + 8XN ), with M similar to N
		 * 
		 * 		full tree:
		 * 			N*O(2Db+2N**2+N+17N+8XN) + N**2*O(2Db+2N**2+2N+17N+8XN) + ...
		 * 			... + N**N*O(2Db+2N**2+NN+17N+8XN) + ...
		 * 			... + N**(N*M)*O(2Db+2N**2+NMN+17N+8XN)
		 * 
		 * 			= (N+N**2+..+N**(NM)) * (it)			+ (N*N + N**2*2N + N**3*3N +..+ N**(NM)*NMN)
		 * 			= ( (N**(NM+1) - N) / (N-1) ) * (it)	+ (N**2 + 2N**3 + 3N**4 +..+ N**(N**2)*N**3)
		 * 			= (N**NM) * (it)						+ ( (N**(N**2+3) - N**2) / (N - 1) )
		 * 			= (N**(N**2)) * (it)					+ (N**(N**2+2))
		 * 
		 * 			= (N**(N**2)) * (2DbSearch + 2N**2 + 17N + 8XN) + (N**(N**2+2))
		 * 			= (N**(N**2)) * (2DbSearch + 2N**2 + 17N + 8XN) + (N**(N**2+2))
		 * 
		 * 			assuming a quite broad development of the tree (or, anyway, you consider an avg h).
		 * 
		 * 		H=max depth reached:
		 * 			= (N + N**2 +..+ N**H) * (it)		+ (N**2 + 2N**3 + 3N**4 +..+ N**(H+1)*HN)
		 * 			= ( (N**(H+1) - N) / (N-1) ) * (it)	+ ( (N**(H+1+1+1) - N**2) / (N - 1) )
		 * 			= O(N**H) * (it)					+ O(N**(H+2))
		 * 			= O(N**H) * (2DbSearch + 2N**2 + 17N + 8XN)					+ O(N**(H+2))
		 * 			= O(N**H * 2DbSearch + 2N**(H+2) + 17N**(H+1) + N**(H+1)8X)	+ O(N**H)
		 * 			= O(N**H * 2DbSearch + 2N**(H+2) + N**(H+2) + N**(H+2))		+ O(N**H) 	-- note(1)
		 * 			= O(N**H * 2DbSearch + 4N**(H+2))	+ O(N**H)
		 * 			= O(N**H * 2DbSearch + 4N**(H+2))
		 * 
		 * 			assuming all moves at each depth are developed, which is not true at all.
		 * 			assuming H << N (otherwise, you would have to add 1 to exponent).
		 * 			actually: updateAncestors doesn't always go to top, so it must cost less.
		 * 			note(1): 8X is probably similar to N, and 17 too (sometimes), so we can round it all, also
		 * 				considering some other overhead from constants, and round al to O(N**(NH+2)), which is absorbed
		 * 				by the other O(N**(NH+2)) anyway.
		 * 
		 * ---
		 * 			Notes: N*M is the max depth, N**h is the number of children at depth h.
		 * 			it = O(iteration) - Nh = O(2DbSearch + 2N**2 + 17N + 8XN);
		 * @return
		 */
		private void visit() {

			int visit_loops_n = 0;	// debug

			LinkedList<Integer> marked_stack		= new LinkedList<Integer>();
			LinkedList<BoardBitPn> boards_to_prune	= new LinkedList<BoardBitPn>();

			/* enhancement: keep track of current node (next to develop), instead of 
			* looking for it at each iteration, restarting from root.
			*/
			TTPnNode most_proving_node;
			while(!root.isProved() && !isTimeEnded()) {

				most_proving_node = selectMostProving(root, marked_stack);

				developNode(most_proving_node);
				
				updateAncestorsWhileChanged(most_proving_node.depth, boards_to_prune, null, true);

				resetBoard(marked_stack);
				pruneTrees(boards_to_prune);
				
				visit_loops_n++;
			}

			resetBoard(marked_stack);

			System.out.println("end of loop : n_loops = " + visit_loops_n + "\n" + debugVisit(""));
		}

		/**
		 * Evaluate a node without using the tree.
		 * This will look at the board's state or use dbSearch.
		 * 
		 * Complexity:
		 * 	-	endgame: O(1)
		 * 	-	else: O(DbSearch)
		 * @param node tree's node to eval
		 * @return true if evaluated the node, i.e. it's an ended state or db found a win sequence.
		 */
		protected boolean evaluate(TTPnNode node) {

			if(board.game_state == GameState.OPEN) {
				TTElementProved evaluation = board.getEntryProved(COL_NULL, node.depth);

				if(evaluation != null)
					return true;
				else
					return evaluateDb(node);
			}
			else {
				node.prove(board.game_state == MY_WIN);
				return true;
			}
		}

		/**
		 * Evaluate `node` according to a DbSearch.
		 * 
		 * Complexity: O(DbSearch)
		 * @param node tree's node to eval
		 * @return true if found a win.
		 */
		protected boolean evaluateDb(TTPnNode node) {

			DbSearchResult eval = dbSearch.selectColumn(board, node, timer_start + timer_duration - System.currentTimeMillis(), board.player, Operators.MAX_TIER);
			
			if(eval == null)
				return false;

			node.prove(board.player == MY_PLAYER, (short)(node.depth + (eval.threats_n * 2 - 1)), eval.winning_col);
			return true;
		}


		/**
		 * set proof and disproof; update node.mostProving; in case insert in TTwon.
		 * Complexity: 
		 * 	-	case proved: O(1)
		 * 	-	case expanded: O(2N)
		 * 	-	else: O(1)
		 * @param node
		 * @param offset offset for initProofAndDisproofNumbers, in case of `node` open and not expanded.
		 */
		protected void setProofAndDisproofNumbers(TTPnNode node, int offset) {

			if(board.game_state != GameState.OPEN) {
				// here prove() just sets its depth, and col=-1, as it's an ended state
				node.prove(board.game_state == MY_WIN);
			}
			// if node has children, set numbers according to children numbers.
			else if(node.isExpanded())
			{
				// set numbers according to children numbers
				TTElementProved entry = node.updateProofAndDisproof(board.player == MY_PLAYER ? PROOF : DISPROOF);
				//	If proved, set the final best move.
				if(entry != null) updateProved(entry);
			}
			else
				initProofAndDisproofNumbers(node, offset);
		}

		/**
		 * Select the most proving node.
		 * 
		 * Complexity: O(h + 4X), where h is the length of the path calling_node-most_proving_descndant
		 * @param node
		 * @param marked_stack
		 * @return the most proving node
		 */
		private TTPnNode selectMostProving(TTPnNode node, LinkedList<Integer> marked_stack) {
			
			if(!node.isExpanded()) return node;
			else {
				board.markCheck(node.most_proving_col);
				marked_stack.push(Integer.valueOf(node.most_proving_col));
				// careful: node.getChild uses board, but it just changed. Better use board.getEntry directly
				return selectMostProving(board.getEntry(COL_NULL, node.depth + 1), marked_stack);
			}
		}

		/**
		 * Develop a node, i.e. generate its children for the tree.
		 * 
		 * Complexity: 
		 * 		for alpha: O(2DbSearch + 13N**2)
		 * @param node
		 */
		private void developNode(TTPnNode node) {

			if(evaluate(node))
				return;
			// else if the game is still open
			generateAllChildren(node);
		}

		
		/**
		 * Generate all node's children.
		 * 
		 * Complexity: O(DbSearch + getThreatCounts + children_generation + sorting + shuffle )
		 *	<p>	= O(DbSearch + 12N**2 + N 4X + N**2 + N )
		 *	<p>	= O(DbSearch + 13N**2)
		 *	<p>		note: setProofAndDisproofNumbers can't go in expanded case from here, so it's O(1)
		 * @param node
		 */
		public void generateAllChildren(TTPnNode node) {

			/* Heuristic: implicit threat.
			 * Only inspect moves in an implicit threat, i.e. a sequence by which the opponent could win
			 * if the current player was to make a "null move".
			 * In fact, the opponent could apply such winning sequence, if the current player was to 
			 * make a move outside it, thus not changing his plans.
			 * 
			 * Actually, no moves are removed, but these results are only used to sort them.
			 * 
			 * Applied to CXGame: columns where the opponent has an immediate attacking move - which leads to a win for him -,
			 * i.e. where the attacker's move corresponds to the first free cell in the column, are for sure
			 * the most interesting (in fact, you would lose not facing them); however, other columns involved in the sequence are
			 * not ignored, since they could block the win too, and also to simplify the calculations by approximation.
			 * 
			 * note: related_cols should already contain only available, not full, columns.
			 */

			// DbSearchResult res_db = dbSearch.selectColumn(board, node, timer_start + timer_duration - System.currentTimeMillis(), Auxiliary.opponent(player), Operators.MAX_TIER);

			/* Heuristic: sorting moves (previously selected from iterated related squares) by number/scores of own threats in them
			 * (i.e., for columns, the sum of the scores in the whole column).
			 */

			// number of cols for which you can create a new node (i.e. neither already analyzed nor an ended state)
			int		available_cols_n = BoardBit.N;
			int[]	col_scores = new int[BoardBit.N],
					threats = dbSearch.getThreatCounts(board, board.player);
			int current_child, j, k;

			// get for each column the score from db, and check if they are free
			boolean won = (board.player != MY_PLAYER);
			for(j = 0; j < BoardBit.N; j++) {
				//if(res_db != null && res_db.related_squares_by_col[j] > 0) col_scores[j] = res_db.related_squares_by_col[j];
				if( board.freeCol(j) ) {
					TTElementProved entry = board.getEntryProved(j, node.depth);

					if(entry == null)
						col_scores[j] = 1;
					else {
						available_cols_n--;
						if(entry.won() == (board.player == MY_PLAYER)) {
							won = (board.player == MY_PLAYER);
							available_cols_n = 0;
							break;
						}
					}
				}
				else available_cols_n--;
			}

			/* Check if the found child nodes can prove this node.
			Otherwise, note that the found nodes won't contribute to this node's numbers. */
			if(available_cols_n == 0) {
				node.prove(won);
				return;
			}

			node.setExpanded();
			byte[] children_cols = new byte[available_cols_n];
			
			/* Fill children with such columns, and sort by threat_scores at the same time.
			 * This is made in two steps: first fill and shuffle moves, then assign parent.children related to those shuffled moves.
			 */
			for(j = 0, current_child = 0; j < BoardBit.N && current_child < available_cols_n; j++) {
				if(col_scores[j] > 0)
				{
					children_cols[current_child++] = (byte)j;
					// move back the new child in the right place
					for(k = current_child - 1; (k > 0) && (threats[children_cols[k - 1]] > threats[j]); k--)
						Auxiliary.swapByte(children_cols, k, k - 1);
				}
			}

			// shuffle children with same priority
			int start, end;
			for(start = 0; start < available_cols_n; start++) {
				for(end = start + 1;
					end < available_cols_n && threats[children_cols[end]] == threats[children_cols[start]];
					end++
				) ;
				Auxiliary.shuffleArrayRangeByte(children_cols, start, end);
				start = end;
			}
	
			// now create the children
			for(current_child = 0; current_child < available_cols_n; current_child++)
			{
				j = children_cols[current_child];
				board.markCheck(j);
				
				TTPnNode child = board.getEntry(COL_NULL, node.depth + 1);
				if(child == null) {
					child = node.createChild();
					/* Heuristic initialization: nodes without any threat should be considered less (or not at all).
					 * Proof init offset: if threats=0, BoardBit.N+1 should give enough space to prioritize other moves;
					 * otherwise, use current_child, so its an incremental number also respecting the random shuffle.
					 */
					setProofAndDisproofNumbers(child, (threats[j] == 0) ? (BoardBit.N + 1) : (current_child) );
					
					// debug
					created_n++;
				}

				board.unmark(j);
			}

		}

		/**
		 * Complexity:
		 * 		worst: O(2Nh)
		 * 			note: setProofAndDisproofNumbers always is called in expanded case, in intermediate nodes.
		 * 		avg: O(Nh) ?
		 * @param node
		 * @param game_state init to `board.game_state`
		 * @param most_proving true if it's the most_proving node, i.e. only first it
		 * @return
		 */
		public void updateAncestorsWhileChanged(int depth, LinkedList<BoardBitPn> boards_to_prune, TTElementProved caller, boolean most_proving) {
			
			TTPnNode node = board.getEntry(COL_NULL, depth);
			TTElementProved entry = board.getEntryProved(COL_NULL, depth);
			
			
			if(entry != null) {
				// just need to update deepest move (using caller), so can save time
				
				if(caller != null) {
					TTElementProved best_child = board.getEntryProved(entry.col(), entry.depth_cur);
					if(isBetterChild(entry, best_child, caller))
						entry.set(getColFromEntryProved(caller, depth), caller.depth_reachable);
					else
						return;
				} else if(most_proving) {
					updateProved(entry);
					boards_to_prune.add(new BoardBitPn(board));
				} else
					return;
			} else if(node != null)
			{
				int old_proof = node.n[PROOF], old_disproof = node.n[DISPROOF];
				setProofAndDisproofNumbers(node, 0);		// offset useless, node always expanded here 
				
				if(old_proof == node.n[PROOF] && old_disproof == node.n[DISPROOF])
					return;
				else if(node.isProved()) {
					entry = board.getEntryProved(COL_NULL, depth);
					boards_to_prune.add(new BoardBitPn(board));
				}
			} else {	// configuration doesn't exist
				return;
			}

			if(depth == root.depth)	// no recursion on root's parents
				return;
			
			// if changed
			for(int j = 0; j < BoardBit.N; j++) {
				if(board.cellState(j) == Auxiliary.opponent(board.player)) {
					board.unmark(j);
					updateAncestorsWhileChanged(depth - 1, boards_to_prune, entry, false);	// marked=null, so don't push/pop when not on most_proving path
					board.mark(j);
				}
			}

		}

		/**
		 * Complexity: O(h)
		 * @param depth where to reset to
		 * @param marked_stack
		 */
		private void resetBoard(LinkedList<Integer> marked_stack) {

			while(marked_stack.size() > 0)
				board.unmark(marked_stack.pop());
		}

		/**
		 * Remove any reference to `node`, and also to its descendants if they only had one parent.
		 * @param isroot true for tree root
		 */
		private void pruneTree(BoardBitPn board, int depth, boolean isroot) {

			TTPnNode node = board.getEntry(COL_NULL, depth);
			
			if (isroot || ( node != null && !node.hasParents() )) {

				if(node != null)
					board.removeEntry(depth);
				
				for(int j = 0; j < BoardBit.N; j++) {
					if(board.freeCol(j)) {
						board.mark(j);
						pruneTree(board, depth + 1, false);
						board.unmark(j);
					}
				}
			}
		}
		/**
		 * 
		 * @param nodes_proved
		 */
		private void pruneTrees(LinkedList<BoardBitPn> boards) {

			ListIterator<BoardBitPn> it_board = boards.listIterator();

			while(it_board.hasNext()) {
				TTPnNode.board = it_board.next();
				pruneTree(TTPnNode.board, TTPnNode.board.getDepth(), true);
			}
			boards.clear();
			TTPnNode.board = board;
		}


	//#endregion PN-SEARCH


	//#region AUXILIARY

		/**
		 * Init proof numbers to offset + current level in game tree.
		 * <p>
		 * Complexity: O(1)
		 * @param node
		 */
		protected void initProofAndDisproofNumbers(TTPnNode node, int offset) {

			int number = offset + getRelativeDepth(node) / 2 + 1;		// never less than 1
			node.setProofAndDisproof(number, number);
		}

		/**
		 * check all proved children, to find the best and deeepst/least deep path.
		 * @param entry
		 */
		private void updateProved(TTElementProved entry) {

			TTElementProved best_child = null;
			for(int j = 0; j < BoardBit.N; j++) {
				if(board.freeCol(j)) {
					TTElementProved child = board.getEntryProved(j, entry.depth_cur);
					if(isBetterChild(entry, best_child, child)) {
						best_child = child;
						entry.set(j, best_child.depth_reachable);
					}
				}
			}
		}
		/**
		 * 
		 * @param current
		 * @param test
		 * @return true if should replace current with test.
		 */
		private boolean isBetterChild(TTElementProved parent, TTElementProved current, TTElementProved test) {
			return test != null &&
				(current == null || ( parent.won() != current.won() && parent.won() == test.won() )	// set anyway
				|| current.won() == test.won() && ( test.depth_reachable > current.depth_reachable == (parent.won() == current.won()) )	// set if deeper
			);
		}
		
		private int getRelativeDepth(TTPnNode node) {
			return node.depth - root.depth;
		}

		private int getColFromEntryProved(TTElementProved child, int depth) {
			for(int j = 0; j < BoardBit.N; j++) {
				if(board.freeCol(j) && board.getEntryProved(j, depth) == child)
					return j;
			}
			return -1;
		}

		/**
		 * Complexity: O(1)
		 * @return true if it's time to end the turn
		 */
		private boolean isTimeEnded() {
			return (System.currentTimeMillis() - timer_start) >= timer_duration;
		}

		/**
		 * @return true if available memory is less than a small percentage of max memory
		 */
		protected boolean isMemoryEnded() {
			// max memory useable by jvm - (allocatedMemory = memory actually allocated by system for jvm - free memory in totalMemory)
			long freeMemory = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
			return freeMemory < runtime.maxMemory() * (5 / 100);
		}

		private String debugVisit(String log) {
			return "log: time = " + (System.currentTimeMillis() - timer_start) + "\tmems = " + runtime.maxMemory() + "\t" + runtime.totalMemory() + "\t" + runtime.freeMemory() + "\t" + Auxiliary.freeMemory(runtime) + "\n";
		}

	//#endregion AUXILIARY

}