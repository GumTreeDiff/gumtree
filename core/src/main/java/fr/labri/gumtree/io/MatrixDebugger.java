package fr.labri.gumtree.io;

public final class MatrixDebugger {
	
	private MatrixDebugger() {
	}
	
	public static void dump(Object[][] mat) {
		for (Object[] r : mat) {
			for (Object l: r) System.out.print(l + "\t");
			System.out.println();
		}
	}
	
	public static void dump(boolean[][] mat) {
		for (boolean[] r : mat) {
			for (boolean l: r) System.out.print(l + "\t");
			System.out.println();
		}
	}
	
	public static void dump(double[][] mat) {
		System.out.println("---");
		for (double[] r : mat) {
			for (double l: r) System.out.print(l + "\t");
			System.out.println();
		}
		System.out.println("---");
	}

}
