package fr.labri.gumtree.matchers.heuristic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import fr.labri.gumtree.matchers.Mapping;
import fr.labri.gumtree.matchers.MappingStore;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.MultiMappingStore;
import fr.labri.gumtree.tree.Tree;

public class GreedySubTreeMatcher extends Matcher {

	private static final int MIN_HEIGHT = 1;
	
	private int maxSize;
	
	private int maxHeight;

	public GreedySubTreeMatcher(Tree src, Tree dst, MappingStore mappings) {
		super(src, dst, mappings);
		maxSize = Math.max(src.getSize(), dst.getSize());
		maxHeight = Math.max(src.getHeight(), dst.getHeight());
		match();
	}

	public GreedySubTreeMatcher(Tree src, Tree dst) {
		this(src, dst, new MappingStore());
	}

	public void match() {
		MultiMappingStore mmappings = new MultiMappingStore();
		List<Set<Tree>> srcs = decomposeTrees(src);
		List<Set<Tree>> dsts = decomposeTrees(dst);

		//System.out.println("phase height");
		for (int h = maxHeight; h >= MIN_HEIGHT; h--) {
			Set<Tree> hSrcs = srcs.get(h);
			Set<Tree> hDsts = dsts.get(h);

			Set<Tree> hDelSrcs = new HashSet<>();
			Set<Tree> hDelDsts = new HashSet<>();


			for(Tree src: hSrcs) {
				for (Tree dst: hDsts) {
					if (src.isClone(dst)) {
						mmappings.link(src, dst);
						hDelSrcs.add(src);
						hDelDsts.add(dst);
					}
				}
			}

			for (Tree delSrc: hDelSrcs) for(Tree t: delSrc.getDescendants()) srcs.get(t.getHeight()).remove(t);
			for (Tree delDst: hDelDsts) for(Tree t: delDst.getDescendants()) dsts.get(t.getHeight()).remove(t);
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

	private List<Set<Tree>> decomposeTrees(Tree root) {
		List<Set<Tree>> trees = new ArrayList<>(maxHeight + 1);
		for (int i = 0; i < maxHeight + 1; i++) trees.add(new HashSet<Tree>());
		for (Tree t: root.getTrees()) trees.get(t.getHeight()).add(t);
		return trees;
	}
	
	private class MappingComparator implements Comparator<Mapping> {
		
		@Override
		public int compare(Mapping m1, Mapping m2) {
			return Double.compare(GreedySubTreeMatcher.this.sim(m2.getFirst(), m2.getSecond()), 
					GreedySubTreeMatcher.this.sim(m1.getFirst(), m1.getSecond()));
		}
		
	}

}
