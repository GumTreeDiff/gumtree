package fr.labri.gumtree.matchers.heuristic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import fr.labri.gumtree.matchers.composite.Matcher;
import fr.labri.gumtree.tree.Mapping;
import fr.labri.gumtree.tree.MappingStore;
import fr.labri.gumtree.tree.MultiMappingStore;
import fr.labri.gumtree.tree.Tree;

public class PrioritySubTreeMatcher extends Matcher {

	private static final int MIN_HEIGHT = 2;

	private int maxSize;

	public PrioritySubTreeMatcher(Tree src, Tree dst, MappingStore mappings) {
		super(src, dst, mappings);
		maxSize = Math.max(src.getSize(), dst.getSize());
		match();
	}

	public PrioritySubTreeMatcher(Tree src, Tree dst) {
		this(src, dst, new MappingStore());
	}
	
	private void popLarger(PriorityTreeList srcs, PriorityTreeList dsts) {
		if (srcs.peekHeight() > dsts.peekHeight()) srcs.open();
		else dsts.open();
	}

	public void match() {
		MultiMappingStore mmappings = new MultiMappingStore();
		
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
						mmappings.link(src, dst);
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

		// System.out.println("phase unique");
		// Select unique mappings first and extract ambiguous mappings
		List<Mapping> ambiguousList = new LinkedList<>();
		Set<Tree> ignored = new HashSet<>();
		for (Tree src: mmappings.getSrcs()) {
			if (mmappings.isSrcUnique(src))
				addFullMapping(src, mmappings.getDst(src).iterator().next());
			else if (!ignored.contains(src)) {
				Set<Tree> adsts = mmappings.getDst(src);
				Set<Tree> asrcs = mmappings.getSrc(mmappings.getDst(src).iterator().next());
				for (Tree asrc : asrcs) for(Tree adst: adsts) ambiguousList.add(new Mapping(asrc, adst));
				ignored.addAll(asrcs);
			}
		}

		// System.out.println("phase sorting");
		// Select the best ambiguous mappings
		Set<Tree> srcIgnored = new HashSet<>();
		Set<Tree> dstIgnored = new HashSet<>();
		Collections.sort(ambiguousList, new MappingComparator());

		// System.out.println("phase ambiguous");
		while (ambiguousList.size() > 0) {
			Mapping ambiguous = ambiguousList.remove(0);
			if (!(srcIgnored.contains(ambiguous.getFirst()) || dstIgnored.contains(ambiguous.getSecond()))) {
				addFullMapping(ambiguous.getFirst(), ambiguous.getSecond());
				srcIgnored.add(ambiguous.getFirst());
				dstIgnored.add(ambiguous.getSecond());
			}
		}
	}

	private double sim(Tree src, Tree dst) {
		double jaccard = jaccardSimilarity(src.getParent(), dst.getParent());
		int posSrc = (src.isRoot()) ? 0 : src.getParent().getChildPosition(src);
		int posDst = (dst.isRoot()) ? 0 : dst.getParent().getChildPosition(dst);
		int maxSrcPos =  (src.isRoot()) ? 1 : src.getParent().getChildren().size();
		int maxDstPos =  (dst.isRoot()) ? 1 : dst.getParent().getChildren().size();
		int maxPosDiff = Math.max(maxSrcPos, maxDstPos);
		double pos = 1D - ((double) Math.abs(posSrc - posDst) / (double) maxPosDiff);
		double po = 1D - ((double) Math.abs(src.getId() - dst.getId()) / (double) this.maxSize);
		return 100 * jaccard + 10 * pos + po;
	}

	private class MappingComparator implements Comparator<Mapping> {

		@Override
		public int compare(Mapping m1, Mapping m2) {
			return Double.compare(PrioritySubTreeMatcher.this.sim(m2.getFirst(), m2.getSecond()), 
					PrioritySubTreeMatcher.this.sim(m1.getFirst(), m1.getSecond()));
		}

	}

	private static class PriorityTreeList {

		private List<Tree>[] trees;

		private int maxHeight;

		private int currentIdx;

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
		
		public List<Tree> peek() {
			return (currentIdx == -1) ? null : trees[currentIdx];
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
