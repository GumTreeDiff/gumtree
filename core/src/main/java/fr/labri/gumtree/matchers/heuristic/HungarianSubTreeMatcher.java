package fr.labri.gumtree.matchers.heuristic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.labri.gumtree.algo.HungarianAlgorithm;
import fr.labri.gumtree.matchers.composite.Matcher;
import fr.labri.gumtree.tree.MappingStore;
import fr.labri.gumtree.tree.MultiMappingStore;
import fr.labri.gumtree.tree.Tree;

public class HungarianSubTreeMatcher extends Matcher {

	private static final int MIN_HEIGHT = 1; 
	
	private int maxSize;

	public HungarianSubTreeMatcher(Tree src, Tree dst, MappingStore mappings) {
		super(src, dst, mappings);
		maxSize = Math.max(src.getSize(), dst.getSize());
		match();
	}

	public HungarianSubTreeMatcher(Tree src, Tree dst) {
		this(src, dst, new MappingStore());
	}

	public void match() {
		MultiMappingStore mmappings = new MultiMappingStore();
		List<Set<Tree>> srcs = decomposeTrees(src);
		List<Set<Tree>> dsts = decomposeTrees(dst);
		int startHeight = Math.min(srcs.size(), dsts.size()) - 1;

		for (int h = startHeight; h >= MIN_HEIGHT; h--) {
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

		// Select unique mappings first and extract ambiguous mappings
		List<MultiMappingStore> ambiguousList = new ArrayList<>();
		Set<Tree> ignored = new HashSet<>();
		for (Tree src: mmappings.getSrcs())
			if (mmappings.isSrcUnique(src))
				addFullMapping(src, mmappings.getDst(src).iterator().next());
			else if (!ignored.contains(src)) {
				MultiMappingStore ambiguous = new MultiMappingStore();
				Set<Tree> adsts = mmappings.getDst(src);
				Set<Tree> asrcs = mmappings.getSrc(mmappings.getDst(src).iterator().next());
				for (Tree asrc : asrcs) for(Tree adst: adsts) ambiguous.link(asrc ,adst);
				ambiguousList.add(ambiguous);
				ignored.addAll(asrcs);
			}
		
		Collections.sort(ambiguousList, new MultiMappingComparator());

		for (MultiMappingStore ambiguous: ambiguousList) {
			System.out.println("hungarian try.");
			List<Tree> lstSrcs = new ArrayList<>(ambiguous.getSrcs());
			List<Tree> lstDsts = new ArrayList<>(ambiguous.getDsts());
			double[][] matrix = new double[lstSrcs.size()][lstDsts.size()];
			for(int i = 0; i < lstSrcs.size(); i++) 
				for(int j = 0; j < lstDsts.size(); j++)
					matrix[i][j] = cost(lstSrcs.get(i), lstDsts.get(j));
			
			HungarianAlgorithm hgAlg = new HungarianAlgorithm(matrix);
			int[] solutions = hgAlg.execute();
			for (int i = 0; i < solutions.length; i++) {
				int dstIdx = solutions[i];
				if (dstIdx != -1) addFullMapping(lstSrcs.get(i), lstDsts.get(dstIdx));
			}
		}
		
	}

	private double cost(Tree src, Tree dst) {
		double jaccard = jaccardSimilarity(src.getParent(), dst.getParent());
		int posSrc = (src.isRoot()) ? 0 : src.getParent().getChildPosition(src);
		int posDst = (dst.isRoot()) ? 0 : dst.getParent().getChildPosition(dst);
		int maxSrcPos =  (src.isRoot()) ? 1 : src.getParent().getChildren().size();
		int maxDstPos =  (dst.isRoot()) ? 1 : dst.getParent().getChildren().size();
		int maxPosDiff = Math.max(maxSrcPos, maxDstPos);
		double pos = 1D - ((double) Math.abs(posSrc - posDst) / (double) maxPosDiff);
		double po = 1D - ((double) Math.abs(src.getId() - dst.getId()) / (double) this.maxSize);
		return 111D - (100 * jaccard + 10 * pos + po);
	}

	private static List<Set<Tree>> decomposeTrees(Tree root) {
		List<Set<Tree>> trees = new ArrayList<>(root.getHeight() + 1);
		for (int i = 0; i < root.getHeight() + 1; i++) trees.add(new HashSet<Tree>());
		for (Tree t: root.getTrees()) trees.get(t.getHeight()).add(t);
		return trees;
	}

	private class MultiMappingComparator implements Comparator<MultiMappingStore> {

		@Override
		public int compare(MultiMappingStore m1, MultiMappingStore m2) {
			return Integer.compare(impact(m1), impact(m2));
		}
		
		public int impact(MultiMappingStore m) {
			int impact = 0;
			for (Tree src: m.getSrcs()) {
				int pSize = src.getParents().size(); 
				if (pSize > impact) impact = pSize;
			}
			for (Tree src: m.getDsts()) {
				int pSize = src.getParents().size(); 
				if (pSize > impact) impact = pSize;
			}
			return impact;
		}
		
	}
	
}
