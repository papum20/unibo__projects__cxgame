package pndbtime.threats;

import pndbtime.constants.MovePair;



public class Threat {

	public static enum USE {
		ATK,
		DEF,
		BTH
	}



	public MovePair[] related;
	public USE[] uses;			//0=attacker, 1=defender, 2=both
	public final byte type;
	
	
	
	public Threat(int related, byte type) {
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
	
	/**
	 * Complexity: O(related.length)
	 * @param atk
	 * @return
	 */
	public MovePair[] getDefensive(int atk) {
		if(related.length <= 1) return null;
		MovePair[] defensive = new MovePair[related.length - 1];
		for(int i = 0; i < atk; i++) defensive[i] = related[i];
		for(int i = atk + 1; i < related.length; i++) defensive[i - 1] = related[i];
		return defensive;
	}

	@Override
	public String toString() {
		String res = "type: " + type + ", related: ";
		for(MovePair m : related) res += m + ", ";
		return res;
	}
}