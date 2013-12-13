package fr.labri.gumtree.matchers.heuristic.gt;

import java.util.ArrayList;
import java.util.List;

import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.MultiMappingStore;
import fr.labri.gumtree.tree.Tree;

public abstract class SubtreeMatcher extends Matcher {

	private static final int MIN_HEIGHT = 1;

	public SubtreeMatcher(Tree src, Tree dst) {
		super(src, dst);
	}
	
	private void popLarger(PriorityTreeList srcs, PriorityTreeList dsts) {
		if (srcs.peekHeight() > dsts.peekHeight()) srcs.open(); else dsts.open();
	}

	public void match() {
		MultiMappingStore multiMappings = new MultiMappingStore();
		
		PriorityTreeList srcs = new PriorityTreeList(src);
		PriorityTreeList dsts = new PriorityTreeList(dst);
		
		while (srcs.peekHeight() != -1 && dsts.peekHeight() != -1) {
			while (srcs.peekHeight() != dsts.peekHeight()) popLarger(srcs, dsts);
			
			List<Tree> hSrcs = srcs.pop();
			List<Tree> hDsts = dsts.pop();
			
			boolean[] srcMarks = new boolean[hSrcs.size()];
			boolean[] dstMarks = new boolean[hDsts.size()];
			
			for (int i = 0; i < hSrcs.size(); i++) {
				for (int j = 0; j < hDsts.size(); j++) {
					Tree src = hSrcs.get(i);
					Tree dst = hDsts.get(j);
					
					if (src.isClone(dst)) {
						multiMappings.link(src, dst);
						srcMarks[i] = true;
						dstMarks[j] = true;
					}
				}
			}
			
			for (int i = 0; i < srcMarks.length; i++) if (srcMarks[i] == false) srcs.open(hSrcs.get(i));
			for (int i = 0; i < dstMarks.length; i++) if (dstMarks[i] == false) dsts.open(hDsts.get(i));
			srcs.updateHeight();
			dsts.updateHeight();
		}

		filterMappings(multiMappings);
	}
	
	public abstract void filterMappings(MultiMappingStore mmappings);

	protected double sim(Tree src, Tree dst) {
		double jaccard = jaccardSimilarity(src.getParent(), dst.getParent());
		int posSrc = (src.isRoot()) ? 0 : src.getParent().getChildPosition(src);
		int posDst = (dst.isRoot()) ? 0 : dst.getParent().getChildPosition(dst);
		int maxSrcPos =  (src.isRoot()) ? 1 : src.getParent().getChildren().size();
		int maxDstPos =  (dst.isRoot()) ? 1 : dst.getParent().getChildren().size();
		int maxPosDiff = Math.max(maxSrcPos, maxDstPos);
		double pos = 1D - ((double) Math.abs(posSrc - posDst) / (double) maxPosDiff);
		double po = 1D - ((double) Math.abs(src.getId() - dst.getId()) / (double) this.getMaxTreeSize());
		return 100 * jaccard + 10 * pos + po;
	}
	
	protected int getMaxTreeSize() {
		return Math.max(src.getSize(), dst.getSize());
	}

	private static class PriorityTreeList {

		private List<Tree>[] trees;

		private int maxHeight;

		private int currentIdx;

		@SuppressWarnings("unchecked")
		public PriorityTreeList(Tree tree) {
			trees = (List<Tree>[]) new ArrayList[tree.getHeight() - MIN_HEIGHT + 1];
			maxHeight = tree.getHeight();
			addTree(tree);
		}

		private int idx(Tree tree) {
			return idx(tree.getHeight());
		}

		private int idx(int height) {
			return maxHeight - height;
		}

		private int height(int idx) {
			return maxHeight - idx;
		}

		private void addTree(Tree tree) {
			if (tree.getHeight() >= MIN_HEIGHT) {
				int idx = idx(tree);
				if (trees[idx] == null) trees[idx] = new ArrayList<Tree>();
				trees[idx].add(tree);
			}
		}

		public List<Tree> open() {
			List<Tree> pop = pop();
			if (pop != null) {
				for (Tree tree: pop) open(tree);
				updateHeight();
				return pop;
			} else return null;
		}
		
		public List<Tree> pop() {
			if (currentIdx == -1) 
				return null;
			else {
				List<Tree> pop = trees[currentIdx];
				trees[currentIdx] = null;
				return pop;
			}
		}
		
		public void open(Tree tree) {
			for(Tree c: tree.getChildren()) addTree(c);
		}
		
		public int peekHeight() {
			return (currentIdx == -1) ? -1 : height(currentIdx);
		}
		
		public void updateHeight() {
			currentIdx = -1;
			for (int i = 0; i < trees.length; i++) {
				if (trees[i] != null) {
					currentIdx = i;
					break;
				}
			}
		}

	}

}
