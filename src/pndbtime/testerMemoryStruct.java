package pndbtime;



public class testerMemoryStruct extends Object {
	long max, allocated, free, real_free;

	public testerMemoryStruct(long max, long allocated, long free, long real_free) {
		this.max = max;
		this.allocated = allocated;
		this.free = free;
		this.real_free = real_free;
	}
	
	public void set(long max, long allocated, long free, long real_free) {
		this.max = max;
		this.allocated = allocated;
		this.free = free;
		this.real_free = real_free;
	}

	@Override
	public String toString() {
		return max + " " + allocated + " " + free + " " + real_free;
	}

}
