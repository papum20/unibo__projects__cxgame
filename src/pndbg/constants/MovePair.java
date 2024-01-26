package pndbg.constants;

import connectx.CXCell;


public class MovePair {
	public short i, j;
	
	//#region CONSTRUCTORS
		public MovePair() {}
		/*
		public MovePair(short i, short j) {
			this.i = i;
			this.j = j;
		}
		*/
		public MovePair(int i, int j) {
			this.i = (short)i;
			this.j = (short)j;
		}
		public MovePair(MovePair move) {
			this.i = move.i;
			this.j = move.j;
		}
		public MovePair(CXCell move) {
			this.i = (short)move.i;
			this.j = (short)move.j;
		}

		/**
		 * set i, j to move.i, move.j.
		 * @param move
		 */
		public MovePair reset(MovePair move) {
			this.i = move.i;
			this.j = move.j;
			return this;
		}
		public MovePair reset(int i, int j) {
			this.i = (short)i;
			this.j = (short)j;
			return this;
		}

		/**
		 * set to sum.
		 * @param move
		 * @param i
		 * @param j
		 */
		public void resetSum(MovePair move, int i, int j) {
			reset(move.i + i, move.j + j);	
		}
		public void resetSum(MovePair a, MovePair b) {
			reset(a.i + b.i, a.j + b.j);	
		}

		/**
		 * set to product.
		 * @param move
		 * @param i
		 * @param j
		 */
		public void resetProduct(MovePair move, int i, int j) {
			reset(move.i * i, move.j * j);	
		}

		/**
		 * Set to center, after applying a translation of distance along direction.
		 * Equal to sum(center, product(direction, distance)).
		 * @param center
		 * @param direction
		 * @param distance
		 */
		public MovePair resetToVector(MovePair center, MovePair direction, int distance) {
			return reset(center.i + direction.i * distance, center.j + direction.j * distance);	
		}

	//#endregion CONSTRUCTORS

	public MovePair getPair() {return this;}
	public String toString() {return "[" + i + "," + j + "]";}

	//#region MATH_OPERATIONS
		public boolean equals(MovePair move) {return i == move.i && j == move.j;}
		public void negate() {i = (short)(-i); j = (short)(-j);}
		public MovePair getNegative() {return new MovePair(-i, -j);}
		public MovePair sum(MovePair B) {
			this.i += B.i;
			this.j += B.j;
			return this;
		}
		public MovePair subtract(MovePair B) {
			this.i -= B.i;
			this.j -= B.j;
			return this;
		}
		public MovePair getSum(MovePair B) {
			return new MovePair(i + B.i, j + B.j);
		}
		public MovePair getSum(int i, int j) {
			return new MovePair(this.i + i, this.j + j);
		}
		public MovePair getDiff(MovePair B) {
			return new MovePair(i - B.i, j - B.j);
		}
		public MovePair getProduct(int t) {
			return new MovePair(i * t, j * t);
		}
	//#endregion MATH_OPERATIONS

	//#region BOUNDS

		/**
		 * `min.i` <= `max.i` && `min.j` <= `max.j`
		 * @param min
		 * @param max
		 * @return true if the `min` <= caller < `max`
		 */
		public boolean inBounds(MovePair min, MovePair max) {
			return i >= min.i && i < max.i && j >= min.j && j < max.j;
		}
		
		/**
		 * `min.i` <= `max.i` && `min.j` <= `max.j`
		 * @param min
		 * @param max
		 * @return true if the `min` <= caller <= `max`
		 */
		public boolean inBounds_included(MovePair min, MovePair max) {
			return i >= min.i && i <= max.i && j >= min.j && j <= max.j;
		}
		
		/**
		 * uses any `first`, `second`z
		 * @param min
		 * @param max
		 * @return true if `min(first.i,second.i)<=caller.i<=max(first.i,second.i)` (and the same for j)
		 */
		public boolean inBetween_included(MovePair first, MovePair second) {
			short imin, imax, jmin, jmax;
			if(first.i < second.i) {
				imin = first.i;		imax = second.i;
			} else {
				imin = second.i;	imax = first.i;
			}
			if(first.j < second.j) {
				jmin = first.j;		jmax = second.j;
			} else {
				jmin = second.j;	jmax = first.j;
			}
			return i >= imin && i <= imax && j >= jmin && j <= jmax;
		}
		
		public void clampMin(MovePair min) {
			if(min.i > i) i = min.i;
			if(min.j > j) j = min.j;
		}
		public void clampMax(MovePair max) {
			if(i >= max.i) i = (short)(max.i - 1);
			if(j >= max.j) j = (short)(max.j - 1);
		}
		public void clamp(MovePair min, MovePair max) {
			clampMin(min);
			clampMax(max);
		}
		
		public MovePair clamp_diag(MovePair min, MovePair max, MovePair dir, int distance) {
			short old_i = i, old_j = j;
			i = (short)Auxiliary.clamp(i + dir.i * distance, min.i, max.i);
			j = (short)Auxiliary.clamp(j + dir.j * distance, min.j, max.j);
			
			if(dir.i != 0 && dir.j != 0) {
				int diff_i = Math.abs(i - old_i), diff_j = Math.abs(j - old_j);
				if(diff_i < diff_j) {
					if(j < old_j) j = (short)(old_j - diff_i);
					else j = (short)(old_j + diff_i);
				}
				else if(diff_j < diff_i) {
					if(i < old_i) i = (short)(old_i - diff_j);
					else i = (short)(old_i + diff_j);
				}
			}
			return this;
		}
		
		public MovePair getDirection(MovePair target) {
			int dir_i, dir_j;
			if(target.i == i)		dir_i = 0;
			else if(target.i > i)	dir_i = 1;
			else					dir_i = -1;
			if(target.j == j)		dir_j = 0;
			else if(target.j > j)	dir_j = 1;
			else					dir_j = -1;
			return new MovePair(dir_i, dir_j);
		}

		/**
		 * difference (distance) from this to target (this excluded), along direction,
		 * assuming this and target are aligned on such direction;
		 * anyway the result rounded to axis with biggest diff.
		 */
		public int getDistanceInDir(MovePair target, MovePair dir) {
			if(Math.abs(i - target.i) > Math.abs(j - target.j))
				return (i - target.i) * dir.i;
			else
				return (j - target.j) * dir.j;
		}
		/** 
		 * abs(difference), excluded;
		 * rounded to axis with biggest diff.
		 */
		public int getDistanceAbs(MovePair target) {
			return Math.max(Math.abs(i - target.i), Math.abs(j - target.j));
		}

	//#endregion BOUDS

}