package com.github.gumtreediff.matchers.heuristic.cd;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeUtils;
import org.simmetrics.StringMetrics;

import java.util.*;

public class ChangeDistillerLeavesMatcher extends Matcher {

    public static final double LABEL_SIM_THRESHOLD = 0.5D;

    public ChangeDistillerLeavesMatcher(ITree src, ITree dst, MappingStore store) {
        super(src, dst, store);
    }

    @Override
    public void match() {
        List<ITree> dstLeaves = retainLeaves(TreeUtils.postOrder(dst));

        List<Mapping> leafMappings = new LinkedList<>();

        for (Iterator<ITree> srcLeaves = TreeUtils.leafIterator(
                TreeUtils.postOrderIterator(src)); srcLeaves.hasNext();) {
            for (ITree dstLeaf: dstLeaves) {
                ITree srcLeaf = srcLeaves.next();
                if (srcLeaf.isMatchable(dstLeaf)) {
                    double sim = StringMetrics.qGramsDistance().compare(srcLeaf.getLabel(), dstLeaf.getLabel());
                    if (sim > LABEL_SIM_THRESHOLD) leafMappings.add(new Mapping(srcLeaf, dstLeaf));
                }
            }
        }

        Set<ITree> srcIgnored = new HashSet<>();
        Set<ITree> dstIgnored = new HashSet<>();
        Collections.sort(leafMappings, new LeafMappingComparator());
        while (leafMappings.size() > 0) {
            Mapping best = leafMappings.remove(0);
            if (!(srcIgnored.contains(best.getFirst()) || dstIgnored.contains(best.getSecond()))) {
                addMapping(best.getFirst(),best.getSecond());
                srcIgnored.add(best.getFirst());
                dstIgnored.add(best.getSecond());
            }
        }
    }

    public List<ITree> retainLeaves(List<ITree> trees) {
        Iterator<ITree> tIt = trees.iterator();
        while (tIt.hasNext()) {
            ITree t = tIt.next();
            if (!t.isLeaf()) tIt.remove();
        }
        return trees;
    }

    private class LeafMappingComparator implements Comparator<Mapping> {

        @Override
        public int compare(Mapping m1, Mapping m2) {
            return Double.compare(sim(m1), sim(m2));
        }

        public double sim(Mapping m) {

            return StringMetrics.qGramsDistance().compare(m.getFirst().getLabel(), m.getSecond().getLabel());
        }

    }
}
