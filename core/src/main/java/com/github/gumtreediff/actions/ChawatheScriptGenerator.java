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
 * Copyright 2019 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.actions;

import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.FakeTree;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeUtils;

import java.util.*;

public class ChawatheScriptGenerator implements EditScriptGenerator {
    private ITree origSrc;

    private ITree cpySrc;

    private ITree origDst;

    private MappingStore origMappings;

    private MappingStore cpyMappings;

    private Set<ITree> dstInOrder;

    private Set<ITree> srcInOrder;

    private EditScript actions;

    private Map<ITree, ITree> origToCopy;

    private Map<ITree, ITree> copyToOrig;

    @Override
    public EditScript computeActions(MappingStore ms) {
        initWith(ms);
        generate();
        return actions;
    }

    public void initWith(MappingStore ms) {
        this.origSrc = ms.src;
        this.cpySrc = this.origSrc.deepCopy();
        this.origDst = ms.dst;
        this.origMappings = ms;

        origToCopy = new HashMap<>();
        copyToOrig = new HashMap<>();
        Iterator<ITree> cpyTreeIterator = TreeUtils.preOrderIterator(cpySrc);
        for (ITree origTree: TreeUtils.preOrder(origSrc)) {
            ITree cpyTree = cpyTreeIterator.next();
            origToCopy.put(origTree, cpyTree);
            copyToOrig.put(cpyTree, origTree);
        }

        cpyMappings = new MappingStore(ms.src, ms.dst);
        for (Mapping m: origMappings)
            cpyMappings.addMapping(origToCopy.get(m.first), m.second);
    }

    public EditScript generate() {
        ITree srcFakeRoot = new FakeTree(cpySrc);
        ITree dstFakeRoot = new FakeTree(origDst);
        cpySrc.setParent(srcFakeRoot);
        origDst.setParent(dstFakeRoot);

        actions = new EditScript();
        dstInOrder = new HashSet<>();
        srcInOrder = new HashSet<>();

        cpyMappings.addMapping(srcFakeRoot, dstFakeRoot);

        List<ITree> bfsDst = TreeUtils.breadthFirst(origDst);
        for (ITree x: bfsDst) {
            ITree w = null;
            ITree y = x.getParent();
            ITree z = cpyMappings.getSrcForDst(y);

            if (!cpyMappings.isDstMapped(x)) {
                int k = findPos(x);
                // Insertion case : insert new node.
                w = new FakeTree();
                // In order to use the real nodes from the second tree, we
                // furnish x instead of w
                Action ins = new Insert(x, copyToOrig.get(z), k);
                actions.add(ins);
                copyToOrig.put(w, x);
                cpyMappings.addMapping(w, x);
                z.insertChild(w, k);
            } else {
                w = cpyMappings.getSrcForDst(x);
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
                        int oldk = w.positionInParent();
                        w.getParent().getChildren().remove(oldk);
                        z.insertChild(w, k);
                    }
                }
            }

            srcInOrder.add(w);
            dstInOrder.add(x);
            alignChildren(w, x);
        }

        for (ITree w : cpySrc.postOrder())
            if (!cpyMappings.isSrcMapped(w))
                actions.add(new Delete(copyToOrig.get(w)));

        return actions;
    }

    private void alignChildren(ITree w, ITree x) {
        srcInOrder.removeAll(w.getChildren());
        dstInOrder.removeAll(x.getChildren());

        List<ITree> s1 = new ArrayList<>();
        for (ITree c: w.getChildren())
            if (cpyMappings.isSrcMapped(c))
                if (x.getChildren().contains(cpyMappings.getDstForSrc(c)))
                    s1.add(c);

        List<ITree> s2 = new ArrayList<>();
        for (ITree c: x.getChildren())
            if (cpyMappings.isDstMapped(c))
                if (w.getChildren().contains(cpyMappings.getSrcForDst(c)))
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

        ITree u = cpyMappings.getSrcForDst(v);
        // siblings = u.getParent().getChildren();
        // int upos = siblings.indexOf(u);
        int upos = u.positionInParent();
        // int r = 0;
        // for (int i = 0; i <= upos; i++)
        // if (srcInOrder.contains(siblings.get(i))) r++;
        return upos + 1;
    }

    private List<Mapping> lcs(List<ITree> x, List<ITree> y) {
        int m = x.size();
        int n = y.size();
        List<Mapping> lcs = new ArrayList<>();

        int[][] opt = new int[m + 1][n + 1];
        for (int i = m - 1; i >= 0; i--) {
            for (int j = n - 1; j >= 0; j--) {
                if (cpyMappings.getSrcForDst(y.get(j)).equals(x.get(i))) opt[i][j] = opt[i + 1][j + 1] + 1;
                else  opt[i][j] = Math.max(opt[i + 1][j], opt[i][j + 1]);
            }
        }

        int i = 0, j = 0;
        while (i < m && j < n) {
            if (cpyMappings.getSrcForDst(y.get(j)).equals(x.get(i))) {
                lcs.add(new Mapping(x.get(i), y.get(j)));
                i++;
                j++;
            } else if (opt[i + 1][j] >= opt[i][j + 1]) i++;
            else j++;
        }

        return lcs;
    }
}
