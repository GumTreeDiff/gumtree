package com.github.gumtreediff.tree.merge;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Pair;
import com.github.gumtreediff.tree.TreeContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PcsMerge {

    TreeContext baseTree;
    TreeContext leftTree;
    TreeContext rightTree;

    Matcher leftMatch;
    Matcher rightMatch;

    public PcsMerge(TreeContext base, TreeContext left, TreeContext right, Matcher matchLeft, Matcher matchRight) {
        this.baseTree = base;
        this.leftTree = left;
        this.rightTree = right;
        this.leftMatch = matchLeft;
        this.rightMatch = matchRight;
    }

    public Set<Pair<Pcs, Pcs>> computeMerge() {
        final TreeContext fakeContext = new TreeContext().merge(baseTree).merge(leftTree).merge(rightTree);

        Set<Pcs> t0 = Pcs.fromTree(baseTree.getRoot());
        Set<Pcs> t1 = Pcs.fromTree(leftTree.getRoot());
        Set<Pcs> t2 = Pcs.fromTree(rightTree.getRoot());

        Map<ITree, ITree> references = buildReferenceTree();

        Set<Pcs> delta = new HashSet<>();
        Set<Pcs> t0_star = star(t0, references);
        Set<Pcs> t1_star = star(t1, references);
        Set<Pcs> t2_star = star(t2, references);
        delta.addAll(t0_star);
        delta.addAll(t1_star);
        delta.addAll(t2_star);

        System.out.println("T0: " + Pcs.inspect(t0_star, fakeContext));
        System.out.println("T1: " + Pcs.inspect(t1_star, fakeContext));
        System.out.println("T2: " + Pcs.inspect(t2_star, fakeContext));
        System.out.println("Delta: " + Pcs.inspect(delta, fakeContext));
        HashSet<Pcs> deltaT1 = new HashSet<>(t1_star);
        deltaT1.removeAll(t0_star);
        HashSet<Pcs> deltaT2 = new HashSet<>(t2_star);
        deltaT2.removeAll(t0_star);
        System.out.println("DT1: " + Pcs.inspect(deltaT1, fakeContext));
        System.out.println("DT2: " + Pcs.inspect(deltaT2, fakeContext));

        return getInconsistencies(t0, delta);
    }

    Set<Pcs> star(Set<Pcs> pcses, Map<ITree, ITree> references) {
        Set<Pcs> result = new HashSet<>();
        for (Pcs pcs: pcses) {
            result.add(new Pcs(
                    references.get(pcs.getRoot()),
                    references.get(pcs.getPredecessor()),
                    references.get(pcs.getSuccessor())
            ));
        }
        return result;
    }

    Set<Pair<Pcs, Pcs>> getInconsistencies(Set<Pcs> base, Set<Pcs> all) {
        Set<Pair<Pcs, Pcs>> inconsistent = new HashSet<>();
        Set<Pcs> ignored = new HashSet<>();
        for (Pcs pcs: all) {
            if (!ignored.contains(pcs)) {
                Pcs other = pcs.getOther(all);
                if (other == null)
                    continue;
                if (base.contains(other))
                    ignored.add(other);
                else if (base.contains(pcs))
                    ignored.add(pcs);
                else
                    inconsistent.add(new Pair<>(pcs, other));
            }
        }
        return inconsistent;
    }

    Map<ITree, ITree> buildReferenceTree() {
        Map<ITree, ITree> result = new HashMap<>();
        for (ITree t: baseTree.getRoot().preOrder())
            result.put(t, t);
        addReferenceTrees(leftTree.getRoot(), leftMatch.getMappings(), result);
        addReferenceTrees(rightTree.getRoot(), rightMatch.getMappings(), result);
        return result;
    }

    private void addReferenceTrees(ITree tree, MappingStore mappings, Map<ITree, ITree> result) {
        for (ITree t: tree.preOrder())
            if (mappings.hasDst(t)) {
                result.put(t, mappings.getSrc(t));
            } else {
                result.put(t, t);
            }
    }
}
