package fr.labri.gumtree.matchers.optimal.zs;

import java.util.Arrays;
import java.util.Iterator;

import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;
import fr.labri.gumtree.matchers.Mapping;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.tree.ITree;

public class ZsNewMatcher extends Matcher {
	
	private static QGramsDistance qgramDist = new QGramsDistance();
	
	private ZsTree src, dst;
	
	private double[][] treeDist, forestDist;
	
	public ZsNewMatcher(ITree src, ITree dst) {
		super(src, dst);
		this.src = new ZsTree(src);
		this.dst = new ZsTree(dst);
	}
	
	private double[][] computeTreeDist() {
		treeDist = new double[src.size + 1][dst.size + 1];
		forestDist = new double[src.size + 1][dst.size + 1];
		
		for (int i = 1; i < src.kr.length; i++) {
			for (int j = 1; j < dst.kr.length; j++) {
				forestDist(src.kr[i], dst.kr[j]);
			}
		}
		
		return treeDist;	
	}
	
	private void forestDist(int i, int j) {
		forestDist[src.lmd[i] - 1][dst.lmd[j] - 1] = 0;
		for (int di = src.lmd[i]; di <= i; di++) {
			double costDel =  getDeletionCost(src.nodes[di]);
			forestDist[di][dst.lmd[j] - 1] = forestDist[di - 1][dst.lmd[j] - 1] + costDel;
			for (int dj = dst.lmd[j]; dj <= j; dj++) {
				double costIns = getInsertionCost(dst.nodes[dj]);
				forestDist[src.lmd[i] - 1][dj] = forestDist[src.lmd[i] - 1][dj - 1] + costIns; 
				
				if ((src.lmd[di] == src.lmd[i]) && (dst.lmd[dj] == dst.lmd[j])) {
					double costUpd = getUpdateCost(src.nodes[di], dst.nodes[dj]);
					forestDist[di][dj] = Math.min(Math.min(forestDist[di - 1][dj] + costDel, 
								forestDist[di][dj - 1] + costIns),
								forestDist[di - 1][dj - 1] + costUpd);
					treeDist[di][dj] = forestDist[di][dj];
				} else {
					forestDist[di][dj] = Math.min(Math.min(forestDist[di - 1][dj] + costDel,
								forestDist[di][dj - 1] + costIns),
								forestDist[src.lmd[di] - 1][dst.lmd[dj] -1] + 
								treeDist[di][dj]);
				}
			}
		}		
	}
	
	private double getDeletionCost(ITree n) {
		return 1D;
	}
	
	private double getInsertionCost(ITree n) {
		return 1D;
	}
	
	private double getUpdateCost(ITree n1, ITree n2) {
		if (n1.getType() == n2.getType())
			if ("".equals(n1.getLabel()) || "".equals(n2.getLabel())) 
				return 1D;
			else 
				return 1D - qgramDist.getSimilarity(n1.getLabel(), n2.getLabel());
		else 
			return Double.MAX_VALUE;
	}
	
	@Override
	public void match() {
		double[][] matrix = computeTreeDist();
		int prevCol = matrix[0].length - 1;
		int prevLine = matrix.length - 1;
		do {
			double l = prevCol == 0D ? Double.MAX_VALUE : matrix[prevLine][prevCol - 1];
			double u = prevLine == 0D ? Double.MAX_VALUE : matrix[prevLine - 1][prevCol];
			double lu = prevCol == 0D || prevLine == 0D ? Double.MAX_VALUE : matrix[prevLine - 1][prevCol - 1];
			if (l < u && l < lu) prevCol--; 
			else if (u < lu && u < l) prevLine--;
			else {
				//FIXME check mapping correctness
				if (src.nodes[prevLine].getType() == dst.nodes[prevCol].getType())
					addMapping(src.nodes[prevLine], dst.nodes[prevCol]);
				else
					System.err.println("Should not map incompatible nodes.");
				prevCol--;
				prevLine--;
			}
		} 
		while (prevCol >= 1 || prevLine >= 1);
	}
	
	private final class ZsTree {
		
		private ITree root;
		
		private ITree[] nodes;
		
		private int[] kr, lmd; // key-roots and left-most descendants
		
		private int size, leaves; // number of nodes and leave
		
		private ZsTree(ITree t) {
			root = t;
			size = t.getSize();
			leaves = t.getLeaves().size();
			nodes = new ITree[size + 1];
			lmd = new int[size + 1];
			Arrays.fill(lmd, -1);
			number(root, 1);
			keyRoots();
		}
		
		private int number(ITree t, int nb) {
			int l = nb;
			if (!t.isLeaf()) 
				for (ITree n: t.getChildren()) nb = number(n, nb);
			nodes[nb] = t;
			lmd[nb] = l;
			return nb + 1;
		}
		
		private void keyRoots() {
			kr = new int[leaves + 1];
			boolean[] visited = new boolean[size + 1];
			Arrays.fill(visited, false);
			int k = kr.length - 1;
			for (int i = size; i >= 1; i--) {
				if (!visited[lmd[i]]) {
					kr[k] = i;
					visited[lmd[i]] = true;
					k--;
				}
			}
			kr[0] = -1;
		}
		
	}

}
