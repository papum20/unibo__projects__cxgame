package pndb;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

import pndb.Operators.AlignmentPattern;
import pndb.Operators.ThreatsByRank;
import pndb.constants.Auxiliary;
import pndb.constants.CellState;
import pndb.constants.GameState;
import pndb.constants.MovePair;
import pndb.constants.Constants.BoardsRelation;
import pndb.threats.ThreatApplied;
import pndb.threats.Threat;
import pndb.threats.Alignment;
import pndb.threats.Threat.USE;
import pndb.tt.TTElementBool;
import pndb.tt.TranspositionTable;
import pndb.structures.AlignmentsRows;
import pndb.structures.BiList_Alignments;
import pndb.structures.BiList.BiNode;



/**
 * <p>	1.	Complexities use N both for M and N, so imagine N is the max/avg.
 * <p>	2.	Most complexities are capped by something like 10N, because alignments_by_dir are created only when needed,
 * 			so, of course, they're actually only created once per board.
 */
public class BoardBitDb extends BoardBit {
	

	//#region CONSTANTS
		private static final MovePair DIRECTIONS[] = MovePair.DIRECTIONS;
		/* number of absolute directions */
		private static final int DIR_ABS_N = MovePair.DIR_ABS_N;
		/* indexes for alignments_by_dir */
		private static int	DIR_IDX_HORIZONTAL	= MovePair.DIR_IDX_HORIZONTAL,
							DIR_IDX_DIAGRIGHT 	= MovePair.DIR_IDX_DIAGRIGHT,
							DIR_IDX_VERTICAL	= MovePair.DIR_IDX_VERTICAL,
							DIR_IDX_DIAGLEFT	= MovePair.DIR_IDX_DIAGLEFT;		

		protected static final MovePair MIN = new MovePair(0, 0);
		protected final MovePair MAX;
		protected final int MAX_SIDE;
		/* alignments */
		protected static int MIN_MARKS;

		public static byte MY_PLAYER;

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
	protected AlignmentsRows[] alignments_by_dir;
	private short alignments_n;
	
	protected byte attacker;


	public static TranspositionTable<TTElementBool, TranspositionTable.Element.Key> TT;
	private static TranspositionTable.Element.Key key = new TranspositionTable.Element.Key();
	public long hash;
	
	/* implementation
	*/
	// for findAlignmentsInDirection()
	protected static MovePair	c1				= new MovePair(),
								c2				= new MovePair(),
								c3				= new MovePair(),
								end_c1			= new MovePair(),
								end_c2			= new MovePair(),
								c_it			= new MovePair(),
								threat_start	= new MovePair(),
								threat_end		= new MovePair();
	
	// Debug
	protected int count = 0;
	protected int found = 0;
	protected static boolean DEBUG_ON		= false;
	protected static boolean DEBUG_PRINT	= false;
	protected static FileWriter file;
  



	/**
	 *	<p>	Complexity: O(3N)
	 *	<p>	-	O(5N), if M > 64
	 * @param M
	 * @param N
	 * @param X
	 */
	protected BoardBitDb(int M, int N, int X) {
		super();
		alignments_n = 0;
		
		MAX = new MovePair(M, N);
		MAX_SIDE = Math.max(M, N);
		MIN_MARKS = X - Operators.MARK_DIFF_MIN;
		attacker = 0;
		hash = 0;
		
		alignments_by_dir = new AlignmentsRows[DIR_ABS_N];
		markedThreats = new LinkedList<ThreatApplied>();
	}

	/**
	 *	<p>	Copy all except threats, and set player.
	 *	<p>	Complexity: O(6N)
	 *	<p>	-	O(10N), if M > 64
	 * @param B
	 */
	protected BoardBitDb(BoardBit B, byte player) {
		super(B);
		alignments_n = 0;
		
		MAX = new MovePair(M, N);
		MAX_SIDE = Math.max(M, N);
		MIN_MARKS = X - Operators.MARK_DIFF_MIN;
		attacker = player;
		hash = 0;
		
		alignments_by_dir = new AlignmentsRows[DIR_ABS_N];
		markedThreats = new LinkedList<ThreatApplied>();
	}
	
	/**
	 * <p>	Copy all and optionally copy threats.
	 * <p>	Complexity: O(6N)
	 * <p>	-	O(10N), if M > 64
	 * @param B
	 * @param copy_threats
	 */
	protected BoardBitDb(BoardBitDb B, boolean copy_threats) {
		super();
		alignments_n = B.alignments_n;
		
		MAX = new MovePair(M, N);
		MAX_SIDE = Math.max(M, N);
		MIN_MARKS = X - Operators.MARK_DIFF_MIN;
		attacker = B.attacker;
		hash = B.hash;
		hash = 0;
		
		if(copy_threats)
			alignments_by_dir = new AlignmentsRows[]{
				(B.alignments_by_dir[0] != null) ? new AlignmentsRows(B.alignments_by_dir[0]) : null,
				(B.alignments_by_dir[1] != null) ? new AlignmentsRows(B.alignments_by_dir[1]) : null,
				(B.alignments_by_dir[2] != null) ? new AlignmentsRows(B.alignments_by_dir[2]) : null,
				(B.alignments_by_dir[3] != null) ? new AlignmentsRows(B.alignments_by_dir[3]) : null
			};
		else alignments_by_dir = new AlignmentsRows[DIR_ABS_N];
		markedThreats = new LinkedList<ThreatApplied>(B.markedThreats);	//copy marked threats
		
		copy(B);
	}
	
	/**
	 * <p>	Complexity: O(6N)
	 * <p>	-	O(10N), if M > 64
	 */
	public BoardBitDb getCopy(boolean copy_threats) {
		return new BoardBitDb(this, copy_threats);
	}
	
	/**
	 *	<p>	Copy all.
	 *	<p>	Complexity: O(3N)
	 *	<p>	-	O(5N), if M > 64
	 * @param B
	 */
	public void copy(BoardBitDb B) {

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
		 * <p>	Mark an arbitrary cell. Use with caution.
		 * <p>	Increases free[] anyway.
		 * <p>	Complexity:  O(1)
		 * <p>	-	Complexity: O(1 + removeAlignments)
		 * @param col
		 * @param player
		 * @return GameState
		 */
		private void markAny(int i, int j, byte player) {
			hash = TranspositionTable.getHash(hash, i, j, Auxiliary.getPlayerBit(player));

			board[j][i / BITSTRING_LEN]			|= (long)(player & 1) << (i % BITSTRING_LEN);	// =1 for CellState.ME
			board_mask[j][i / BITSTRING_LEN]	|= (long)1 << (i % BITSTRING_LEN);
			free[j]++;
			free_n--;

			removeAlignments(new MovePair(i, j), Auxiliary.opponent(player));
		}

		/**
		 * <p>	Complexity: O(1)
		 */
		@Override
		public void mark(int col, byte player) {
			hash = TranspositionTable.getHash(hash, free[col], col, Auxiliary.getPlayerBit(player));
			super.mark(col, player);
		}

		/**
		 * <p>	Mark cell; also remove opponent's alignments, but doesn't find new ones.
		 * <p>	Complexity (worst):	O(4X)
		 * <p>	Complexity (best):	O(1)
		 * <p>	-	Complexity (worst):	O(4X +	removeAlignments)
		 * <p>	-	Complexity (best):	O(1	+	removeAlignments)
		 * @param i
		 * @param j
		 * @param player
		 */
		protected void mark(int i, int j, byte player) {
			markCheck(j, player);
			removeAlignments(new MovePair(i, j), Auxiliary.opponent(player));
		}
		
		/**
		 * <p>	Complexity (worst):	O(4X)
		 * <p>	Complexity (best):	O(1)
		 * <p>	-	Complexity (worst):	O(4X +	removeAlignments)
		 * <p>	-	Complexity (best):	O(1	+	removeAlignments)
		 */
		public void mark(MovePair cell, byte player) {mark(cell.i, cell.j, player);}

		/**
		 * <p>	Complexity (worst):	O(cells.len * 4X)
		 * <p>	Complexity (best):	O(cells.len * 1)
		 * <p>	*	cells are usually only a few threat cells
		 * <p>	*	having common als, remove works less
		 */
		public void markMore(MovePair[] cells, byte player) {
			for(MovePair c : cells) mark(c.i, c.j, player);
		}

		/**
		 * <p>	Complexity (worst): O(16X)
		 * <p>	Complexity (worst):	O(related.len * 4X)
		 * <p>	Complexity (best):	O(related.len * 1)
		 * <p>	*	related are usually only a few
		 * <p>	*	having common als, remove works less
		 */
		public void markThreat(MovePair[] related, int atk_index) {

			for(int i = 0; i < related.length; i++) {
				if(i == atk_index) {
					markAny(related[i].i, related[i].j, attacker);	// markAny, for vertical
					check(related[i].i, related[i].j, attacker);		// only check for attacker
				} else {
					markAny(related[i].i, related[i].j, Auxiliary.opponent(attacker));	// markAny, for vertical
					check(related[i].i, related[i].j, Auxiliary.opponent(attacker));	// only check for defender
				}
			}
		}

		/**
		 * Complexity: O(1)
		 */
		@Override
		public void unmark(int col) {
			hash = TranspositionTable.getHash(hash, free[col] - 1, col, _cellState(free[col] - 1, col));
			super.unmark(col);
		}

		/**
		 * <p>	Complexity (best):		O(4X),	if isWinningMove
		 * <p>	Complexity (best):		O(12X),	if findAlignments, and dir_excluded
		 * <p>	Complexity (best):		O(40X),	if findAlignments
		 * <p>	Complexity (worst/avg):	O(10N),	if findAlignments, and has to create all AlignmentsRows now
		 * @param cell
		 * @param max_tier
		 * @param dir_excluded
		 * @param caller
		 */
		protected void checkAlignments(MovePair cell, int max_tier, int dir_excluded) {

			if(isWinningMove(cell.i, cell.j))
				game_state = cell2GameState(cell.i, cell.j);
			else {
				findAlignments(cell, cellState(cell), max_tier, true, null, 0, dir_excluded);
				if(free_n == 0 && game_state == GameState.OPEN) game_state = GameState.DRAW;
			}
		}
		/**
		 * <p>	Complexity (best):	O(4X),	if isWinningMove
		 * <p>	Complexity (best):	O(16X),	if 1 cell
		 * <p>	Complexity (best):	O(4X * cells.len), if more cells
		 * <p>	-	= O(4*16X)
		 * <p>	Complexity (worst):	O(10N),	if has to create all AlignmentsRows now
		 */
		public void checkAlignments(MovePair[] cells, int max_tier) {
			if(cells.length == 1) {
				checkAlignments(cells[0], max_tier, -1);
			} else {
				// only check the common direction once
				MovePair dir = cells[0].getDirection(cells[1]);
				int dir_index = dirIdx_fromDir(dir);

				for(int i = 0; i < cells.length && game_state == GameState.OPEN; i++)
					checkAlignments(cells[i], max_tier, dir_index);

				if(game_state == GameState.OPEN) {
					// loop useless, to remove, but should understand which 2 cells to use as direction, i.e. the borders
					for(int i = 1; i < cells.length; i++)
						findAlignmentsInDirection(cells[i], cells[i-1], cellState(cells[i]), dir_index, max_tier, true, null, 0);
				}
				//update gameState
				if(free_n == 0 && game_state == GameState.OPEN) game_state = GameState.DRAW;
			}
		}

	//#endregion BOARD


	//#region DB_SEARCH

		/**
		 * <p>	Complexity, def: O(6N),		capped by getCopy
		 * <p>	Complexity, bth: O(6N),		if not check_threats,	capped by getCopy
		 * <p>	Complexity, bth: O(16N),	if check_threats and has to create AlignmentsRows now
		 * <p>	*	capped by getCopy+checkAlignments
		 * <p>
		 * @param threat as defined in Operators
		 * @param atk attacker's move index in threat
		 * @param use whose threat cells to add
		 * @param threats wether to update alignments and threats for this board
		 * @return :	a new board resulting after developing this with such threat (dependency stage);
		 * 				the new board only has alignment involving the newly marked cells
		 */
		public BoardBitDb getDependant(Threat threat, int atk, USE use, int max_tier, boolean check_threats) {
			
			BoardBitDb res = null;
			
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
						res.markMore(threat.getDefensive(atk), attacker);
					// add this threat and check_alignments are done in getDefensiveRoot
					break;
					//used for dependency stage
				case BTH:
					res = getCopy(false);
					res.markThreat(threat.related, atk);
					res.addThreat(threat, atk, attacker);
					if(check_threats) res.checkAlignments(threat.related[atk], max_tier, -1);
			}
			return res;
		}
		
		/**
		 * <p>	Only checks for alignments not included in the union of A's and B's alignments, i.e. those which involve at  least one cell only present in A and one only in B.
		 * <p>	Complexity (best):	O( 6N ), 	if nothing added
		 * <p>	Complexity:			O( 6N + added_threats_n * 3 * 40X )
		 * <p>	*	getCopy + findAlignment for each added threat, assuming its len 3 (avg)
		 * <p>	Complexity (worst): O(16N),		if has to create all AlignmentsRows now
		 */
		public BoardBitDb getCombined(BoardBitDb B, int max_tier) {

			BoardBitDb res = getCopy(true);
			LinkedList<MovePair> added_threat_attacks = new LinkedList<MovePair>();

			for(ThreatApplied athreat : B.markedThreats) {
				if(isUsefulThreat(athreat)) {
					//mark other board's threat on res
					for(int i = 0; i < athreat.threat.related.length; i++) {
						MovePair c = athreat.threat.related[i];

						if(cellFree(c.i, c.j)) {
							if(i == athreat.related_index) {
								res.mark(c, athreat.attacker);
								added_threat_attacks.add(c);
							}
							else res.mark(c, Auxiliary.opponent(athreat.attacker));
						}
					}
					//add threats
					res.addThreat(athreat);
				}
			}

			// calculate alignments
			for(MovePair m = added_threat_attacks.pop(); !added_threat_attacks.isEmpty(); m = added_threat_attacks.pop())
				findAlignments(m, attacker,  max_tier, true, null, 0, -1);

			return res;
		}

		/**
		 * <p>	Check if a combination with node is valid, i.e. if they're not in conflict and both have a marked cell the other doesn't.
		 * <p>	Assumes both boards have the same `MY_PLAYER` (i.e. the same bit-byte association for players).
		 * <p>
		 * <p>	Complexity: O(N)
		 * <p>	-	O(2N), if M > 64
		 * @param B
		 * @param attacker
		 * @return
		 */
		public BoardsRelation validCombinationWith(BoardBitDb B) {

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


	//#region AUXILIARY
	
		/**
		 * Complexity: O(1)
		 */
		public void addThreat(Threat threat, int atk, byte attacker) {
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
			return -1;
		}

		//#region ALIGNMENTS

			/**
			 * <p>	Complexity: O(1)
			 * <p>	-	O(4 + als_involving_center), usually not many
			 * @param center
			 * @param player
			 */
			protected void removeAlignments(final MovePair center, byte player) {

				if(alignments_n == 0)
					return;
			
				for(int d = 0; d < DIR_ABS_N; d++) {
					if(alignments_by_dir[d] == null)
						continue;

					BiList_Alignments als_in_row = alignments_by_dir[d].get(getIndex_for_alignmentsByDir(DIRECTIONS[d], center));

					if(als_in_row != null) {
						BiNode<Alignment>	al = als_in_row.getFirst(player),
												al_next;
						
						while(al != null) {
							al_next = al.next;

							if(center.inBetween_included(al.item.start, al.item.end))
								removeAlignmentNode(als_in_row, al, player);

							al = al_next;
						}
					}
				}

			}


			/**
			 * <p>	Return the first/last (depending on parameter `find_first`) cell containing `target`, moving from `start`, with increment `incr`,
			 * 		for a max distance of `max_distance` (excluded).
			 * <p>	Stop if finds a cell containing `stop_value` (excluded), or if reaches `stop_cell` (excluded), or if `only_target` is true and finds
			 * 		something different from target (in case of `only_target`, `stop_value` is not considered).
			 * <p>
			 * <p>	note: this function could be simplified, as is used only once.
			 * <p>
			 * <p>	Complexity: O(3)
			 * <p>	*	considering the only way it's used
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
			protected int _findOccurrenceUntil(MovePair res, final MovePair start, MovePair incr,
				int max_distance, byte target, byte stop_value, boolean only_target, boolean find_first, boolean only_valid, int dir_index
			) {
				
				int distance;

				for( c_it.reset(start), distance = 0
					; distance < max_distance && c_it.inBounds(MIN, MAX)
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
			 * <p>	note: no max_tier, must check before calling.
			 * <p>	Complexity (best): O(6),	checks all als of 1 type
			 * <p>	Complexity (worst): O(2N),	if has to create an AlignmentsRows
			 * @param dir_index
			 * @param player
			 * @param lined
			 * @param marks
			 * @param before
			 * @param after
			 * @param stacked see `findAlignmentsInDirection`
			*/
			private void _addValidAlignments(int dir_index, byte player,
				int lined, int marks, int before, int after, final MovePair last_stacked, int stacked
			) {

				int tier = Operators.tier_from_alignment(X, marks);

				// check alignments, foreach alignment of mark marks
				for(byte threat_code : Operators.alignmentCodes(tier))
				{
					AlignmentPattern alignment = Operators.alignmentPatterns(tier).get((int)threat_code);

					//if (inner alignment conditions)
					if(alignment.isApplicableExactly(X, lined, marks, before, after)) {

						if(tier == 0) {
							game_state = Auxiliary.cellState2winState(player);
							return;
						}

						//add to structures
						threat_start.resetToVector(c1, DIRECTIONS[dir_index], -before);
						threat_end.resetToVector(c2, DIRECTIONS[dir_index], alignment.out - before);
						Alignment threat_pos = (last_stacked == null) ? new Alignment(threat_start, threat_end, threat_code)
							: new Alignment(threat_start, threat_end, threat_code, last_stacked, (byte)stacked);
						addAlignment(threat_pos, dir_index, player);
					}
				}
			}

			/**
			 * <p>	Check for alignments for all values of before (decreasing it up to 0);
			 * <p>	also check for tier <= max_tier.
			 * <p>	Complexity (best): O(18),	if has to check all values for before, for all al types of 1 tier
			 * <p>	Complexity (worst): O(2N),	if has to create an AlignmentsRows
			 * <p>	Complexity (best):	O(1),	if wrong tier
			 * @param dir_index
			 * @param player
			 * @param lined
			 * @param marks
			 * @param before
			 * @param after
			 * @param last_stacked
			 * @param stacked
			 */
			private void _addAllValidAlignments(int dir_index, byte player,
				int lined, int marks, int before, int after,
				int max_tier, final MovePair last_stacked, int stacked
			) {
				// test alignments and extend the line
				if(Operators.tier_from_alignment(X, marks) <= max_tier) {
					while(before >= 0) {
						_addValidAlignments(dir_index, player, lined, marks, before, after, last_stacked, stacked);
						before--;
					}
				}
			}
			
			/**
			 * <p>	invariants:
			 * <p>	-	c1 and c2 indicate the first and last mark of a possible alignment (otehrwise, the priority is to bring both on a player's cell);
			 * <p>	-	lined = c2-c1 : length of the alignment (excluding outer free cells);
			 * <p>	-	marks : number of marks in range c1-c2;
			 * <p>	-	in = lined - marks : number of free cells in range c1-c2;
			 * <p>	-	before, after: trailing and lookahead number of free cells, constantly kept updated not to go beyond max value
			 * 			(before is initialized correctly as a parameter, while after is checked in a recursion condition);
			 * <p>	-	c3 = c2 + after : lookahead;
			 * <p>
			 * <p>	note: the second extreme is always excluded from counting the alignment (c1, c2, after..).
			 * <p>	efficiency: c2 and c3 neveg go back
			 * <p>
			 * <p>	Complexity (best):	O(2 * (second - first + 2X)),		nothing found/either c1 or c2 advance
			 * <p>	Complexity (worst):	O( 2N + 18 * possible_als_found),	if found als, and has to create AlignmentsRows
			 * <p>	*	note that only 1 AlignmentsRows can be added, in 1 same direction
			 * <p>	*	with possible_als_found=number of calls for addAllValidAlignments
			 * @param second limit (included)
			 * @param dir
			 * @param player
			 * @param lined
			 * @param marks
			 * @param in
			 * @param before
			 * @param after
			 * @param only_valid
			 * @param max_tier
			 * @param stacked see `findAlignmentsInDirection`
			 */
			private void _findAlignmentsInDirection(final MovePair second, MovePair dir, int dir_index, byte player,
				int lined, int marks, int before, int after,
				boolean only_valid, int max_tier, final MovePair last_stacked, int stacked
			) {

				// end recursion if not enough space to create any alignment
				if(c1.getDistanceInDir(second, dir) < Operators.MIN_LINED_LEN(X) || !c1.inBounds(MIN, MAX)) {
					return;
				}
				/* cases to "soft" increase c1 (don't reset all params): 
				 * check the free cell is the first free in the column (i.e. can put a stone directly in it),
				 * otherwise pass to next branches.
				 */
				else if( cellFree(c1.i, c1.j) && (
					!only_valid							// can skip
					|| free[c1.j] == c1.i				// is first free
					|| dir_index == DIR_IDX_VERTICAL)	// see later
				) {
					if(dir_index == DIR_IDX_VERTICAL) {
						// there aren't holes stacked in vertical
						return;
					}
					c1.sum(dir);
					if(lined > 0)
						// it means c2 is on a player's cell, and c1 was already advancing, and now is passing on an 'in' free cell
						_findAlignmentsInDirection(second, dir, dir_index, player,
							lined - 1, marks, Math.min(before + 1, Operators.MAX_OUT_ONE_SIDE), after,
							only_valid, max_tier, last_stacked, stacked);
					else
						// still searching for the start of any alignment
						_findAlignmentsInDirection(second, dir, dir_index, player,
							0, 0, Math.min(before + 1, Operators.MAX_OUT_ONE_SIDE), 0,
							only_valid, max_tier, last_stacked, stacked);
				}
				/* cases to "hard" increase c1 (reset params):
				 * other cases except c1 = player; specifically:
				 * 1. other cases c1 free (only_valid && first free cell is lower && dir != vertical),
				 * or 2. c1 occupied by opponent
				 */
				else if(cellState(c1) != player) {
					// can only happen whlie was searching for the start of any alignment
					c1.sum(dir);
					_findAlignmentsInDirection(second, dir, dir_index, player,
						0, 0, 0, 0,
						only_valid, max_tier, last_stacked, stacked);
				}
				/* case c1 = player:
				 * note: only in this branch alignment grows (i.e. c2/c3 increase)
				 */
				else {
					if(lined + after == 0)
						// to be sure (c3 could be before c1)
						c3.reset(c1);

					// test alignments
					_addAllValidAlignments(dir_index, player,
						lined, marks, before, after,
						max_tier, last_stacked, stacked);
						
					/* c1 is on a player's cell, now let's analyze .
					 * not to add useless threats (i.e. smaller ones when there are bigger ones available), we extend c2 as much as we can,
					 * and then we add them just before reducing 'lined' - and we (try to) do that each very time we need to do that (all cases).
					 */
					/* note that the following condition reflect previous ones, but opereate on c3 instead of c1.
					 */

					// too much free space while looking for the next c2, need to increase c1.
					if(after + lined > Operators.MAX_LINED_LEN(X) || !c3.inBounds(MIN, MAX)) {
						// reduce the line
						c1.sum(dir);
						_findAlignmentsInDirection(second, dir, dir_index, player,
							lined - 1, marks - 1, 0, after,
							only_valid, max_tier, last_stacked, stacked);
					}
					/* case c3 empty:
					 * (same as for c1)
					 * increase after (after checks)
					 */
					else if( cellFree(c3.i, c3.j) && (
						!only_valid							// can skip
						|| free[c3.j] == c3.i				// is first free
						|| dir_index == DIR_IDX_VERTICAL)	// see later
					) {
						if(dir_index == DIR_IDX_VERTICAL) {
							// vertical: last check and break (note: before is 0)
							_addAllValidAlignments(dir_index, player,
								lined, marks, before, after + M - c3.i,
								max_tier, last_stacked, stacked);
							return;
						}
						c3.sum(dir);
						_findAlignmentsInDirection(second, dir, dir_index, player,
							lined, marks, before, after + 1,
							only_valid, max_tier, last_stacked, stacked);
					}
					/* cases "hard":
					 * reset alignment (make c1 jump ahead)
					 */
					else if(cellState(c3) != player) {
						c1.reset(c3.sum(dir));
						// reduce the line
						_findAlignmentsInDirection(second, dir, dir_index, player,
							0, 0, 0, 0,
							only_valid, max_tier, last_stacked, stacked);
					}
					/* case c3 = player.
					 * increase c3
					 */
					else {
						c2.reset(c3);
						c3.sum(dir);
						_findAlignmentsInDirection(second, dir, dir_index, player,
							lined + after + 1, marks + 1, before, 0,
							only_valid, max_tier, last_stacked, stacked);
					}
				}

			}

			/**
			 * <p>	Find all alignments involving at least one cell between the aligned first, second.
			 * <p>	Stack operator is only used for vertical.
			 * <p>	
			 * <p>	1 -	the cells whose alignments will change are those at max distance K-1 from this;
			 * <p>	2 -	considering the algorithm is correct, we assume they already have associated, if already
			 *			existed, alignments from K-MIN_SYM_LINE to K-1 symbols (if existed of K, the game would be ended);
			 * <p>	3 -	that said, we will only need to increase existing alignments by 1 symbol, and to add
			 * 			new alignments of K-MIN_SYM_LINE.
			 * <p>	HOWEVER, this will be a future enhancement: for now, the function simply deletes and recreates all.  
			 * <p>
			 * <p>	(not considering stacked)
			 * <p>	Complexity (best):	O( 2 * (second - first + 2X) ),		nothing found/either c1 or c2 advance
			 * <p>	-	O(2N),	if whole row
			 * <p>	-	O(4X),	if single cell
			 * <p>	Complexity (worst):	O( 6N + 18 * possible_als_found ),	if found als, and has to create AlignmentsRows
			 * <p>	*	note: as 3 * _findAlignmentsInDirection
			 * <p>	*	note that only 1 AlignmentsRows can be added, in 1 same direction
			 * <p>	*	with possible_als_found=number of calls for addAllValidAlignments
			 * <p>
			 * @param first
			 * @param second
			 * @param player
			 * @param dir_index
			 * @param max_tier
			 * @param only_valid if true, only search for immediately applicable threats, i.e. free[j] = i, for each (i,j) in threat
			 * @param stacked (implementative) number of pieces stacked to check for other alignments (to init to 0)
			 * @param caler
			 */
			protected void findAlignmentsInDirection(MovePair first, MovePair second, byte player,
				int dir_index, int max_tier, boolean only_valid, MovePair last_stacked, int stacked
			) {

				// swap first, second if not first->second in same direction as dir
				if(!DIRECTIONS[dir_index].equals( first.getDirection(second)) ) {
					MovePair tmp = first;
					first	= second;
					second	= tmp;
				}

				// find furthest c1 back, from center
				_findOccurrenceUntil(c1, c1.reset(first), DIRECTIONS[dir_index + DIR_ABS_N],
					X + Operators.MAX_OUT_ONE_SIDE - 1, player, Auxiliary.opponent(player), false, false, only_valid, dir_index);
				c2.reset(c1);
				c3.reset(c1);

				_findAlignmentsInDirection(second,				// flip if direction inverted
					DIRECTIONS[dir_index], dir_index, player,
					0, 0, 0, 0,
					only_valid, max_tier, last_stacked, stacked);

				// additional threat, for vertical dir
				if(dir_index == DIR_IDX_VERTICAL && second.i >= free[second.j] && free[second.j] < M && game_state == GameState.OPEN && max_tier >= 2) {
					MovePair new_stacked = new MovePair(free[second.j], second.j);
					markCheck(new_stacked.j, player);
					// exclude vertical, or would add non-existing alignments
					findAlignments(new_stacked.getSum(DIRECTIONS[DIR_IDX_VERTICAL]), player, max_tier - 1, only_valid, new_stacked, stacked + 1, DIR_IDX_VERTICAL);
					unmark(new_stacked.j);
				}

			}

			/**
			 * <p>	Find alignments for a cell in all directions.
			 * <p>	Complexity (approx):	O(10N),	if adds all AlignmentsRows now
			 * <p>	Complexity (best):		O(40X)
			 * <p>	Complexity (worst):		O( 10N + 18 * possible_als_found ),	if found als, and has to create AlignmentsRows
			 * <p>	*	4 * findAlignmentsInDirection for single cell, + stacked
			 * <p>	*	with possible_als_found=number of calls for addAllValidAlignments
			 * <p>	*	10N results from 3 directions * 3 sotacked, +1 for vertical direction
			 * @param stacked see `findAlignmentsInDireciton` (to init to 0)
			 */
			protected void findAlignments(final MovePair cell, final byte player,
				int max_tier, boolean only_valid, final MovePair last_stacked, int stacked, int dir_excluded
			) {
				for(int d = 0; d < DIR_ABS_N; d++) {
					if(d != dir_excluded)
						findAlignmentsInDirection(cell, cell, player, d, max_tier, only_valid, last_stacked, stacked);
				}
			}
			
			/**
			 * <p>	Find all alignments for a player.
			 * <p>	Complexity (all):	O( 12N**2 )
			 * <p>	*	6N * findAlignmentsInDirection
			 * <p>	*	number of rows scanned covers other costs
			 * @param attacker
			 * @param max_tier
			 */
			public void findAllAlignments(int max_tier, boolean only_valid) {

				MovePair start, end;
				for(int d = 0; d < DIR_ABS_N; d++)
				{
					for(start = nextStartOfRow_inDir(null, d), end = new MovePair();
						start.inBounds(MIN, MAX);
						start = nextStartOfRow_inDir(start, d)
					) {
						end.reset(start).clamp_diag(MIN, MAX, DIRECTIONS[d], Math.max(M, N));
						findAlignmentsInDirection(start, end,  attacker, d, max_tier, only_valid, null, 0);
					}
				}
			}

			/**
			 * Complexity: O(1)
			 * @param idx
			 * @return
			 */
			private int alignmentsByDirSize(int idx) {
				if(idx == 0) return M;
				else if(idx == 1 || idx == 3) return M + N - 1;
				else return N;
			}
			/**
			 * <p>	Index for alignments_by_direction.
			 * <p>	Complexity: O(1)
			 * @return the index where, in alignments_by_direction, is contained position in direction dir
			 */
			protected int getIndex_for_alignmentsByDir(MovePair dir, MovePair position) {
				if(dir.i == 0)				return position.i;				//horizontal
				else if(dir.j == 0)			return position.j;				//vertical
				else if(dir.i == dir.j)		return dir.j - dir.i + M - 1;	//dright
				else return dir.i + dir.j;									//dleft
			}

			/**
			 * <p>	Complexity: 
			 * <p>	-	one call: O(1)
			 * <p>	-	single iteration:	M, N, M+N-1, M+N-1 for each direction (col, row, diag, anti-diag)
			 * <p>	-	all directions:		O(6N)
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
			 * Complexity: O(1)
			 * @param player
			 * @return true if there are valid alignments (calculated before, with proper max_tier)
			 */
			public boolean hasAlignments() {
				return alignments_n > 0;
			}


			/**
			 * <p>	Complexity: O(1)
			 * <p>	-	Complexity (worst): O(4), threat.related are only a few
			 * @param athreat
			 * @param attacker
			 * @return true if the AppliedThreat is compatible with this board, and useful, i.e. adds at least one attacker's threat.
			 */
			protected boolean isUsefulThreat(ThreatApplied athreat) {

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

			/**
			 * <p>	Complexity (best):	O(1),			if no als
			 * <p>	Complexity (worst):	O( 6N + als_n * X )
			 * <p>	*	N for structure size, als_n for alignments n, X as worst applied()
			 */
			public ThreatsByRank getApplicableOperators(int max_tier) {

				byte defender		= Auxiliary.opponent(attacker);
				ThreatsByRank res	= new ThreatsByRank();

				if(alignments_n == 0)
					return res;
			
				for(AlignmentsRows alignments_by_row : alignments_by_dir) {
					if(alignments_by_row == null)
						continue;

					for(BiList_Alignments alignments_in_line : alignments_by_row) {
						if(alignments_in_line == null)
							continue;
							
						BiNode<Alignment> alignment = alignments_in_line.getFirst(attacker);
						if(alignment != null && Operators.tier_from_code(alignment.item.type) <= max_tier) {
							do {
								Threat cell_threat_operator = Operators.applied(this, alignment.item, attacker, defender);
								
								if(cell_threat_operator != null) res.add(cell_threat_operator);
								alignment = alignment.next;

							} while(alignment != null);
						}
					}
				}
				return res;
			}

			/**
			 * <p>	Complexity: O( 12N**2 )
			 * <p>	*	capped by findAllAlignments
			 */
			public int[] getThreatCounts(byte player) {

				setAttacker(player);
				findAllAlignments(Operators.MAX_TIER, false);
				
				int[] threats_by_col = new int[N];

				if(alignments_n == 0)
					return threats_by_col;
					
				for(int d = 0; d < DIR_ABS_N; d++) {
					if(alignments_by_dir[d] == null)
						continue;

					for(BiList_Alignments alignments_in_line : alignments_by_dir[d]) {
						if(alignments_in_line == null)
							continue;
						
						BiNode<Alignment> p = alignments_in_line.getFirst(player);
						while(p != null) {
							// if in same col
							if(p.item.start.j == p.item.end.j)
								threats_by_col[p.item.start.j] += (p.item.start.getDistanceAbs(p.item.end) + 1) * Operators.indexInTier(p.item.type);
							
							else {
								for(int j = p.item.start.j; j <= p.item.end.j; j++)
									threats_by_col[j] += Operators.indexInTier(p.item.type);
							}
	
							p = p.next;
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
		public void setAttacker(byte player) {
			this.attacker = player;
		}
		
		/**
		 * Complexity: O(N)
		 */
		public int getMC_n() {
			int MC_n = 0;
			for(int j = 0; j < N; j++) MC_n += free[j];
			return MC_n;
		}

		/**
		 * Complexity: O(1)
		 */
		public LinkedList<ThreatApplied> getMarkedThreats() {return markedThreats;}
	
	//#endregion GET
	
	//#region SET

		/**
		 * <p>	Helper, for adding an alignment to the structures.
		 * <p>	Complexity (best):	O(1)
		 * <p>	Complexity (worst):	O(2N), if has to create an AlignmentsRows
		 */
		protected void addAlignment(Alignment alignment, int dir_index, byte player) {
			if(alignments_by_dir[dir_index] == null)
				alignments_by_dir[dir_index] = new AlignmentsRows(alignmentsByDirSize(dir_index));
			alignments_by_dir[dir_index].add(player, getIndex_for_alignmentsByDir(DIRECTIONS[dir_index], threat_start), alignment);
			alignments_n++;
		}

		/**
		 * <p>	Helper, for adding an alignment to the structures.
		 * <p>	Complexity: O(1)
		 */
		protected void removeAlignmentNode(BiList_Alignments alignments_in_line, BiNode<Alignment> node, byte player) {
			alignments_in_line.remove(player, node);
			alignments_n--;
		}
	
	//#endregion SET

	//#region TT

		/**
		 * <p>	Get entry from TTdag.
		 * <p>	Complexity: O(1 + alpha)
		 * @return entry
		 */
		public TTElementBool getEntry() {
			return TT.get(TTElementBool.setKey(key, hash));
		}

		/**
		 * <p>	Add entry to TTdag.
		 * <p>	Complexity: O(1)
		 * @param node
		 */
		public void addEntry(TTElementBool node) {
			TT.insert(hash, node);
			//TT.count++;
		}

		/**
		 * <p>	Remove entry from TTdag.
		 * <p>	Complexity: O(1 + alpha)
		 */
		public void removeEntry() {
			TT.remove(TTElementBool.setKey(key, hash));
			//TT.count--;
		}
	
	//#endregion TT

	//#region DEBUG

		public void printAlignments() {
			System.out.println(printAlignmentsString(0));
		}
	
		public String printAlignmentsString(int indentation) {

			String	indent = "",
					res = "";
			for(int i = 0; i < indentation; i++) indent += '\t';

			res += indent + "ALIGNMENTS:\n";
			res += indent + "by rows:\n";

			for(int d = 0; d < DIR_ABS_N; d++) {
				MovePair dir = DIRECTIONS[d % DIR_ABS_N];
				res += indent + "direction: " + dir + "\n";

				for(byte player = 0; player < 2; player++) {
					res += indent + "player " + player + ":\n\n";
					if(alignments_by_dir[d] == null)
						continue;
						
					for(int i = 0; i < alignments_by_dir[d].size(); i++) {

						if(alignments_by_dir[d].get(i) != null) {
							res += indent + "index " + i + "\n\n";
							for(BiNode<Alignment> p = alignments_by_dir[d].getFirst(player, i);
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

		public void printAlignmentsFile(FileWriter file, int indentation) {
			try {
				file.write(printAlignmentsString(indentation));
			} catch(IOException io) {}
		}
	
	//#endregion DEBUG

}