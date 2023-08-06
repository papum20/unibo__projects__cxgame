/*
 * un operatore è definito quando si ha una sequenza di caselle in fila con le seguenti proprietà:
 * 	-	primo e ultimo elemento
 * 	-	
 */


package pndb.alpha;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import pndb.alpha.threats.ThreatCells;
import pndb.alpha.threats.ThreatPosition;
import pndb.alpha.threats.ThreatCells.USE;
import pndb.constants.MovePair;




public class Operators {

	//byte CODES FOR THREATS (ALIGNMENTS)
	public static final byte LINE_0		= 0;	//xxxxx				k
	public static final byte LINE_1F	= 16;	//_xxxx_			straight k-1
	public static final byte LINE_1a	= 17;	//_xxxx				k-1
	public static final byte LINE_1b	= 18;	//xx_xx				k-1
	public static final byte LINE_2B	= 32;	//_xx_x_			broken	k-2
	public static final byte LINE_2		= 33;	//__xxx_			2	replies	k-2
	public static final byte LINE_2T	= 34;	//__xxx__			3 	replies	k-2
	public static final byte LINE_21a	= 35;	//xxx__				any k-2 in k
	public static final byte LINE_21b	= 36;	//xx_x_				any k-2 in k
	public static final byte LINE_21c	= 37;	//xx__x				any k-2 in k
	public static final byte LINE_3B	= 48;	//_x_x__			broken	k-3
	public static final byte LINE_3B2	= 49;	//_x__x_			broken	k-3 2-holes
	public static final byte LINE_3		= 50;	//__xx__			2 replies k-3
	public static final byte LINE_32	= 51;	//_xx___			any k-3 in k-1 with 2 empty boders
	public static final byte LINE_3T	= 52;	//__xx___			3 replies k-3
	public static final byte LINE_3Bb	= 53;	//__x_x___			3 replies	k-2
	/*
	public static final byte THREAT_0	= 0;	//xxx_x		xxxXx		k					PREREQUISITE: LINE_1|any(TIER 1)
	public static final byte THREAT_1F	= 16;	//_x_xx_	_xXxx_		straight k-1		PREREQUISITE: LINE_2|LINE_2B|any(TIER 2)
	public static final byte THREAT_1	= 17;	//_x_xx		XxOxx		k-1					PREREQUISITE: LINE_23|any(TIER 2)
	public static final byte THREAT_2B	= 32;	//_x__x_	_OxXOxO_	broken		k-2		PREREQUISITE: LINE_3B|LINE_3B2|LINE_32|any(TIER 3)
	public static final byte THREAT_2	= 33;	//__x_x__	_OxXxO_		2 replies	k-2			PREREQUISITE: ...
	public static final byte THREAT_2T	= 34;	//__x_x___	_OxXxOO_	3 replies	k-2		PREREQUISITE: ...
	*/
	
	private static final byte THREAT_TIER_MASK	= (byte)240;		//(bin)11110000
	private static final byte THREAT_INDEX_MASK	= (byte)15;			//(bin)00001111

	
	
	//public static final short MAX_LINE				= 0;	//max length of alignment (K+MAX_LINE) (including marks and spaces inside)
	public static final short MAX_FREE_EXTRA		= 3;	//max length by which a sequence of aligned symbols can extend, with empty squares
	public static final short MAX_FREE_EXTRA_TOT	= 5;	//left+right
	public static final short MAX_FREE_IN			= 2;	//max number of missing symbols such that K-MAX_FREE_LINE is considered an alignment
	//MAX_LINE = K
	//min-max x such that there must be, for a threat, k-x marks aligned
	public static final short MARK_DIFF_MIN	= 3;
	public static final short MARK_DIFF_MAX	= 0;
	
	// STATIC INSTANCES OF APPLIERS
	private static ApplierNull				applierNull				= new ApplierNull();
	private static Applier1first			applier1first			= new Applier1first();
	private static Applier1in				applier1in				= new Applier1in();
	private static Applier1second			applier1second			= new Applier1second();
	private static Applier1_1first_or_in	applier1_1first_or_in	= new Applier1_1first_or_in();
	private static Applier1_1in_or_in		applier1_1in_or_in		= new Applier1_1in_or_in();
	private static Applier1_3second_or_in	applier1_3second_or_in	= new Applier1_3second_or_in();
	private static Applier1_3in_or_in		applier1_3in_or_in		= new Applier1_3in_or_in();
	private static Applier1_2third			applier1_2third			= new Applier1_2third();
	private static Applier1_2in				applier1_2in			= new Applier1_2in();

	

	
	// ARRAY OF ALIGNMENTS (REPRESENTED AS CODES), GROUPED BY TIER
	public static final byte[][] ALIGNMENT_CODES = {
		new byte[]{LINE_0},
		new byte[]{LINE_1F, LINE_1a, LINE_1b},
		new byte[]{LINE_2B, LINE_2, LINE_2T, LINE_21a, LINE_21b, LINE_21c},
		new byte[]{LINE_3B, LINE_3B2, LINE_3, LINE_32, LINE_3T, LINE_3Bb}
	};
	// ARRAY OF ALIGNMENTS (REPRESENTED AS class Alignment), GROUPED BY TIER
	public static final AlignmentsMap[] ALIGNMENTS = {
		//TIER 0
		new AlignmentsMap(
			ALIGNMENT_CODES[0],
			new AlignmentPattern[]{
				new AlignmentPattern(0, 0, 0, 0, 0)	//xxxxx
			}
		),
		//TIER 1
		new AlignmentsMap(
			ALIGNMENT_CODES[1],
			new AlignmentPattern[]{
				new AlignmentPattern(-1, -1, 0, 2, 1),		//_xxxx_
				new AlignmentPattern(-1, -1, 0, 1, 0),		//xxxx_
				new AlignmentPattern(0, -1, 1, 0, 0)		//xxx_x
			}
		),
		//TIER 2
		new AlignmentsMap(
			ALIGNMENT_CODES[2],
			new AlignmentPattern[]{
				new AlignmentPattern(-1, -2, 1, 2, 1),		//_xx_x_
				new AlignmentPattern(-2, -2, 0, 3, 1),		//_xxx__
				new AlignmentPattern(-2, -2, 0, 4, 2),		//__xxx__
				new AlignmentPattern(-2, -2, 0, 2, 0),		//xxx__
				new AlignmentPattern(-1, -2, 1, 1, 0),		//xx_x_
				new AlignmentPattern(0, -2, 2, 0, 0)	//xx__x
			}
		),
		//TIER 3
		new AlignmentsMap(
				ALIGNMENT_CODES[3],
			new AlignmentPattern[]{
				new AlignmentPattern(-2, -3, 1, 3, 1),	//_x_x__
				new AlignmentPattern(-1, -3, 2, 2, 1),	//_x__x_
				new AlignmentPattern(-3, -3, 0, 4, 2),	//__xx__
				new AlignmentPattern(-3, -3, 0, 4, 1),	//_xx___
				new AlignmentPattern(-3, -3, 0, 5, 2),	//__xx___
				new AlignmentPattern(-2, -3, 1, 4, 2)		//__x_x__->_oxxxo_
			}
		)
	};
	
	
	/*
	 * also, are defined some alignments which must respect precise patterns:
	 * 	line[]:	aligned cells pattern
	 * 	out:	tot free cells outside (before and after) aligned cells
	 * 	mnout:	min free cells per sided (min )
	 */
	
	public static final int TIER_N = ALIGNMENT_CODES.length;	//number of tiers for alignments (K-1 to K-3, not excluding K even if it is won)
	public static final byte TIER_MAX = (byte)TIER_N;
	
	// ARRAY OF OPERATORS, GROUPED BY TIER, i.e. APPLICATIONS OF CONVERSIONS FROM ALIGNMENTS TO THREATS
	public static final AppliersMap[] OPERATORS = {
		//TIER 0
		new AppliersMap(
			ALIGNMENT_CODES[0],
			new Applier[]{
				applierNull				//xxxxx->do nothigh
			}
		),
		//TIER 1
		new AppliersMap(
			ALIGNMENT_CODES[1],
			new Applier[]{
				applierNull,			//_xxxx_->do nothing (implicit in [1])
				applier1first,			//xxxx_ ->xxxxX
				applier1in				//xxx_x ->xxxXx
			}
		),
		//TIER 2
		new AppliersMap(
			ALIGNMENT_CODES[2],
			new Applier[]{
				applier1in,				//_xx_x_->_xxXx_
				applier1second,			//_xxx__->_xxxX_
				applierNull,			//__xxx__->do nothing (implicit in [1])
				applier1_1first_or_in,	//xxx__->xxxXO/xxxOX
				applier1_1first_or_in,	//xx_x_->xxXxO/xxOxX
				applier1_1in_or_in		//xx__x->xxxOx/xxOXx
			}
		),
		//TIER 3
		new AppliersMap(
			ALIGNMENT_CODES[3],
			new Applier[]{
				applier1_3second_or_in,		//_x_x__ ->OxXxOO/OxOxXO
				applier1_3in_or_in,			//_x__x_ ->OxXOxO/OxOXxO
				applierNull,				//__xx__ ->do nothing (implicit in [3])
				applier1_3second_or_in,		//_xx___ ->OxxXOO/OxxOXO
				applier1_2third,			//__xx___->_OxxXO_
				applier1_2in				//__x_x__->_OxXxO_
			}
		)
	};

	// ARRAY OF ALIGNMENTS, GROUPED BY TIER, REPRESENTED AS SCORES (USED AS HEURISTIC FOR MOVE ORDERING)
	// 0 = not considered
	public static final int[][] ALIGNMENT_SCORES = {
		//TIER 0
		/*
		{
			0
		},
		*/
		//TIER 1
		{
			0,
			0,
			0
		},
		//TIER 2
		{
			2,		//_xx_x_->_xxXx_
			3,		//_xxx__->_xxxX_
			4,		//__xxx__
			0,
			0,
			0
		},
		//TIER 3
		{
			0,
			0,
			2,		//__xx__
			0,
			0,
			1		//__x_x__
		}
	};
	
	
	
	// 0...7 (also -8...-1)
	public static byte tier(byte threat) {
		return (byte)((threat & THREAT_TIER_MASK) >> (byte)4);
	}
	public static byte indexInTier(byte threat) {
		return (byte)(threat & THREAT_INDEX_MASK);
	}

	public static int score(byte threat) {
		return ALIGNMENT_SCORES[tier(threat)][indexInTier(threat)];
	}

	public static <B extends _BoardBitDb<B, BB>, BB extends _BoardBit<BB>> ThreatCells applied(final _BoardBitDb<B, BB> board, ThreatPosition op, byte attacker, byte defender) {
		try {
			return OPERATORS[tier(op.type)].get((int)(op.type)).getThreatCells(board, op, attacker, defender);
		} catch(Exception e) {
			board.print();
			board.printAlignments();
			System.out.println(op + " ... " + attacker);
			throw e;
		}
	}
	//public static CXCell threat(CXCell[] operator, CXCellState attacker) {
	//	for(CXCell cell : operator)
	//		if(cell.state == attacker) return cell;
	//	return null;
	//}



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
				private AlignmentPattern(short line, short mark, short in, short out, short mnout) {
					this.line = line;
					this.mark = mark;
					this.in = in;
					this.out = out;
					this.mnout = mnout;
				}
				private AlignmentPattern(int line, int mark, int in, int out, int mnout) {
					this.line = (short)line;
					this.mark = (short) mark;
					this.in = (short)in;
					this.out = (short)out;
					this.mnout = (short)mnout;
				}
				@Override public String toString() {
					return line + "," + mark + "," + in + "," + out + "," + mnout;
				}
			}
			public static class AlignmentsMap extends HashMap<Integer, AlignmentPattern> {
				private AlignmentsMap(byte[] keys, AlignmentPattern[] values) {
					super(keys.length);
					for(int i = 0; i < keys.length; i++)
						put((int)(keys[i]), values[i]);
				}
			}
			private static interface Applier {
				//given a board and and an alignment relative to it,
				//returns a threatArray, that contains the cells to mark to apply an operator
				public <B extends _BoardBitDb<B, BB>, BB extends _BoardBit<BB>> ThreatCells getThreatCells(final _BoardBitDb<B, BB> board, ThreatPosition pos, byte attacker, byte defender);
			}
			public static class AppliersMap extends HashMap<Integer, Applier> {
				private AppliersMap(byte[] keys, Applier[] values) {
					super(keys.length);
					for(int i = 0; i < keys.length; i++)
						put((int)(keys[i]), values[i]);
				}
			}

			public static class ThreatsByRank extends ArrayList<LinkedList<ThreatCells>> {

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
		//#region APPLIERS
			private static class ApplierNull implements Applier {
				public <B extends _BoardBitDb<B, BB>, BB extends _BoardBit<BB>> ThreatCells getThreatCells(final _BoardBitDb<B, BB> board, ThreatPosition pos, byte attacker, byte defender) {
					return null;
				}
			}
			private static class Applier1first implements Applier {
				public <B extends _BoardBitDb<B, BB>, BB extends _BoardBit<BB>> ThreatCells getThreatCells(final _BoardBitDb<B, BB> board, ThreatPosition pos, byte attacker, byte defender) {
					ThreatCells res = new ThreatCells(1, pos.type);
					if(board.cellFree(pos.start.i, pos.start.j))	res.set(pos.start, 0, USE.ATK);
					else											res.set(pos.end, 0, USE.ATK);
					return res;
				}
			}
			private static class Applier1in implements Applier {
				public <B extends _BoardBitDb<B, BB>, BB extends _BoardBit<BB>> ThreatCells getThreatCells(final _BoardBitDb<B, BB> board, ThreatPosition pos, byte attacker, byte defender) {
					ThreatCells res = new ThreatCells(1, pos.type);
					MovePair dir	= pos.start.getDirection(pos.end);
					MovePair it		= pos.start.getSum(dir);
					//doesn't check termination condition ( && !it.equals(op.end)): assumes the operator is appliable
					while(!board.cellFree(it.i, it.j)) it.sum(dir);
					//if(it.equals(op.end))	return null;
					res.set(it, 0, USE.ATK);
					return res;
				}
			}
			private static class Applier1second implements Applier {
				public <B extends _BoardBitDb<B, BB>, BB extends _BoardBit<BB>> ThreatCells getThreatCells(final _BoardBitDb<B, BB> board, ThreatPosition pos, byte attacker, byte defender) {
					ThreatCells res	= new ThreatCells(1, pos.type);
					MovePair dir	= pos.start.getDirection(pos.end);
					MovePair cell	= pos.start.getSum(dir);
					if(!board.cellFree(cell.i, cell.j)) cell = pos.end.getDiff(dir);
					res.set(cell, 0, USE.ATK);
					return res;
				}
			}
			//like 1kc, but starts from the free border
			private static class Applier1_1first_or_in implements Applier {
				public <B extends _BoardBitDb<B, BB>, BB extends _BoardBit<BB>> ThreatCells getThreatCells(final _BoardBitDb<B, BB> board, ThreatPosition pos, byte attacker, byte defender) {
					ThreatCells res = new ThreatCells(2, pos.type);
					MovePair dir;
					MovePair it;
					if (board.cellFree(pos.start.i, pos.end.j)) {
						it = new MovePair(pos.start);
						dir = pos.start.getDirection(pos.end);
					} else {
						it = new MovePair(pos.end);
						dir = pos.end.getDirection(pos.start);
					}
					int len = 0;
					//doesn't check termination condition ( && !it.equals(op.end)): assumes the operator is appliable
					while(len < 2) {
						if(board.cellFree(it.i, it.j)) res.set(new MovePair(it), len++, USE.BTH);
						//if(it.equals(op.end)) len = 2;	//exit while
						it.sum(dir);
					}
					return res;
				}
			}
			private static class Applier1_1in_or_in implements Applier {
				public <B extends _BoardBitDb<B, BB>, BB extends _BoardBit<BB>> ThreatCells getThreatCells(final _BoardBitDb<B, BB> board, ThreatPosition pos, byte attacker, byte defender) {
					ThreatCells res = new ThreatCells(2, pos.type);
					MovePair dir = pos.start.getDirection(pos.end);
					MovePair it = new MovePair(pos.start);
					int len = 0;
					//doesn't check termination condition ( && !it.equals(op.end)): assumes the operator is appliable
					while(len < 2) {
						if(board.cellFree(it.i, it.j)) res.set(new MovePair(it), len++, USE.BTH);
						//if(it.equals(op.end)) len = 2;	//exit while
						it.sum(dir);
					}
					return res;
				}
			}
			private static class Applier1_3second_or_in implements Applier {
				public <B extends _BoardBitDb<B, BB>, BB extends _BoardBit<BB>> ThreatCells getThreatCells(final _BoardBitDb<B, BB> board, ThreatPosition pos, byte attacker, byte defender) {
					ThreatCells res = new ThreatCells(4, pos.type);
					MovePair dir = pos.start.getDirection(pos.end);
					MovePair it = pos.start.getSum(dir);
					if (!board.cellFree(it.i, it.j)) {
						it.reset(pos.end.i - dir.i, pos.end.j - dir.j);
						dir.negate();
					}
					// DOESN'T PUT res IN ORDER WHEN START AND END ARE INVERTED
					res.set(pos.start, 0, USE.DEF); 
					res.set(new MovePair(it), 2, USE.BTH); 
					int ind = 1;
					//doesn't check termination condition ( && !it.equals(op.end)): assumes the operator is appliable
					while(ind < 3) {
						if(board.cellFree(it.i, it.j)) ind++;
						//if(it.equals(op.end)) len = 2;	//exit while
						if(ind < 3) it.sum(dir);
					}
					res.set(it, 1, USE.BTH); 
					res.set(pos.end, 3, USE.DEF); 
					return res;
				}
			}
			private static class Applier1_3in_or_in implements Applier {
				public <B extends _BoardBitDb<B, BB>, BB extends _BoardBit<BB>> ThreatCells getThreatCells(final _BoardBitDb<B, BB> board, ThreatPosition pos, byte attacker, byte defender) {
					ThreatCells res = new ThreatCells(4, pos.type);
					MovePair dir = pos.start.getDirection(pos.end);
					MovePair it = new MovePair(pos.start);
					res.set(pos.start, 0, USE.DEF);
					int ind = 1;
					//doesn't check termination condition ( && !it.equals(op.end)): assumes the operator is appliable
					while(ind < 3) {
						it.sum(dir);
						if(board.cellFree(it.i, it.j)) res.set(new MovePair(it), ind++, USE.BTH);
						//if(it.equals(op.end)) len = 2;	//exit while
					}
					res.set(pos.end, 3, USE.DEF);
					return res;
				}
			}
			private static class Applier1_2third implements Applier {
				public <B extends _BoardBitDb<B, BB>, BB extends _BoardBit<BB>> ThreatCells getThreatCells(final _BoardBitDb<B, BB> board, ThreatPosition pos, byte attacker, byte defender) {
					ThreatCells res = new ThreatCells(3, pos.type);
					MovePair dir = pos.start.getDirection(pos.end);
					res.set(pos.start.getSum(dir), 0, USE.DEF);
					if (board.cellFree(pos.start.i + 2*dir.i, pos.start.j + 2*dir.j)) {
						res.set(new MovePair(pos.start.i + 2*dir.i, pos.start.j + 2*dir.j), 1, USE.ATK);
					} else {
						res.set(new MovePair(pos.end.i - 2*dir.i, pos.end.j - 2*dir.j), 1, USE.ATK);
					}
					res.set(pos.end.getDiff(dir), 2, USE.DEF);
					return res;
				}
			}
			private static class Applier1_2in implements Applier {
				public <B extends _BoardBitDb<B, BB>, BB extends _BoardBit<BB>> ThreatCells getThreatCells(final _BoardBitDb<B, BB> board, ThreatPosition pos, byte attacker, byte defender) {
					ThreatCells res = new ThreatCells(3, pos.type);
					MovePair dir = pos.start.getDirection(pos.end);
					MovePair it = pos.start.getSum(dir);
					res.set(new MovePair(it), 0, USE.DEF);
					int ind = 0;
					//doesn't check termination condition ( && !it.equals(op.end)): assumes the operator is appliable
					while(ind < 2) {
						if(board.cellFree(it.i, it.j)) ind++;
						//if(it.equals(op.end)) len = 2;	//exit while
						if(ind < 2) it.sum(dir);
					}
					res.set(it, 1, USE.ATK);
					res.set(pos.end.getDiff(dir), 2, USE.DEF);
					return res;
				}
			}
			
		//#endregion APPLIERS
	//#endregion CLASSES

}
