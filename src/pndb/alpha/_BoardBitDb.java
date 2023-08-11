package pndb.alpha;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

import connectx.CXCell;
import pndb.alpha._Operators.AlignmentPattern;
import pndb.alpha._Operators.ThreatsByRank;
import pndb.alpha.threats.AlignmentsList;
import pndb.alpha.threats.BiList_ThreatPos;
import pndb.alpha.threats.ThreatApplied;
import pndb.alpha.threats.ThreatCells;
import pndb.alpha.threats.ThreatPosition;
import pndb.alpha.threats.ThreatCells.USE;
import pndb.constants.Auxiliary;
import pndb.constants.CellState;
import pndb.constants.GameState;
import pndb.constants.MovePair;
import pndb.constants.Constants.BoardsRelation;
import pndb.structures.BiList.BiNode;




public abstract class _BoardBitDb<S extends _BoardBitDb<S, BB>, BB extends _BoardBit<BB>> extends _BoardBit<BB> implements IBoardBitDb<S, BB> {
	

	//#region CONSTANTS
		
		/* order: clockwise, from right, with 0 above.
		 */
		public static final MovePair DIRECTIONS[] = {
			new MovePair(0, 1),
			new MovePair(1, 1),
			new MovePair(1, 0),
			new MovePair(1, -1),
			new MovePair(0, -1),
			new MovePair(-1, -1),
			new MovePair(-1, 0),
			new MovePair(-1, 1)
		};
		/* number of absolute directions */
		protected static final int DIR_ABS_N = 4;
		/* indexes for alignments_by_dir */
		protected static int	DIR_IDX_HORIZONTAL	= 0,
								DIR_IDX_DIAGRIGHT 	= 1,
								DIR_IDX_VERTICAL	= 2,
								DIR_IDX_DIAGLEFT	= 3;

		protected static final MovePair MIN = new MovePair(0, 0);
		protected final MovePair MAX;
		protected final int MAX_SIDE;
		/* alignments */
		protected static int MIN_MARKS;

		public static byte MY_PLAYER;

		protected static _Operators OPERATORS;

	//#endregion CONSTANTS
		
	public LinkedList<ThreatApplied> markedThreats;

	/*
	 * alignments_by_direction contains an array of all alignments for each row in a certain direction,
	 * for each direction.
	 * The direction corresponding to each index d, is DIRECTIONS[d%4].
	 * Generally, it refers to the column index, except for the horizontal direction where it refers to the row index
	 * (for diagonal, imagine to extend the board such that all diagonals reach row 0).
	 *
	 * horizontal:	dimension=M,		indexed: by row
	 * vertical:	dimension=N,		indexed: by col
	 * dright:		dimension=M+N-1,	indexed: by start of diagonal on the top row, i.e. from -M+1 to N-1
	 * dleft:		dimension=M+N-1,	indexed: by start of diagonal on the top row, i.e. from 0 to N+M-1
	 */
	protected AlignmentsList[] alignments_by_dir;

	protected static int[] alignments_by_dir_sizes;



	protected final byte[] Player_byte 	= {CellState.P1, CellState.P2};
	protected int currentPlayer;		// currentPlayer plays next move (= 0 or 1)

	/* implementation
	*/
	// for findAlignmentsInDirection()
	protected static MovePair	c1				= new MovePair(),
								c2				= new MovePair(),
								end_c1			= new MovePair(),
								end_c2			= new MovePair(),
								c_it			= new MovePair(),
								threat_start	= new MovePair(),
								threat_end		= new MovePair();
	
	// Debug
	protected int count = 0;
	protected int found = 0;
	protected static boolean DEBUG_ON		= true;
	protected static boolean DEBUG_PRINT	= false;
	protected static FileWriter file;
  



	/**
	 * Complexity: O(3N + 3(M+N)) = O(3M+4N)
	 * @param M
	 * @param N
	 * @param X
	 */
	protected _BoardBitDb(int M, int N, int X, _Operators operators) {
		super(M, N, X);
		alignments_by_dir_sizes = new int[]{M, M + N - 1, N, M + N - 1};
		
		_BoardBitDb.OPERATORS = operators;
		
		MAX = new MovePair(M, N);
		MAX_SIDE = Math.max(M, N);
		MIN_MARKS = X - operators.MARK_DIFF_MIN;
		currentPlayer = 0;
		
		initAlignmentStructures();
		markedThreats = new LinkedList<ThreatApplied>();
	}

	/**
	 * Complexity: O(3N + 3(M+N) + N) = O(3M + 7N)
	 * @param B
	 */
	protected _BoardBitDb(BB B, _Operators operators) {
		super(B.M, B.N, B.X);
		alignments_by_dir_sizes = new int[]{M, M + N - 1, N, M + N - 1};
		
		_BoardBitDb.OPERATORS = operators;
		
		MAX = new MovePair(M, N);
		MAX_SIDE = Math.max(M, N);
		MIN_MARKS = X - _BoardBitDb.OPERATORS.MARK_DIFF_MIN;
		currentPlayer = 0;
		
		initAlignmentStructures();
		markedThreats = new LinkedList<ThreatApplied>();
		
		copy(B);
	}
	
	/**
	 * Complexity: O(3N + 2*3(M+N) + B.markedThreats.length + N) = O(3M + 10N + B.markedThreats.length) -- avg_threats
	 * @param B
	 * @param copy_threats
	 */
	protected _BoardBitDb(S B, boolean copy_threats, _Operators operators) {
		super(B.M, B.N, B.X);
		alignments_by_dir_sizes = new int[]{M, M + N - 1, N, M + N - 1};

		_BoardBitDb.OPERATORS = operators;
		
		MAX = new MovePair(M, N);
		MAX_SIDE = Math.max(M, N);
		MIN_MARKS = X - _BoardBitDb.OPERATORS.MARK_DIFF_MIN;
		currentPlayer = B.currentPlayer;
		hash = B.hash;
		
		if(copy_threats) copyAlignmentStructures(B);
		else initAlignmentStructures();
		markedThreats = new LinkedList<ThreatApplied>(B.markedThreats);	//copy marked threats
		
		copy(B);
	}

	/**
	 * Complexity: O(N*COLSIZE(M)) = O(N)
	 * @param B
	 */
	public void copy(S B) {

		// copy all
		for(int j = 0; j < N; j++) {
			for(int i = 0; i < COL_SIZE(M); i++) {
				board[j][i]			= B.board[j][i];
				board_mask[j][i]	= B.board_mask[j][i];
			}
			free[j] = B.free[j];
		}

		free_n = B.free_n;

		// debug
		count = B.count;
	}
	

	//#region BOARD

		/**
		 * Mark an arbitrary cell. Use with caution.
		 * Increases free[] anyway.
		 * Complexity:  O(4 * AVG_THREATS_PER_DIR_PER_LINE)
		 * @param col
		 * @param player
		 * @return GameState
		 */
		private void markAny(int i, int j, byte player) {
			hash = TT.getHash(hash, i, j, Auxiliary.getPlayerBit(player));

			board[j][i / BITSTRING_LEN]			|= (player & 1) << (i % BITSTRING_LEN);	// =1 for CellState.ME
			board_mask[j][i / BITSTRING_LEN]	|= 1 << (i % BITSTRING_LEN);
			free[j]++;
			free_n--;

			removeAlignments(new MovePair(i, j), Auxiliary.opponent(player));
		}
		/**
		 * Mark cell; also remove opponent's alignments, but doesn't find new ones.
		 * Complexity: O(4X + 4 AVG_THREATS_PER_DIR_PER_LINE)
		 * @param i
		 * @param j
		 * @param player
		 */
		protected void mark(int i, int j, byte player) {
			markCheck(j, player);
			removeAlignments(new MovePair(i, j), Auxiliary.opponent(player));
		}
		/**
		 * Complexity: O(4X + 4 AVG_THREATS_PER_DIR_PER_LINE)
		 */
		public void mark(int j, byte player) {
			mark(free[j], j, player);
		}
		
		//public void markCell(MovePair cell) {markCell(cell.i(), cell.j(), Player[currentPlayer]);}
		/**
		 * Complexity: O(4X + 4 AVG_THREATS_PER_DIR_PER_LINE)
		 */
		@Override
		public void mark(MovePair cell, byte player) {mark(cell.i, cell.j, player);}
		/**
		 * Complexity: O(16 (X + AVG_THREATS_PER_DIR_PER_LINE)),
		 * 		considering that it's usually used for marking threats.
		 */
		@Override
		public void markMore(MovePair[] cells, byte player) {
			for(MovePair c : cells) mark(c.i, c.j, player);
		}
		/**
		 * Complexity: O(16*AVG_THREATS_PER_DIR_PER_LINE + 4X),
		 * 		as only one move is for the atttacker.
		 */
		@Override
		public void markThreat(MovePair[] related, int atk_index) {

			for(int i = 0; i < related.length; i++) {
				if(i == atk_index) {
					markAny(related[i].i, related[i].j, Player_byte[currentPlayer]);		// markAny, for vertical
					check(related[i].i, related[i].j, Player_byte[currentPlayer]);			// only check for attacker
				} else
					markAny(related[i].i, related[i].j, Player_byte[1 - currentPlayer]);	// markAny, for vertical
			}
		}

		/**
		 * Complexity: 
		 *  -	best case: O(4X)
		 * 	-	worst and avg: O(4 findAlignmentsInDirection )
		 * @param cell
		 * @param max_tier
		 * @param dir_excluded
		 * @param caller
		 */
		protected void checkAlignments(MovePair cell, int max_tier, int dir_excluded, String caller) {

			if(isWinningMove(cell.i, cell.j))
				game_state = cell2GameState(cell.i, cell.j);
			else {
				findAlignments(cell, cellState(cell), max_tier, null, null, true, dir_excluded, caller + "checkOne_");
				if(free_n == 0 && game_state == GameState.OPEN) game_state = GameState.DRAW;
			}
		}
		/**
		 * Complexity: 
		 *  -	best case (single cell): O(checkAlignments) 	-- the other one
		 * 	-	worst case (more cells): O( 4*(checkAlignments + OfindAlignmentsInDirection) )
		 * 			= O( 8*findAlignmentsInDirection )
		 * 			as usually used for threats
		 */
		public void checkAlignments(MovePair[] cells, int max_tier, String caller) {
			if(cells.length == 1) {
				checkAlignments(cells[0], max_tier, -1, caller + "checkArray_");
			} else {
				MovePair dir = cells[0].getDirection(cells[1]);
				int dir_index = dirIdx_fromDir(dir);

				for(int i = 0; i < cells.length && game_state == GameState.OPEN; i++)
					checkAlignments(cells[i], max_tier, dir_index, caller + "checkArray_");

				if(game_state == GameState.OPEN) {
					for(int i = 1; i < cells.length; i++)
						findAlignmentsInDirection(cells[i], cells[i-1], cellState(cells[i]), dir_index, max_tier, null, null, true, caller + "checkArray_");
				}
				//update gameState
				if(free_n == 0 && game_state == GameState.OPEN) game_state = GameState.DRAW;
			}
		}

	//#endregion BOARD


	//#region DB_SEARCH

		/**
		 * Complexity: (all assuming check_threats=true, otherwise don't consider that)
		 * 	-	case atk: O(16X * 4 + CheckAlignments ) = O(64X CheckAlignments)
		 * 	-	case def: O(64X * 4) = O(256X)
		 * 			( + CheckAlignments), performed in DbSearch.defensiveVisit
		 * 	-	case bth: O(16 * 4 + 4X + CheckAlignments ) = O(64 + 4X + CheckAlignments)
		 * 	note: using getCopy(false), no threat to remove.
		 * @param threat as defined in Operators
		 * @param atk attacker's move index in threat
		 * @param use as def in Operators
		 * @param threats wether to update alignments and threats for this board
		 * @return :	a new board resulting after developing this with such threat (dependency stage);
		 * 				the new board only has alignment involving the newly marked cells
		 */
		public S getDependant(ThreatCells threat, int atk, USE use, int max_tier, boolean check_threats) {
			
			S res = null;
			
			switch(use) {
				//used for...
				case ATK:
				/*
					res = getCopy(false);
					MovePair cell = threat.related[threat.nextAtk(atk)];
					res.mark(cell.i, cell.j, Player_byte[currentPlayer]);
					if(check_threats) res.checkAlignments(cell, max_tier, -1, "dep");
				*/
					break;
					//used for init defensive visit (marks defensive cells as own)
				case DEF:
					res = getCopy(true);
					//if there exist any defensive moves
					if(threat.related.length > 1)
					res.markMore(threat.getDefensive(atk), Player_byte[currentPlayer]);
					// add this threat and check_alignments are done in getDefensiveRoot
					break;
					//used for dependency stage
				case BTH:
					res = getCopy(false);
					res.markThreat(threat.related, atk);
					res.addThreat(threat, atk, Player_byte[currentPlayer]);
					if(check_threats) res.checkAlignments(threat.related[atk], max_tier, -1, "dep");
			}
			return res;
		}
		
		/**
		 * Only checks for alignments not included in the union of A's and B's alignments, i.e. those which involve at  least one cell only present in A and one only in B.
		 * Complexity:
		 * 		with mc:
		 *	 		O(marked_threats.length + N**2 + 13N)	+ O(B.marked_threats.length * (4+4+4X + 4 AVG_THREATS_PER_DIR_PER_LINE) )	+ O(findAllCombinedAlignments)
		 *			= O(marked_threats.length + N**2 + 13N) + O(B.marked_threats.length * (8 + 4X + 4 avg_threats_per_dir_per_line) )	+ O(6N * O(findAlignmentsInDirection) )
		 *			= O(marked_threats.length + N**2		+ 4 B.marked_threats.length * (X + avg_threats_per_dir_per_line)			+ 6N O(findAlignmentsInDirection) )
		 *			worst case for findAlignmentsInDirection:
		 *				 O(marked_threats.length + N**2		+ 4 B.marked_threats.length * (X + avg_threats_per_dir_per_line) + 432 NX**2 )
		 * 		no mc:
		 *			= O(marked_threats.length +				+ 4 B.marked_threats.length * (X + avg_threats_per_dir_per_line)			+ 6N O(findAlignmentsInDirection) )
		 *			worst case for findAlignmentsInDirection:
		 *				 O(marked_threats.length 			+ 4 B.marked_threats.length * (X + avg_threats_per_dir_per_line) + 432 NX**2 )
		 */
		public S getCombined(S B, byte attacker, int max_tier) {

			S res = getCopy(true);

			for(ThreatApplied athreat : B.markedThreats) {
				if(isUsefulThreat(athreat, attacker)) {
					//mark other board's threat on res
					for(int i = 0; i < athreat.threat.related.length; i++) {
						MovePair c = athreat.threat.related[i];

						if(cellFree(c.i, c.j)) {
							if(i == athreat.related_index) res.mark(c, athreat.attacker);
							else res.mark(c, Auxiliary.opponent(athreat.attacker));
						}
					}
					//add threats
					res.addThreat(athreat);
				}
			}

			//re-calculate alignments
			res.addAllCombinedAlignments(B, attacker, max_tier);

			return res;
		}

		@Override
		/**
		 * Complexity: O(1)
		 */
		public BoardsRelation validCombinationWith(S B, byte attacker) {
			return null;
		}
		
	//#endregion DB_SEARCH


	//#region AUXILIARY
	
		/**
		 * Complexity: O(1)
		 */
		public void addThreat(ThreatCells threat, int atk, byte attacker) {
			// from when appliedThreat was different:
			//MovePair[] def = new MovePair[threat.related.length - 1];
			//for(int i = 0; i < atk; i++) def[i] = threat.related[i];
			//for(int i = atk + 1; i < threat.related.length; i++) def[i - 1] = threat.related[i];
			ThreatApplied at = new ThreatApplied(threat, atk, attacker);
			markedThreats.addLast(at);
		}
		/**
		 * Complexity: O(1)
		 * @param a_threat
		 */
		public void addThreat(ThreatApplied a_threat) {
			markedThreats.addLast(a_threat);
		}

		/**
		 * returns index in lines_per_dir to the line related to this direction
		 * Complexity: O(1)
		 */
		protected int dirIdx_fromDir(MovePair dir) {
			for(int d = 0; d < DIR_ABS_N; d++)
				if(dir.equals(DIRECTIONS[d]) || dir.equals(DIRECTIONS[d + 4]))
					return d;
			return -1;	// useless
		}

		//#region ALIGNMENTS

			/**
			 * Complexity: O(4 * AVG_THREATS_PER_DIR_PER_LINE)
			 * @param center
			 * @param player
			 */
			protected void removeAlignments(final MovePair center, byte player) {

				// debug
				//if(DEBUG_ON)
				//	System.out.println("\nremoveAlignments START:");
				// foreach direction
				for(int d = 0; d < DIR_ABS_N; d++) {
					BiList_ThreatPos alignments_in_line = alignments_by_dir[d].get(getIndex_for_alignmentsByDir(DIRECTIONS[d], center));

					if(alignments_in_line != null) {
						BiNode<ThreatPosition>	p = alignments_in_line.getFirst(player),
												p_next;
						
						while(p != null) {
							p_next = p.next;

							// debug
							if(DEBUG_PRINT) System.out.println("remove " + p.item);

							if(center.inBetween_included(p.item.start, p.item.end))
								removeAlignmentNode(alignments_in_line, p, player);

							p = p_next;
						}
					}
				}

			}


			/**
			 * Return the first/last (depending on parameter `find_first`) cell containing `target`, moving from `start`, with increment `incr`,
			 * for a max distance of `max_distance` (excluded).
			 * Stop if finds a cell containing `stop_value` (excluded), or if reaches `stop_cell` (excluded), or if `only_target` is true and finds
			 * something different from target (in case of `only_target`, `stop_value` is not considered).
			 * 
			 * Complexity: variable, max O(max{M,N})
			 * @param res where the result is stored.
			 * @param start
			 * @param incr
			 * @param stop_cell
			 * @param max_distance
			 * @param target
			 * @param stop_value
			 * @param only_target
			 * @param find_first
			 * @param only_valid
			 * @param dir_index
			 * @return the distance found
			 */
			protected int _findOccurrenceUntil(MovePair res, final MovePair start, MovePair incr, MovePair stop_cell, int max_distance, byte target, byte stop_value, boolean only_target, boolean find_first, boolean only_valid, int dir_index) {
				
				int distance;

				for( c_it.reset(start), distance = 0
					; distance < max_distance && c_it.inBounds(MIN, MAX) && !c_it.equals(stop_cell)
					&& ((!only_target && cellState(c_it) != stop_value) || (only_target && cellState(c_it) == target))
					&& ( (dir_index == DIR_IDX_VERTICAL) || (free[c_it.j] == c_it.i) || (!only_valid && (free[c_it.j] < c_it.i)) || (target != CellState.FREE && cellState(c_it) == target) )
					; c_it.sum(incr), distance++
				) {
					if(cellState(c_it) == target) {
						res.reset(c_it);
						if(find_first) break;
					}
				}

				return distance;
			}
			
			/**
			 * Find all alignments involving at least one cell between the aligned first, second.
			 * Also, if check1 and check2 != null, checks that the new threat involves at least one cell only present in 1, and one only in 2.
			 * 
			 * 1 -	the cells whose alignments will change are those at max distance K-1 from this;
			 * 2 -	considering the algorithm is correct, we assume they already have associated, if already
			 * 		existed, alignments from K-MIN_SYM_LINE to K-1 symbols (if existed of K, the game would be ended);
			 * 3 -	that said, we will only need to increase existing alignments by 1 symbol, and to add
			 * 		new alignments of K-MIN_SYM_LINE.
			 * HOWEVER, this will be a future enhancement: for now, the function simply deletes and recreates all.  
			 * 
			 * Complexity: variable, worst cases (with worst approximation):
			 * 	-	whole line: O(max{M,N}**2 * 6 * 3) = O(18 max{M,N}**2), 
			 * 			where 6 is the max number of alignments to check in a tier,
			 * 			and 3 is the maximum number of free cells on one side of an alignment (i.e. number of iteration for before, after).
			 *  -	single cell: O((2X)**2 * 6 * 3)
			 * 				= O(72 X**2),
			 * 			as c1,c2 get at most X cells distant from center, on each side.
			 *  -	sequence: O( (2X+(second-first))**2 * 6*3 )
			 * 				= O(18(2X+second-first)**2 )
			 * 				= O(18(3X)**2 ) if second-first==X
			 * 				= O(162X**2 )
			 * @param first
			 * @param second
			 * @param player
			 * @param dir_index
			 * @param max_tier
			 * @param check1
			 * @param check2
			 * @param only_valid if true, only search for immediately applicable threats, i.e. free[j] = i, for each (i,j) in threat
			 * @param caller
			 */
			protected void findAlignmentsInDirection(MovePair first, MovePair second, byte player, int dir_index, int max_tier, _BoardBitDb<S, ?> check1, _BoardBitDb<S, ?> check2, boolean only_valid, String caller) {

				String filename = "debug/db2/" + player + "_" + caller + count + "_" + (int)(Math.random() * 99999) + "_.txt";
				count++;
				
				try {

					byte opponent						= Auxiliary.opponent(player);
					MovePair dir						= DIRECTIONS[dir_index];
					MovePair dir_neg					= DIRECTIONS[(dir_index + 4) % 8];

					// swap first, second if not first->second in same direction as dir
					if(!dir.equals( first.getDirection(second)) ) {
						MovePair tmp = first;
						first	= second;
						second	= tmp;
					}
					/*	
					MovePair c*:
						center = starting cell, end_c* = after last to check for c* (excluded), c1,c2 = iterators (first and last in line to check)
						c1 goes from center-MAX_LEN to center, c2 from c1 to center+MAX_LEN
					*/

					end_c1.reset(second).clamp_diag(MIN, MAX, dir, OPERATORS.MAX_OUT_ONE_SIDE);
					end_c2.reset(second).clamp_diag(MIN, MAX, dir, X + OPERATORS.MAX_OUT_ONE_SIDE);
					
					int	lined,			// alignment length, i.e. max distance. It's always c2-c1+1
						marks,			// marks in alignment
						in,				// inside alignment
						before,			// 
						after;			// 
					int tier;
					
					boolean	c2_passed_endc1 = false, c1_reset_to_c2 = false,
						found1, found2,			// checks if found in check1, check2
						to_check = (check1 != null && check2 != null);
						
					// debug
					int found = 0;
					if(DEBUG_ON) {
						file = new FileWriter(filename);
						file.write(printString(0) + "\naddAlignments START, for player " + player + ", moves " + first + " " + second + " dir: " + DIRECTIONS[dir_index] + ", end_c1/c2:" + end_c1 + " " + end_c2 + ", onlyvalid:" + only_valid + ":\n");
					}
					if(DEBUG_PRINT) System.out.println(printString(0) + "\naddAlignments START, for player " + player + ", moves " + first + " " + second + " dir: " + DIRECTIONS[dir_index] + ", end_c1/c2:" + end_c1 + " " + end_c2 + ", onlyvalid:" + only_valid + ":\n");
						
					for( _findOccurrenceUntil(c1, c1.reset(first), dir_neg, MAX, X + OPERATORS.MAX_OUT_ONE_SIDE - 1, player, opponent, false, false, only_valid, dir_index)	// find furthest c1 back, from center
						; !c1.equals(end_c1) && !(c2_passed_endc1 && c1_reset_to_c2)
						&& cellState(c1) == player
						; _findOccurrenceUntil(c1, c1.sum(dir), dir, end_c1, MAX_SIDE, player, CellState.NULL, false, true, only_valid, dir_index)							// find first player cell, before end_c1
					) {
						lined = 0;					// always c2-c1+1.
						marks = 0;					// we are assured c1 (thus c2) contains player, or the loop would end.
						in = before = after = 0;	// in = lined - marks = 0
						found1 = found2 = false;
						c2_passed_endc1 = c1_reset_to_c2 = false;
					
						for( c2.reset(c1)
							; !c2.equals(end_c2)
							; c2.sum(dir)
						) {

							// check boards
							if(to_check) {
								if		(!check1.cellFree(c2.i, c2.j) && check2.cellFree(c2.i, c2.j))	found1 = true;
								else if	(!check2.cellFree(c2.i, c2.j) && check1.cellFree(c2.i, c2.j))	found2 = true;
							}

							// update alignment values
							if(c2.equals(end_c1)) c2_passed_endc1 = true;
							lined++;
							if (cellFree(c2.i, c2.j)) {
								if(!only_valid || dir_index == DIR_IDX_VERTICAL || free[c2.j] == c2.i) {
									in++;
									continue;										// c2 must be player's
								} else {
									if(c2.getDistanceAbs(c1) < MIN_MARKS + 2){
										c1.reset(c2);								// skip useless c1's
										c1_reset_to_c2 = true;
									}
									break;
								}
							}
							else if	(cellState(c2) == player) marks++;	
							else {													// opponent: c1++
								if(c2.getDistanceAbs(c1) < MIN_MARKS + 2) {
									c1.reset(c2);
									c1_reset_to_c2 = true;
								}
								break;
							}

							// debug
							if(DEBUG_ON) file.write("\t\t\t\tvalues: " + c1 + "->" + c2 + " : " + lined + ", " + marks + ", " + in + "\n");
							if(DEBUG_PRINT) System.out.println("\t\t\t\tvalues: " + c1 + "->" + c2 + " : " + lined + ", " + marks + ", " + in + "\n");

							if(marks > X)
								break;
							
							tier = X - marks;
							if(marks < MIN_MARKS || (to_check && !(found1 && found2)) || tier > max_tier)
								continue;

							// check alignments, foreach alignment of mark marks
							for(byte threat_code : OPERATORS.alignmentCodes(tier))
							{
								AlignmentPattern alignment = OPERATORS.alignmentPatterns(tier).get((int)threat_code);

								// debug
								if(DEBUG_ON) file.write("\t\t\t\t\tstart checking alignment = " + alignment + "\n");
								if(DEBUG_PRINT) System.out.println("\t\t\t\t\tstart checking alignment = " + alignment + "\n");

								//if (inner alignment conditions)
								if(alignment.isCompatible(X, lined, marks, in)) {

									if(tier == 0) {
										game_state = cell2GameState(first.i, first.j);
										return;
									}

									//add to structures
									for( before = _findOccurrenceUntil(threat_start.reset(c1), c1.getDiff(dir), dir_neg, MAX, alignment.out - alignment.mnout, CellState.FREE, CellState.NULL, true, false, only_valid, dir_index),
										after = _findOccurrenceUntil(threat_end.reset(c2), c2.getSum(dir), dir, MAX, alignment.out - before, CellState.FREE, CellState.NULL, true, false, only_valid, dir_index)
										; before >= alignment.mnout && after >= alignment.mnout && before + after >= alignment.out																// alignment conditions
										&& threat_end.inBounds(MIN, MAX) && (after == 0 || (cellFree(threat_end.i, threat_end.j) && (!only_valid || dir_index == DIR_IDX_VERTICAL || free[threat_end.j] == threat_end.i) ) )	// in bounds and player's cells
										; after++, before--, threat_start.sum(dir), threat_end.sum(dir)
									) {
										ThreatPosition threat_pos = new ThreatPosition(threat_start, threat_end, threat_code);
										addAlignment(threat_pos, dir_index, player);

										// debug
										found++;
										if(DEBUG_ON) file.write("found threat: " + threat_start + "_( " + c1 + "->" + c2 + ") _" + threat_end + " : " + threat_pos + "before, after=" + before + " " + after + "\n");
										if(DEBUG_PRINT) System.out.println("found threat: " + threat_start + "_( " + c1 + "->" + c2 + ") _" + threat_end + " : " + threat_pos +  "before, after=" + before + " " + after + "\n");
									}
								}
							}

						}
					}

					// debug
					if(DEBUG_ON) {
						file.write("addAlignments END;\n");
						file.close();
						if(found == 0) {
							File todel = new File(filename);
							todel.delete();
						}
					}
				} catch (IOException e) {}
				catch (Exception e) {
					try{file.write("\n\nERROR\n\n");file.close();} catch(IOException io) {}
					throw e;
				}
				
			}

			/**
			 * Find alignments for a cell in all directions.
			 * Complexity: O(4 O(findAlignmentsInDirection))
			 * -	whole line:		O(72 max{M,N})
			 * -	single cell		O(288 X**2)
			 * -	sequence:		O((72(2X+second-first)**2 )
			 */
			protected void findAlignments(final MovePair cell, final byte player, int max_tier, _BoardBitDb<S, ?> check1, _BoardBitDb<S, ?> check2, boolean only_valid, int dir_excluded, String caller) {
				for(int d = 0; d < DIR_ABS_N; d++) {
					if(d != dir_excluded)
						findAlignmentsInDirection(cell, cell, player, d, max_tier, check1, check2, only_valid, caller + "find_");
				}
			}

			/**
			 * Find all alignments for a player.
			 * Complexity: O( (M+N+(M+N-1)*2) * O(findAlignmentsInDirection) ) = O( 3(M+N) * O(findAlignmentsInDirection) )
			 * @param player
			 * @param max_tier
			 */
			public void findAllAlignments(byte player, int max_tier, boolean only_valid, String caller) {

				MovePair start, end;
				for(int d = 0; d < DIR_ABS_N; d++)
				{
					for(start = nextStartOfRow_inDir(null, d), end = new MovePair();
						start.inBounds(MIN, MAX);
						start = nextStartOfRow_inDir(start, d)
					) {
						end.reset(start).clamp_diag(MIN, MAX, DIRECTIONS[d], Math.max(M, N));
						findAlignmentsInDirection(start, end,  player, d, max_tier, null, null, only_valid, caller + "all_");
					}
				}
			}
			
			/**
			 * Same as findAllAlignments, just had problems with types.
			 * Complexity: O(findAllAlignments) = O(3(M+N) * O(findAlignmentsInDirection) )
			 * @param B
			 * @param player
			 * @param max_tier
			 */
			protected void addAllCombinedAlignments(S B, byte player, int max_tier) {
				
				MovePair start, end;
				for(int d = 0; d < DIR_ABS_N; d++)
				{
					for(start = nextStartOfRow_inDir(null, d), end = new MovePair();
						start.inBounds(MIN, MAX);
						start = nextStartOfRow_inDir(start, d)
					) {
						end.reset(start).clamp_diag(MIN, MAX, DIRECTIONS[d], MAX_SIDE);
						findAlignmentsInDirection(start, start, player, d, max_tier, this, B, true, "combined_");
					}
				}
			}

			/**
			 * Index for alignments_by_direction.
			 * Complexity: O(1)
			 * @return the index where, in alignments_by_direction, is contained position in direction dir
			 */
			protected int getIndex_for_alignmentsByDir(MovePair dir, MovePair position) {
				if(dir.i == 0)				return position.i;				//horizontal
				else if(dir.j == 0)			return position.j;				//vertical
				else if(dir.i == dir.j)		return dir.j - dir.i + M - 1;	//dright
				else return dir.i + dir.j;									//dleft
			}

			/**
			 * Complexity: 
			 * -	one call: O(1)
			 * -	single iteration: M, N, M+N-1, M+N-1 for each direction (col, row, diag, anti-diag)
			 * -	all directions: O(M + N + M+N-1 + M+N-1) = O(3MN)
			 * @param start
			 * @param dir_index
			 * @return
			 */
			protected MovePair nextStartOfRow_inDir(MovePair start, int dir_index) {
				if(start == null) {
					if(dir_index == DIR_IDX_DIAGRIGHT) return new MovePair(M - 1, 0);	//dright
					else return new MovePair(0, 0);
				}

				if(dir_index == DIR_IDX_HORIZONTAL)
						return start.reset(start.i + 1, start.j);
				else if(dir_index == DIR_IDX_DIAGLEFT) {
						if(start.j == N - 1)	return start.reset(start.i + 1, start.j);
						else					return start.reset(start.i, start.j + 1);
				} else if(dir_index == DIR_IDX_VERTICAL)
						return start.reset(start.i, start.j + 1);
				else {	// DIR_IDX_DIAGRIGHT
					if(start.i == 0)	return start.reset(start.i, start.j + 1);
					else				return start.reset(start.i - 1, start.j);
				}
			}

			/**
			 * Complexity: 
			 * 	-	worst (return false): O(3(M+N))
			 * @param player
			 * @return true if there are valid alignments (calculated before, with proper max_tier)
			 */
			public boolean hasAlignments(byte player) {

				for(int i = 0; i < alignments_by_dir.length; i++) {
					for(int j = 0; j < alignments_by_dir[i].size(); j++) {
						BiList_ThreatPos t = alignments_by_dir[i].get(j);
						if(t != null && !t.isEmpty(player)) return true;
					}
				}
				return false;
			}

			/**
			 * Complexity: O(4), where 4 is the max number of cells in an appliedThreat
			 * @param athreat
			 * @param attacker
			 * @return true if the AppliedThreat is compatible with this board, and useful, i.e. adds at least one attacker's threat.
			 */
			protected boolean isUsefulThreat(ThreatApplied athreat, byte attacker) {

				byte state, state_athreat;
				boolean useful = false;
				
				for(int i = 0; i < athreat.threat.related.length; i++) {
					state			= cellState(athreat.threat.related[i]);
					state_athreat	= (i == athreat.related_index) ? athreat.attacker : Auxiliary.opponent(athreat.attacker);
					if(state == CellState.FREE) {						//compatible
						if(state_athreat == attacker) useful = true;	//useful
					} else if(state != state_athreat) return false;		//not compatible
				}

				return useful;
			}

			@Override
			/**
			 * Complexity: worst: O(3(M+N) * AVG_THREATS_PER_DIR_PER_LINE * AVG(O(Operators.applied)) ) = O(3X(M+N))
			 */
			public ThreatsByRank getApplicableOperators(byte attacker, int max_tier) {

				byte defender		= Auxiliary.opponent(attacker);
				ThreatsByRank res	= OPERATORS.new ThreatsByRank();

				for(AlignmentsList alignments_by_row : alignments_by_dir) {
					for(BiList_ThreatPos alignments_in_line : alignments_by_row) {
						if(alignments_in_line != null) {
							
							BiNode<ThreatPosition> alignment = alignments_in_line.getFirst(attacker);
							if(alignment != null && OPERATORS.tier(alignment.item.type) <= max_tier) {
								do {
									ThreatCells cell_threat_operator = OPERATORS.applied(this, alignment.item, attacker, defender);
									
									if(cell_threat_operator != null) res.add(cell_threat_operator);
									alignment = alignment.next;

								} while(alignment != null);
							}
						}
					}
				}

				return res;
			}

			/**
			 * Complexity: O(3(M+N) * AVG_THREATS_PER_DIR_PER_LINE ) = O(3(M+N))
			 */
			@Override
			public int[] getThreatCounts(byte player) {

				setPlayer(player);
				findAllAlignments(player, OPERATORS.TIER_MAX, false, "selCol_");
		
				int[] threats_by_col = new int[N];

				for(int d = 0; d < DIR_ABS_N; d++) {
					for(BiList_ThreatPos alignments_in_line : alignments_by_dir[d]) {
						if(alignments_in_line != null) {
							
							BiNode<ThreatPosition> p = alignments_in_line.getFirst(player);
							while(p != null) {
								// if in same col
								if(p.item.start.j == p.item.end.j)
									threats_by_col[p.item.start.j] += (p.item.start.getDistanceAbs(p.item.end) + 1) * OPERATORS.indexInTier(p.item.type);
								
								else {
									for(int j = p.item.start.j; j <= p.item.end.j; j++)
										threats_by_col[j] += OPERATORS.indexInTier(p.item.type);
								}
		
								p = p.next;
							}
						}
					}
				}
		
				return threats_by_col;
			}

		//#endregion ALIGNMENTS
		
	//#endregion AUXILIARY


	//#region GET
	
		/**
		 * Complexity: O(1)
		 */
		@Override
		public int getCurrentPlayer() {return currentPlayer;}
	
		/**
		 * Complexity: O(1)
		 */
		@Override
		public void setPlayer(byte player) {currentPlayer = (player == this.Player_byte[0]) ? 0 : 1;}

		/**
		 * Complexity: O(N)
		 */
		@Override
		public int getMC_n() {
			int MC_n = 0;
			for(int j = 0; j < N; j++) MC_n += free[j];
			return MC_n;
		}
		/**
		 * Complexity: O(1)
		 */
		@Override
		public CXCell getMarkedCell(int i) {return null;}
		/**
		 * Complexity: O(1)
		 */
		@Override
		public LinkedList<ThreatApplied> getMarkedThreats() {return markedThreats;}

		/**
		 * Complexity: O(1)
		 */
		@Override
		public long getHash() {return hash;}
	
	//#endregion GET
	
	//#region SET

		/**
		 * Helper, for adding an alignment to the structures.
		 */
		protected void addAlignment(ThreatPosition alignment, int dir_index, byte player) {
			alignments_by_dir[dir_index].add(player, getIndex_for_alignmentsByDir(DIRECTIONS[dir_index], threat_start), alignment);
		}
		/**
		 * Helper, for adding an alignment to the structures.
		 */
		protected void removeAlignmentNode(BiList_ThreatPos alignments_in_line, BiNode<ThreatPosition> node, byte player) {
			alignments_in_line.remove(player, node);
		}
	
	//#endregion SET

	//#region INIT

		/**
		 * Complexity: O(M + N + M+N-1 + M+N-1 + 4) = O(3(M+N))
		 */
		protected void initAlignmentStructures() {
			alignments_by_dir		= new AlignmentsList[]{
				new AlignmentsList(M),			// horizontal
				new AlignmentsList(M + N - 1),	// diagright
				new AlignmentsList(N),			// vertical
				new AlignmentsList(M + N - 1),	// diagleft
			};
		}

		//#region COPY

		/**
		 * Complexity: O(M + N + M+N-1 + M+N-1 + 4) = O(3(M+N))
		 * @param DB
		 */
		protected void copyAlignmentStructures(S DB) {
			alignments_by_dir		= new AlignmentsList[]{
				new AlignmentsList(DB.alignments_by_dir[0]),
				new AlignmentsList(DB.alignments_by_dir[1]),
				new AlignmentsList(DB.alignments_by_dir[2]),
				new AlignmentsList(DB.alignments_by_dir[3])
			};
		}

		//#endregion COPY
		
	//#endregion INIT


	//#region DEBUG

		@Override
		public void printAlignments() {
			System.out.println(printAlignmentsString(0));
		}
	
		@Override
		public String printAlignmentsString(int indentation) {

			String	indent = "",
					res = "";
			for(int i = 0; i < indentation; i++) indent += '\t';

			res += indent + "ALIGNMENTS:\n";
			res += indent + "by rows:\n";

			for(int d = 0; d < DIR_ABS_N; d++) {
				MovePair dir = DIRECTIONS[d % DIR_ABS_N];
				res += indent + "direction: " + dir + "\n";

				for(int player = 0; player < 2; player++) {
					res += indent + "player " + Player_byte[player] + ":\n\n";
					for(int i = 0; i < alignments_by_dir[d].size(); i++) {

						if(alignments_by_dir[d].get(i) != null) {
							res += indent + "index " + i + "\n\n";
							for(BiNode<ThreatPosition> p = alignments_by_dir[d].getFirst(Player_byte[player], i);
								p != null; p = p.next
							) {
								res += indent + p.item + "\n\n";
							}
							
						}
					}
					
				}
			}
			return res;
		}

		@Override
		public void printAlignmentsFile(FileWriter file, int indentation) {
			try {
				file.write(printAlignmentsString(indentation));
			} catch(IOException io) {}
		}
	
	//#endregion DEBUG

}