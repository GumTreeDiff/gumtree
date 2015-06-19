package fr.labri.gumtree.matchers.heuristic.gt;

import fr.labri.gumtree.matchers.Mapping;
import fr.labri.gumtree.matchers.MappingStore;
import fr.labri.gumtree.matchers.MultiMappingStore;
import fr.labri.gumtree.tree.ITree;
import fr.labri.gumtree.tree.Pair;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.*;

public class CliqueSubtreeMatcher extends SubtreeMatcher {
	
	public CliqueSubtreeMatcher(ITree src, ITree dst, MappingStore store) {
		super(src, dst, store);
	}

	@Override
	public void filterMappings(MultiMappingStore mmappings) {
		TIntObjectHashMap<Pair<List<ITree>, List<ITree>>> cliques = new TIntObjectHashMap<>();
		for (Mapping m : mmappings) {
			int hash = m.getFirst().getHash();
			if (!cliques.containsKey(hash))
				cliques.put(hash, new Pair(new ArrayList<ITree>(), new ArrayList<ITree>()));
			cliques.get(hash).getFirst().add(m.getFirst());
			cliques.get(hash).getSecond().add(m.getSecond());
		}
		
		List<Pair<List<ITree>, List<ITree>>> ccliques = new ArrayList<>();
		
		for (int hash : cliques.keys()) {
			Pair<List<ITree>, List<ITree>> clique = cliques.get(hash);
			if (clique.getFirst().size() == 1 && clique.getSecond().size() == 1) {
				addFullMapping(clique.getFirst().get(0), clique.getSecond().get(0));
				cliques.remove(hash);
			} 
			else
				ccliques.add(clique);
		}
		
		Collections.sort(ccliques, new CliqueComparator());
		
		for (Pair<List<ITree>, List<ITree>> clique : ccliques) {
			List<Mapping> cliqueAsMappings = fromClique(clique);
			Collections.sort(cliqueAsMappings, new MappingComparator(cliqueAsMappings));
			Set<ITree> srcIgnored = new HashSet<>();
			Set<ITree> dstIgnored = new HashSet<>();
			while (cliqueAsMappings.size() > 0) {
				Mapping mapping = cliqueAsMappings.remove(0);
				if (!(srcIgnored.contains(mapping.getFirst()) || dstIgnored.contains(mapping.getSecond()))) {
					addFullMapping(mapping.getFirst(), mapping.getSecond());
					srcIgnored.add(mapping.getFirst());
					dstIgnored.add(mapping.getSecond());
				}
			}
		}
	}
	
	private List<Mapping> fromClique(Pair<List<ITree>, List<ITree>> clique) {
		List<Mapping> cliqueAsMappings = new ArrayList<Mapping>();
		for (ITree src: clique.getFirst())
			for (ITree dst: clique.getFirst())
				cliqueAsMappings.add(new Mapping(src, dst));
		return cliqueAsMappings;
	}
	
	private static class CliqueComparator implements Comparator<Pair<List<ITree>, List<ITree>>> {

		@Override
		public int compare(Pair<List<ITree>, List<ITree>> l1,
				Pair<List<ITree>, List<ITree>> l2) {
			int minDepth1 = minDepth(l1);
			int minDepth2 = minDepth(l2);
			if (minDepth1 != minDepth2)
				return -1 * Integer.compare(minDepth1, minDepth2);
			else {
				int size1 = size(l1);
				int size2 = size(l2);
				return -1 * Integer.compare(size1, size2);
			}
 		}
		
		private int minDepth(Pair<List<ITree>, List<ITree>> trees) {
			int depth = Integer.MAX_VALUE;
			for (ITree t : trees.getFirst())
				if (depth > t.getDepth())
					depth = t.getDepth();
			for (ITree t : trees.getSecond())
				if (depth > t.getDepth())
					depth = t.getDepth();
			return depth;
		}
		
		private int size(Pair<List<ITree>, List<ITree>> trees) {
			return trees.getFirst().size() + trees.getSecond().size();
		}
		
	}
	
private class MappingComparator implements Comparator<Mapping> {
		
		private Map<Mapping, double[]> simMap = new HashMap<>();
		
		public MappingComparator(List<Mapping> mappings) {
			for (Mapping mapping: mappings)
				simMap.put(mapping, sims(mapping.getFirst(), mapping.getSecond()));
		}
		
		public int compare(Mapping m1, Mapping m2) {
			double[] sims1 = simMap.get(m1);
			double[] sims2 = simMap.get(m2);
			for (int i = 0; i < sims1.length; i++) {
				if (sims1[i] != sims2[i])
					return -1 * Double.compare(sims2[i], sims2[i]);
			}
			return 0;
		}
		 
		private Map<ITree, List<ITree>> srcDescendants = new HashMap<>();
		
		private Map<ITree, Set<ITree>> dstDescendants = new HashMap<>();

	
		protected int numberOfCommonDescendants(ITree src, ITree dst) {
			if (!srcDescendants.containsKey(src)) srcDescendants.put(src, src.getDescendants());
			if (!dstDescendants.containsKey(dst)) dstDescendants.put(dst, new HashSet<>(dst.getDescendants()));
		
			int common = 0;
			
			for (ITree t: srcDescendants.get(src)) {
				ITree m = mappings.getDst(t);
				if (m != null && dstDescendants.get(dst).contains(m)) common++;
			}

			return common;
		}
		
		protected double[] sims(ITree src, ITree dst) {
			double[] sims = new double[4];
			sims[0] = jaccardSimilarity(src.getParent(), dst.getParent());
			sims[1] = src.positionInParent() - dst.positionInParent();
			sims[2] = src.getId() - dst.getId();
			sims[3] = src.getId();
			return sims;
		}
		
		protected double jaccardSimilarity(ITree src, ITree dst) {
			double num = (double) numberOfCommonDescendants(src, dst);
			double den = (double) srcDescendants.get(src).size() + (double) dstDescendants.get(dst).size() - num;
			return num/den;
		}

	}
}
