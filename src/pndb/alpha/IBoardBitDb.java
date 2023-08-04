package pndb.alpha;

import java.io.FileWriter;
import java.util.LinkedList;

import connectx.CXCell;
import pndb.alpha.threats.ThreatApplied;
import pndb.alpha.threats.ThreatCells;
import pndb.alpha.threats.ThreatCells.USE;
import pndb.constants.MovePair;



public interface IBoardBitDb<S extends IBoardBitDb<S>> extends IBoardBit<S> {
	


	public S getCopy(boolean copy_threats);

	public void copy(S B);
	

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
		
		//only checks for alignments not included in the union of A's and B's alignments, i.e. those which involve at  least one cell only present in A and one only in B
		public abstract S getCombined(S B, byte attacker, int max_tier);
		
	//#endregion DB_SEARCH


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
			 * 
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
		
	//#endregion GET


	//#region DEBUG

			public abstract void printAlignments();
			public abstract String printAlignmentsString(int indentation);
			public abstract void printAlignmentsFile(FileWriter file, int indentation);
	
	//#endregion DEBUG

}