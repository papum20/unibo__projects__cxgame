package pndb.alpha;

import java.io.FileWriter;
import java.util.LinkedList;

import connectx.CXCell;
import pndb.alpha.Operators.ThreatsByRank;
import pndb.alpha.threats.ThreatApplied;
import pndb.alpha.threats.ThreatCells;
import pndb.alpha.threats.ThreatCells.USE;
import pndb.constants.MovePair;
import pndb.constants.Constants.BoardsRelation;



public interface IBoardBitDb<S extends IBoardBitDb<S, BB>, BB extends IBoardBit> extends IBoardBit {
	


	/**
	 * Complexity:
	 * 		with mc: O(3M + 10N + B.marked_threats.length + MN) = O(B.marked_threats.length + N**2 + 13N)
	 * 		no mc: O(3M + 10N + B.marked_threats.length) = O(B.marked_threats.length + 13N)
	 * @param copy_threats
	 * @return
	 */
	public S getCopy(boolean copy_threats);

	public void copy(BB B);
	

	//#region BOARD

		public void mark(int j, byte player);

		public void mark(MovePair cell, byte player);
		public void markMore(MovePair[] cells, byte player);
		public void markThreat(MovePair[] related, int atk_index);

		public void checkAlignments(MovePair[] cells, int max_tier, String caller);

	//#endregion BOARD


	//#region DB_SEARCH

		/**
		 * 
		 * @param threat as defined in Operators
		 * @param atk attacker's move index in threat
		 * @param use as def in Operators
		 * @param threats wether to update alignments and threats for this board
		 * @return :	a new board resulting after developing this with such threat (dependency stage);
		 * 				the new board only has alignment involving the newly marked cells
		 */
		public abstract S getDependant(ThreatCells threat, int atk, USE use, int max_tier, boolean check_threats);
		
		/**
		 * only checks for alignments not included in the union of A's and B's alignments, i.e. those which involve at  least one cell only present in A and one only in B
		 * 		= O(marked_threats.length + N**2) + O(B.marked_threats.length * (16X * avg_threats_per_dir_per_line) )
		 */
		public abstract S getCombined(S B, byte attacker, int max_tier);

		public abstract BoardsRelation validCombinationWith(S B, byte attacker);

		
	//#endregion DB_SEARCH


	//#region ALIGNMENTS
		
		public ThreatsByRank getApplicableOperators(byte attacker, int max_tier);
		public int[] getThreatCounts(byte player);
		
	//#endregion ALIGNMENTS


	//#region AUXILIARY

		public void addThreat(ThreatCells threat, int atk, byte attacker);

		//#region ALIGNMENTS

			/**
			 * Find all alignments for a player.
			 * @param player
			 * @param max_tier
			 */
			public abstract void findAllAlignments(byte player, int max_tier, boolean only_valid, String caller);

			/**
			 * Complexity:
			 * 		worst (return false): O(3(M+N))
			 * @param player
			 * @return true if there are valid alignments (calculated before, with proper max_tier)
			 */
			public boolean hasAlignments(byte player);

		//#endregion ALIGNMENTS
		
	//#endregion AUXILIARY


	//#region GET
	
		public abstract int getCurrentPlayer();
	
		public abstract void setPlayer(byte player);

		public abstract int getMC_n();
		public abstract CXCell getMarkedCell(int i);
		public abstract LinkedList<ThreatApplied> getMarkedThreats();

		public abstract long getHash();
		
	//#endregion GET


	//#region DEBUG

			public abstract void printAlignments();
			public abstract String printAlignmentsString(int indentation);
			public abstract void printAlignmentsFile(FileWriter file, int indentation);
	
	//#endregion DEBUG

}