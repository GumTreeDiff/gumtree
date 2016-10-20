package com.github.gumtreediff.tree.merge;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Tree;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Pcs {
    private ITree root;

    private ITree predecessor;

    private ITree successor;

    public ITree getRoot() {
        return root;
    }

    public ITree getPredecessor() {
        return predecessor;
    }

    public ITree getSuccessor() {
        return successor;
    }

    public Pcs(ITree root, ITree predecessor, ITree successor) {
        this.root = root;
        this.predecessor = predecessor;
        this.successor = successor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pcs pcs = (Pcs) o;

        if (root != null ? !root.equals(pcs.root) : pcs.root != null) return false;
        if (predecessor != null ? !predecessor.equals(pcs.predecessor) : pcs.predecessor != null) return false;
        return successor != null ? successor.equals(pcs.successor) : pcs.successor == null;
    }

    @Override
    public String toString() {
        return "(" + root + "," + predecessor + "," + successor + ")";
    }

    @Override
    public int hashCode() {
        int result = root != null ? root.hashCode() : 0;
        result = 31 * result + (predecessor != null ? predecessor.hashCode() : 0);
        result = 31 * result + (successor != null ? successor.hashCode() : 0);
        return result;
    }

    public static Set<Pcs> star(Set<Pcs> pcses, Map<Tree, Tree> references) {
        Set<Pcs> result = new HashSet<>();
        for(Pcs pcs : pcses) {
            result.add(new Pcs(
                    references.get(pcs.getRoot()),
                    references.get(pcs.getPredecessor()),
                    references.get(pcs.getSuccessor())
            ));
        }
        return result;
    }

    public static Set<Pcs> fromTree(ITree tree) {
        Set<Pcs> result = new HashSet<>();
        for(ITree t: tree.preOrder()) {
            int size = t.getChildren().size();
            for (int i = 0; i < size; i++) {
                ITree c = t.getChild(i);
                if (i == 0)
                    result.add(new Pcs(t, null, c));
                result.add(new Pcs(t, c, i == (size - 1) ? null : t.getChild(i + 1)));
            }
            if (size == 0)
                result.add(new Pcs(t, null, null));
        }
        result.add(new Pcs(null, tree, null));
        result.add(new Pcs(null, null, tree));
        return result;
    }

    public static Map<ITree, ITree> referenceTrees(ITree base, ITree left, ITree right, MappingStore leftMappings, MappingStore rightMappings) {
        Map<ITree, ITree> result = new HashMap<>();
        for(ITree t: base.preOrder())
            result.put(t, t);
        referenceTrees(left, leftMappings, result);
        referenceTrees(right, rightMappings, result);
        return result;
    }

    private static void referenceTrees(ITree tree, MappingStore mappings, Map<ITree, ITree> result) {
        for(ITree t: tree.preOrder())
            if (mappings.hasDst(t))
                result.put(t, mappings.getSrc(t));
            else
                result.put(t, t);
    }
}
