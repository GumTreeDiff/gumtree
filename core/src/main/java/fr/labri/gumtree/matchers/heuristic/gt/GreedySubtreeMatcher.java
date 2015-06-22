package fr.labri.gumtree.matchers.heuristic.gt;

import fr.labri.gumtree.matchers.Mapping;
import fr.labri.gumtree.matchers.MappingStore;
import fr.labri.gumtree.matchers.MultiMappingStore;
import fr.labri.gumtree.tree.ITree;

import java.util.*;

public class GreedySubtreeMatcher extends SubtreeMatcher {

    public GreedySubtreeMatcher(ITree src, ITree dst, MappingStore store) {
        super(src, dst, store);
    }

    public void filterMappings(MultiMappingStore multiMappings) {
        // Select unique mappings first and extract ambiguous mappings.
        List<Mapping> ambiguousList = new LinkedList<>();
        Set<ITree> ignored = new HashSet<>();
        for (ITree src: multiMappings.getSrcs()) {
            if (multiMappings.isSrcUnique(src))
                addFullMapping(src, multiMappings.getDst(src).iterator().next());
            else if (!ignored.contains(src)) {
                Set<ITree> adsts = multiMappings.getDst(src);
                Set<ITree> asrcs = multiMappings.getSrc(multiMappings.getDst(src).iterator().next());
                for (ITree asrc : asrcs)
                    for (ITree adst: adsts)
                        ambiguousList.add(new Mapping(asrc, adst));
                ignored.addAll(asrcs);
            }
        }

        // Rank the mappings by score.
        Set<ITree> srcIgnored = new HashSet<>();
        Set<ITree> dstIgnored = new HashSet<>();
        Collections.sort(ambiguousList, new MappingComparator(ambiguousList));

        // Select the best ambiguous mappings
        while (ambiguousList.size() > 0) {
            Mapping ambiguous = ambiguousList.remove(0);
            if (!(srcIgnored.contains(ambiguous.getFirst()) || dstIgnored.contains(ambiguous.getSecond()))) {
                addFullMapping(ambiguous.getFirst(), ambiguous.getSecond());
                srcIgnored.add(ambiguous.getFirst());
                dstIgnored.add(ambiguous.getSecond());
            }
        }
    }

    private class MappingComparator implements Comparator<Mapping> {

        private Map<Mapping, Double> simMap = new HashMap<>();

        public MappingComparator(List<Mapping> mappings) {
            for (Mapping mapping: mappings)
                simMap.put(mapping, sim(mapping.getFirst(), mapping.getSecond()));
        }

        public int compare(Mapping m1, Mapping m2) {
            return Double.compare(simMap.get(m2), simMap.get(m1));
        }

        private Map<ITree, List<ITree>> srcDescendants = new HashMap<>();

        private Map<ITree, Set<ITree>> dstDescendants = new HashMap<>();

        protected int numberOfCommonDescendants(ITree src, ITree dst) {
            if (!srcDescendants.containsKey(src))
                srcDescendants.put(src, src.getDescendants());
            if (!dstDescendants.containsKey(dst))
                dstDescendants.put(dst, new HashSet<>(dst.getDescendants()));

            int common = 0;

            for (ITree t: srcDescendants.get(src)) {
                ITree m = mappings.getDst(t);
                if (m != null && dstDescendants.get(dst).contains(m))
                    common++;
            }

            return common;
        }

        protected double sim(ITree src, ITree dst) {
            double jaccard = jaccardSimilarity(src.getParent(), dst.getParent());
            int posSrc = (src.isRoot()) ? 0 : src.getParent().getChildPosition(src);
            int posDst = (dst.isRoot()) ? 0 : dst.getParent().getChildPosition(dst);
            int maxSrcPos =  (src.isRoot()) ? 1 : src.getParent().getChildren().size();
            int maxDstPos =  (dst.isRoot()) ? 1 : dst.getParent().getChildren().size();
            int maxPosDiff = Math.max(maxSrcPos, maxDstPos);
            double pos = 1D - ((double) Math.abs(posSrc - posDst) / (double) maxPosDiff);
            double po = 1D - ((double) Math.abs(src.getId() - dst.getId())
                    / (double) GreedySubtreeMatcher.this.getMaxTreeSize());
            return 100 * jaccard + 10 * pos + po;
        }

        protected double jaccardSimilarity(ITree src, ITree dst) {
            double num = (double) numberOfCommonDescendants(src, dst);
            double den = (double) srcDescendants.get(src).size() + (double) dstDescendants.get(dst).size() - num;
            return num / den;
        }

    }
}
