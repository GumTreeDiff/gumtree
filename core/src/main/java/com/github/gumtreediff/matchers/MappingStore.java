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

package com.github.gumtreediff.matchers;

import java.util.*;

import com.github.gumtreediff.tree.Tree;

/**
 * Stores the mappings between the nodes of a src and dst trees.
 */
public class MappingStore implements Iterable<Mapping> {
    public final Tree src;
    public final Tree dst;

    private Map<Tree, Tree> srcToDst;
    private Map<Tree, Tree> dstToSrc;

    public MappingStore(MappingStore ms) {
        this(ms.src, ms.dst);
        for (Mapping m : ms)
            addMapping(m.first, m.second);
    }

    public MappingStore(Tree src, Tree dst) {
        this.src = src;
        this.dst = dst;
        srcToDst = new HashMap<>();
        dstToSrc = new HashMap<>();
    }

    public int size() {
        return srcToDst.size();
    }

    public Set<Mapping> asSet() {
        return new AbstractSet<Mapping>() {

            @Override
            public Iterator<Mapping> iterator() {
                Iterator<Tree> it = srcToDst.keySet().iterator();
                return new Iterator<Mapping>() {
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public Mapping next() {
                        Tree src = it.next();
                        if (src == null) return null;
                        return new Mapping(src, srcToDst.get(src));
                    }
                };
            }

            @Override
            public int size() {
                return srcToDst.keySet().size();
            }
        };
    }

    public void addMapping(Tree src, Tree dst) {
        srcToDst.put(src, dst);
        dstToSrc.put(dst, src);
    }

    public void addMappingRecursively(Tree src, Tree dst) {
        addMapping(src, dst);
        for (int i = 0; i < src.getChildren().size(); i++)
            addMappingRecursively(src.getChild(i), dst.getChild(i));
    }

    public void removeMapping(Tree src, Tree dst) {
        srcToDst.remove(src);
        dstToSrc.remove(dst);
    }

    public Tree getDstForSrc(Tree src) {
        return srcToDst.get(src);
    }

    public Tree getSrcForDst(Tree dst) {
        return dstToSrc.get(dst);
    }

    public boolean isSrcMapped(Tree src) {
        return srcToDst.containsKey(src);
    }

    public boolean isDstMapped(Tree dst) {
        return dstToSrc.containsKey(dst);
    }

    public boolean areBothUnmapped(Tree src, Tree dst) {
        return !(isSrcMapped(src) || isDstMapped(dst));
    }

    public boolean areSrcsUnmapped(Collection<Tree> srcs) {
        for (Tree src : srcs)
            if (isSrcMapped(src))
                return false;

        return true;
    }

    public boolean areDstsUnmapped(Collection<Tree> dsts) {
        for (Tree dst : dsts)
            if (isDstMapped(dst))
                return false;

        return true;
    }

    public boolean hasUnmappedSrcChildren(Tree t) {
        for (Tree c : t.getDescendants())
            if (!isSrcMapped(c))
                return true;

        return false;
    }

    public boolean hasUnmappedDstChildren(Tree t) {
        for (Tree c : t.getDescendants())
            if (!isDstMapped(c))
                return true;

        return false;
    }

    public boolean has(Tree src, Tree dst) {
        return srcToDst.get(src) == dst;
    }

    @Override
    public Iterator<Mapping> iterator() {
        return asSet().iterator();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        for (Mapping m : this)
            b.append(m.toString()).append('\n');
        return b.toString();
    }

    public boolean isMappingAllowed(Tree src, Tree dst) {
        return src.hasSameType(dst) && areBothUnmapped(src, dst);
    }
}
