/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.actions;

import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.AbstractTree;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeUtils;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import javax.swing.plaf.TreeUI;
import java.util.*;

public class ActionGenerator {

    private ITree origSrc;

    private ITree newSrc;

    private ITree origDst;

    private MappingStore origMappings;

    private MappingStore newMappings;

    private Set<ITree> dstInOrder;

    private Set<ITree> srcInOrder;

    private int lastId;

    private List<Action> actions;

    private Map<ITree, ITree> origToCopy;

    private Map<ITree, ITree> copyToOrig;

    public static boolean REMOVE_MOVES_AND_UPDATES = Boolean.valueOf(System.getProperty("gt.ag.nomove", "false"));

    public ActionGenerator(ITree src, ITree dst, MappingStore mappings) {
        this.origSrc = src;
        this.newSrc = this.origSrc.deepCopy();
        this.origDst = dst;

        origToCopy = new HashMap<>();
        copyToOrig = new HashMap<>();
        Iterator<ITree> cpyTreeIterator = TreeUtils.preOrderIterator(newSrc);
        for (ITree origTree: TreeUtils.preOrder(origSrc)) {
            ITree cpyTree = cpyTreeIterator.next();
            origToCopy.put(origTree, cpyTree);
            copyToOrig.put(cpyTree, origTree);
        }

        origMappings = new MappingStore();
        for (Mapping m: mappings)
            this.origMappings.addMapping(origToCopy.get(m.first), m.second);
        this.newMappings = origMappings.copy();
    }

    public List<Action> getActions() {
        return actions;
    }

    public List<Action> generate() {
        ITree srcFakeRoot = new AbstractTree.FakeTree(newSrc);
        ITree dstFakeRoot = new AbstractTree.FakeTree(origDst);
        newSrc.setParent(srcFakeRoot);
        origDst.setParent(dstFakeRoot);

        actions = new ArrayList<>();
        dstInOrder = new HashSet<>();
        srcInOrder = new HashSet<>();

        // lastId = newSrc.getSize() + 1;
        newMappings.addMapping(srcFakeRoot, dstFakeRoot);

        List<ITree> bfsDst = TreeUtils.breadthFirst(origDst);
        for (ITree x: bfsDst) {
            ITree w = null;
            ITree y = x.getParent();
            ITree z = newMappings.getSrcForDst(y);

            if (!newMappings.isDstMapped(x)) {
                int k = findPos(x);
                // Insertion case : insert new node.
                w = new AbstractTree.FakeTree();
                // In order to use the real nodes from the second tree, we
                // furnish x instead of w and fake that x has the newly
                // generated ID.
                Action ins = new Insert(x, copyToOrig.get(z), k);
                actions.add(ins);
                //System.out.println(ins);
                copyToOrig.put(w, x);
                newMappings.addMapping(w, x);
                z.getChildren().add(k, w);
                w.setParent(z);
            } else {
                w = newMappings.getSrcForDst(x);
                if (!x.equals(origDst)) { // TODO => x != origDst // Case of the root
                    ITree v = w.getParent();
                    if (!w.getLabel().equals(x.getLabel())) {
                        actions.add(new Update(copyToOrig.get(w), x.getLabel()));
                        w.setLabel(x.getLabel());
                    }
                    if (!z.equals(v)) {
                        int k = findPos(x);
                        Action mv = new Move(copyToOrig.get(w), copyToOrig.get(z), k);
                        actions.add(mv);
                        //System.out.println(mv);
                        int oldk = w.positionInParent();
                        z.getChildren().add(k, w);
                        w.getParent().getChildren().remove(oldk);
                        w.setParent(z);
                    }
                }
            }

            srcInOrder.add(w);
            dstInOrder.add(x);
            alignChildren(w, x);
        }

        for (ITree w : newSrc.postOrder())
            if (!newMappings.isSrcMapped(w))
                actions.add(new Delete(copyToOrig.get(w)));


        if (REMOVE_MOVES_AND_UPDATES)
            actions = removeMovesAndUpdates();

        simplify();

        return actions;
    }

    private List<Action> removeMovesAndUpdates() {
        List<Action> actionsCpy = new ArrayList<>(actions.size());
        for (Action a: actions) {
            if (a instanceof Update) {
                Update u = (Update) a;
                ITree src = origToCopy.get(a.getNode());
                ITree dst = origMappings.getDstForSrc(src);
                actionsCpy.add(new Insert(
                        dst,
                        dst.getParent(),
                        dst.positionInParent()));
                actionsCpy.add(new Delete(copyToOrig.get(u.getNode())));
            }
            else if (a instanceof Move) {
                Move m = (Move) a;
                ITree src = origToCopy.get(a.getNode());
                ITree dst = origMappings.getDstForSrc(src);
                actionsCpy.add(new TreeInsert(
                        dst,
                        dst.getParent(),
                        m.getPosition()));
                actionsCpy.add(new TreeDelete(copyToOrig.get(m.getNode())));
            }
            else
                actionsCpy.add(a);
        }

        return actionsCpy;
    }

    private void simplify() {
        Map<ITree, Action> addedTrees = new HashMap<>();
        Map<ITree, Action> deletedTrees = new HashMap<>();

        for (Action a: actions)
            if (a instanceof Insert)
                addedTrees.put(a.getNode(), a);
            else if (a instanceof Delete)
                deletedTrees.put(a.getNode(), a);


        for (ITree t : addedTrees.keySet()) {
            if (addedTrees.keySet().contains(t.getParent()) && addedTrees.keySet().containsAll(t.getDescendants()))
                actions.remove(addedTrees.get(t));
            else {
                if (t.getChildren().size() > 0 && addedTrees.keySet().containsAll(t.getDescendants())) {
                    Insert originalAction = (Insert) addedTrees.get(t);
                    TreeInsert ti = new TreeInsert(originalAction.getNode(),
                            originalAction.getParent(), originalAction.getPosition());
                    int index = actions.lastIndexOf(originalAction);
                    actions.add(index, ti);
                    actions.remove(index +  1);
                }

            }
        }

        for (ITree t : deletedTrees.keySet()) {
            if (deletedTrees.keySet().contains(t.getParent()) && deletedTrees.keySet().containsAll(t.getDescendants()))
                actions.remove(deletedTrees.get(t));
            else {
                if (t.getChildren().size() > 0 && deletedTrees.keySet().containsAll(t.getDescendants())) {
                    Delete originalAction = (Delete) deletedTrees.get(t);
                    TreeDelete ti = new TreeDelete(originalAction.getNode());
                    int index = actions.lastIndexOf(originalAction);
                    actions.add(index, ti);
                    actions.remove(index +  1);
                }

            }
        }
    }

    private void alignChildren(ITree w, ITree x) {
        srcInOrder.removeAll(w.getChildren());
        dstInOrder.removeAll(x.getChildren());

        List<ITree> s1 = new ArrayList<>();
        for (ITree c: w.getChildren())
            if (newMappings.isSrcMapped(c))
                if (x.getChildren().contains(newMappings.getDstForSrc(c)))
                    s1.add(c);

        List<ITree> s2 = new ArrayList<>();
        for (ITree c: x.getChildren())
            if (newMappings.isDstMapped(c))
                if (w.getChildren().contains(newMappings.getSrcForDst(c)))
                    s2.add(c);

        List<Mapping> lcs = lcs(s1, s2);

        for (Mapping m : lcs) {
            srcInOrder.add(m.first);
            dstInOrder.add(m.second);
        }

        for (ITree a : s1) {
            for (ITree b: s2 ) {
                if (origMappings.has(a, b)) {
                    if (!lcs.contains(new Mapping(a, b))) {
                        int k = findPos(b);
                        Action mv = new Move(copyToOrig.get(a), copyToOrig.get(w), k);
                        actions.add(mv);
                        //System.out.println(mv);
                        int oldk = a.positionInParent();
                        w.getChildren().add(k, a);
                        if (k  < oldk ) // FIXME this is an ugly way to patch the index
                            oldk ++;
                        a.getParent().getChildren().remove(oldk);
                        a.setParent(w);
                        srcInOrder.add(a);
                        dstInOrder.add(b);
                    }
                }
            }
        }
    }

    private int findPos(ITree x) {
        ITree y = x.getParent();
        List<ITree> siblings = y.getChildren();

        for (ITree c : siblings) {
            if (dstInOrder.contains(c)) {
                if (c.equals(x)) return 0;
                else break;
            }
        }

        int xpos = x.positionInParent();
        ITree v = null;
        for (int i = 0; i < xpos; i++) {
            ITree c = siblings.get(i);
            if (dstInOrder.contains(c)) v = c;
        }

        //if (v == null) throw new RuntimeException("No rightmost sibling in order");
        if (v == null) return 0;

        ITree u = newMappings.getSrcForDst(v);
        // siblings = u.getParent().getChildren();
        // int upos = siblings.indexOf(u);
        int upos = u.positionInParent();
        // int r = 0;
        // for (int i = 0; i <= upos; i++)
        // if (srcInOrder.contains(siblings.get(i))) r++;
        return upos + 1;
    }

    private int newId() {
        return ++lastId;
    }

    private List<Mapping> lcs(List<ITree> x, List<ITree> y) {
        int m = x.size();
        int n = y.size();
        List<Mapping> lcs = new ArrayList<>();

        int[][] opt = new int[m + 1][n + 1];
        for (int i = m - 1; i >= 0; i--) {
            for (int j = n - 1; j >= 0; j--) {
                if (newMappings.getSrcForDst(y.get(j)).equals(x.get(i))) opt[i][j] = opt[i + 1][j + 1] + 1;
                else  opt[i][j] = Math.max(opt[i + 1][j], opt[i][j + 1]);
            }
        }

        int i = 0, j = 0;
        while (i < m && j < n) {
            if (newMappings.getSrcForDst(y.get(j)).equals(x.get(i))) {
                lcs.add(new Mapping(x.get(i), y.get(j)));
                i++;
                j++;
            } else if (opt[i + 1][j] >= opt[i][j + 1]) i++;
            else j++;
        }

        return lcs;
    }

}
