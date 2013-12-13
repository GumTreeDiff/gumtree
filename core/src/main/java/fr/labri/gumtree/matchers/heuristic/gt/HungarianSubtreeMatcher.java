package fr.labri.gumtree.matchers.heuristic.gt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.labri.gumtree.algo.HungarianAlgorithm;
import fr.labri.gumtree.matchers.MultiMappingStore;
import fr.labri.gumtree.tree.Tree;

public class HungarianSubtreeMatcher extends SubtreeMatcher {

	public HungarianSubtreeMatcher(Tree src, Tree dst) {
		super(src, dst);
	}
	
	public void filterMappings(MultiMappingStore mmappings) {
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
		return 111D - sim(src, dst);
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
