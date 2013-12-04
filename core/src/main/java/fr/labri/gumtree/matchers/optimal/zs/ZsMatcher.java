package fr.labri.gumtree.matchers.optimal.zs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import fr.labri.gumtree.matchers.composite.Matcher;
import fr.labri.gumtree.tree.Mapping;
import fr.labri.gumtree.tree.Tree;

public class ZsMatcher extends Matcher {
	
	private TreeInfo src, dst;
	
	private double[][] dist;
	
	private ComputeTreeCost cost;

	public ZsMatcher(Tree src, Tree dst, Set<Mapping> mappings) {
		super(src, dst);
		this.src = new TreeInfo(src);
		this.dst = new TreeInfo(dst);
		dist = new double[src.getSize()][dst.getSize()];
		cost = new ComputeTreeCost();
		for (int i = 0; i < dist.length; i++) Arrays.fill(dist[i], -1);
		match();
	}
	
	public ZsMatcher(Tree src, Tree dst) {
		this(src, dst, new HashSet<Mapping>());
	}
	
	private void computeMappings() {
		double[][] matrix = forestDist(src.size() - 1, dst.size() - 1);
		int prevCol = matrix[0].length - 1;
		int prevLine = matrix.length - 1;
		do {
			double l = prevCol == 0D ? Double.MAX_VALUE : matrix[prevLine][prevCol - 1];
			double u = prevLine == 0D ? Double.MAX_VALUE : matrix[prevLine - 1][prevCol];
			double lu = prevCol == 0D || prevLine == 0D ? Double.MAX_VALUE : matrix[prevLine - 1][prevCol - 1];
			if (l < u && l < lu) prevCol--; 
			else if (u < lu && u < l) prevLine--;
			else {
				//TODO check mapping correctness
				addMapping(src.nodes[prevLine - 1], dst.nodes[prevCol - 1]);
				prevCol--;
				prevLine--;
			}
		} 
		while (prevCol >= 1 || prevLine >= 1);
	}
	
	private double align() {
		int startKrDst = 0;
		for (; startKrDst < dst.kr.length; startKrDst++) if (dst.kr[startKrDst] != -1) break;
		
		// Do the cross product on key-roots
		for (int i = 0; i < src.kr.length; i++)
			if (src.kr[i] != -1) // only if they are valid
				for (int j = startKrDst; j < dst.kr.length; j++)
						treeDist(src.kr[i], dst.kr[j]);
		
		return dist[src.size() - 1][dst.size() - 1];
	}
	
	private double treeDist(int i, int j) {
		double d;
		if ((d = dist[i][j]) != -1) return d;
		int li = src.lmd[i];
		int lj = dst.lmd[j];
		
		int ishift = i - li + 2;		
		int jshift = j - lj + 2;
		
		double[][] fd = new double[ishift][jshift];
		
		for (int i1 = 1; i1 < ishift; i1++) fd[i1][0] = fd[i1 - 1][0] + src.deleteCost(i1 - 1);
		for (int j1 = 1; j1 < jshift; j1++) fd[0][j1] = fd[0][j1 - 1] + dst.insertCost(j1 - 1);		
		for (int k = li, kk = 1; k <= i; k++, kk++)
			for (int l = lj, ll = 1; l <= j; l++, ll++)
				if (src.lmd[k] == li && dst.lmd[l] == lj) {
					double ins = fd[kk - 1][ll] + src.deleteCost(k);
					double del = fd[kk][ll - 1] + dst.insertCost(l);
					double up = fd[kk - 1][ll - 1] + updateCost(k, l);
					
					dist[k][l] = fd[kk][ll] = Math.min(ins, Math.min(del, up));
				} else {
					double ins = fd[kk - 1][ll] + src.deleteCost(k);
					double del = fd[kk][ll - 1] + dst.insertCost(l);
					double up = fd[src.lmd[k] - li][dst.lmd[l] - lj] + treeDist(k, l);
					fd[kk][ll] = Math.min(ins, Math.min(del, up));					
				}
		
		return dist[i][j];
	}
	
	private double[][] forestDist(int i, int j) {
		int li = src.lmd[i];
		int lj = dst.lmd[j];
		
		int ishift = i - li + 2;		
		int jshift = j - lj + 2;
		
		double[][] fd = new double[ishift][jshift];
		
		for (int i1 = 1; i1 < ishift; i1++) fd[i1][0] = fd[i1 - 1][0] + src.deleteCost(i1 - 1);
		for (int j1 = 1; j1 < jshift; j1++) fd[0][j1] = fd[0][j1 - 1] + dst.insertCost(j1 - 1);
		for (int k = li, kk = 1; k <= i; k++, kk++)
			for (int l = lj, ll = 1; l <= j; l++, ll++)
				if (src.lmd[k] == li && dst.lmd[l] == lj) {
					double ins = fd[kk - 1][ll] + src.deleteCost(k);
					double del = fd[kk][ll - 1] + dst.insertCost(l);
					double up = fd[kk - 1][ll - 1] + updateCost(k, l);
					dist[k][l] = fd[kk][ll] = Math.min(ins, Math.min(del, up));
				} else {
					double ins = fd[kk - 1][ll] + src.deleteCost(k);
					double del = fd[kk][ll - 1] + dst.insertCost(l);
					double up = fd[src.lmd[k] - li][dst.lmd[l] - lj] + treeDist(k, l);
					fd[kk][ll] = Math.min(ins, Math.min(del, up));					
				}
		return fd;
	}
	
	private double updateCost(int node, int other) {
		return cost.updateCost(src.nodes[node], dst.nodes[other]);
	}

	@Override
	public void match() {
		align();
		computeMappings();
	}
	
	private final class TreeInfo {
		private Tree root;
		private Tree[] nodes;
		
		private int[] lmd; // left-most descendants
		private int[] kr; // key roots
		
		private TreeInfo(Tree t) {
			root = t;
			int size = t.getSize();
			nodes = new Tree[size];
			lmd = new int[size];
			kr = new int[size];
			Arrays.fill(kr, -1);
			postOrderNumbering(root, 0);
			Arrays.sort(kr);
		}
		
		private double insertCost(int n) {
			return cost.insertCost(nodes[n]);
		}

		private double deleteCost(int n) {
			return cost.deleteCost(nodes[n]);
		}
				
		private int size() {
			return nodes.length;
		}
		
		private int postOrderNumbering(Tree t, int nb) {
			int l = nb;
			if (!t.isLeaf()) for (Tree n: t.getChildren()) nb = postOrderNumbering(n, nb);
			nodes[nb] = t;
			lmd[nb] = l;
			kr[l] = nb;
			return nb + 1;
		}
	}

}
