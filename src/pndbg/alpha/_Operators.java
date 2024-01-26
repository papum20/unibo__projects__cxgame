/*
 * un operatore è definito quando si ha una sequenza di caselle in fila con le seguenti proprietà:
 * 	-	primo e ultimo elemento
 * 	-	
 */


package pndbg.alpha;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import pndbg.alpha.threats.ThreatCells;
import pndbg.alpha.threats.ThreatPosition;




public abstract class _Operators {

	
	//public static final short MAX_LINE			= 0;	//max length of alignment (K+MAX_LINE) (including marks and spaces inside)
	public final short MAX_OUT_ONE_SIDE;			//max length by which a sequence of aligned symbols can extend, with empty squares
	public final short[] MAX_OUT_ONE_SIDE_PER_TIER;
	public final short MAX_OUT;						//left+right
	public final short[] MAX_OUT_PER_TIER;
	public final short MAX_IN;						//max number of missing symbols such that K-MAX_FREE_LINE is considered an alignment
	public final short MIN_LINED;
	public final short MAX_LINED;
	//min-max x such that there must be, for a threat, k-x marks aligned
	public final short MARK_DIFF_MIN;
	
	public final int TIER_N;	//number of tiers for alignments (K-1 to K-3, not excluding K even if it is won)
	public final byte MAX_TIER;



	protected _Operators(	short MAX_OUT_ONE_SIDE,
				short MAX_OUT,
				short MIN_LINED,
				short MAX_LINED,
				short MAX_IN,
				short MARK_DIFF_MIN,

				short[] MAX_OUT_ONE_SIDE_PER_TIER,
				short[] MAX_OUT_PER_TIER,
				
				int TIER_N,
				byte TIER_MAX
				) {
		this.MAX_OUT_ONE_SIDE	= MAX_OUT_ONE_SIDE;
		this.MAX_OUT			= MAX_OUT;
		this.MIN_LINED			= MIN_LINED;
		this.MAX_LINED			= MAX_LINED;
		this.MAX_IN				= MAX_IN;
		this.MARK_DIFF_MIN		= MARK_DIFF_MIN;
		
		this.MAX_OUT_ONE_SIDE_PER_TIER = MAX_OUT_ONE_SIDE_PER_TIER;
		this.MAX_OUT_PER_TIER	= MAX_OUT_PER_TIER;

		this.TIER_N				= TIER_N;
		this.MAX_TIER			= TIER_MAX;
	}
	
	/**
	 * return an array of alignment codes for the given tier.
	 */
	public abstract byte[] alignmentCodes(int tier);

	/**
	 * return an array of alignment patterns for the given tier.
	 */
	public abstract AlignmentsMap alignmentPatterns(int tier);
	
	/**
	 * return an array of threat appliers for the given tier.
	 */
	public abstract AppliersMap appliers(int tier);

	/**
	 * return an array of scores for each alignment in the given tier.
	 */
	public abstract int[] scores(int tier);
	 

	// 0...7 (also -8...-1)
	public abstract byte tier(byte threat);

	public abstract byte indexInTier(byte threat);

	public abstract int score(byte threat);

	public abstract <B extends _BoardBitDb<B, BB>, BB extends _BoardBit<BB>> ThreatCells applied(final _BoardBitDb<B, BB> board, ThreatPosition op, byte attacker, byte defender);



	//#region CLASSES

		//#region MAIN
			/**
			 * An AlignmentPattern is a way to recgnize and categorize an alignment.
			 * an operator ìs defined by the following parameters:
			 * 	line:	K+line aligned cells, i.e. longest allowed distance between two marks
			 * 	mark:	K+mark marked
			 * 	in:		free cells needed inside aligned cells (i.e. at least one mark before, one after)
			 * 	out:	tot free cells outside (before and after) aligned cells
			 * 	mnout:	min free cells per sided (min )
			 */
			public static class AlignmentPattern {
				public final short line, mark, in, out, mnout;
				protected AlignmentPattern(short line, short mark, short in, short out, short mnout) {
					this.line	= line;
					this.mark	= mark;
					this.in		= in;
					this.out	= out;
					this.mnout	= mnout;
				}
				protected AlignmentPattern(int line, int mark, int in, int out, int mnout) {
					this.line	= (short)line;
					this.mark	= (short) mark;
					this.in		= (short)in;
					this.out	= (short)out;
					this.mnout	= (short)mnout;
				}

				public boolean isCompatible(int X, int lined, int marks, int holes) {
					return lined == X + this.line && marks == X + this.mark && holes == this.in;
				}

				/**
				 * @param X
				 * @param lined
				 * @param marks
				 * @param before_exact
				 * @param after
				 * @return true if the alignment is compatible with exact `before` (but after can be bigger, and is calculated basing on before).
				 */
				public boolean isApplicableExactly(int X, int lined, int marks, int before_exact, int after) {
					return lined == X + this.line && marks == X + this.mark
						&& before_exact >= mnout
						&& out - before_exact >= mnout		// calculated after
						&& after >= out - before_exact;		// after can satisfy calculated after
				}
				
				@Override public String toString() {
					return line + "," + mark + "," + in + "," + out + "," + mnout;
				}
			}

			public static class AlignmentsMap extends HashMap<Integer, AlignmentPattern> {
				protected AlignmentsMap(byte[] keys, AlignmentPattern[] values) {
					super(keys.length);
					for(int i = 0; i < keys.length; i++)
						put((int)(keys[i]), values[i]);
				}
			}
			public static interface Applier {
				//given a board and and an alignment relative to it,
				//returns a threatArray, that contains the cells to mark to apply an operator
				public <B extends _BoardBitDb<B, BB>, BB extends _BoardBit<BB>> ThreatCells getThreatCells(final _BoardBitDb<B, BB> board, ThreatPosition pos, byte attacker, byte defender);
			}

			public static class AppliersMap extends HashMap<Integer, Applier> {
				protected AppliersMap(byte[] keys, Applier[] values) {
					super(keys.length);
					for(int i = 0; i < keys.length; i++)
						put((int)(keys[i]), values[i]);
				}
			}

			public class ThreatsByRank extends ArrayList<LinkedList<ThreatCells>> {

				public ThreatsByRank() {
					super(TIER_N);
					for(int i = 0; i < TIER_N; i++) add(i, null);
				}
				public void add(ThreatCells threat) {
					int tier = tier(threat.type);
					if(get(tier) == null) set(tier, new LinkedList<ThreatCells>());
					get(tier).add(threat);
				}
			}

		//#endregion MAIN
		
	//#endregion CLASSES

}
