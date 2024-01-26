package pndbg.delt2.threats;

import pndbg.constants.MovePair;



public class ThreatCells {

	public static enum USE {
		ATK,
		DEF,
		BTH
	}



	public MovePair[] related;
	public USE[] uses;			//0=attacker, 1=defender, 2=both
	public final byte type;
	
	
	
	public ThreatCells(int related, byte type) {
		this.related = new MovePair[related];
		uses = new USE[related];
		this.type = type;
	}

	public void set(MovePair cell, int index, USE use) {
		related[index] = cell;
		uses[index] = use;
	}

	public int nextAtk(int index) {
		while(index < related.length) {
			if(uses[index] != USE.DEF) return index;
			else index++;
		}
		return -1;
	}
	
	public int nextDef(int index) {
		while(index < related.length) {
			if(uses[index] != USE.ATK) return index;
			else index++;
		}
		return -1;
	}
	
	public MovePair[] getDefensive(int atk) {
		if(related.length <= 1) return null;
		MovePair[] defensive = new MovePair[related.length - 1];
		for(int i = 0; i < atk; i++) defensive[i] = related[i];
		for(int i = atk + 1; i < related.length; i++) defensive[i - 1] = related[i];
		return defensive;
	}
}