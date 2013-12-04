class Test {
	
	void dispFoo(int z) {
		int y = getY();
		System.out.println(z+y);
	}
	
	int getY() {
		int x = 12;
		x++;
		x++;
		int y = x + 16;
		return y;
	}
	
}