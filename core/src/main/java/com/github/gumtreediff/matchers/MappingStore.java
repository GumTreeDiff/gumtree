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

import com.github.gumtreediff.tree.ITree;

public class MappingStore implements Iterable<Mapping> {
    public final ITree src;
    public final ITree dst;

    private Map<ITree, ITree> srcToDst;
    private Map<ITree, ITree> dstToSrc;

    public MappingStore(MappingStore ms) {
        this(ms.src, ms.dst);
        for (Mapping m : ms)
            addMapping(m.first, m.second);
    }

    public MappingStore(ITree src, ITree dst) {
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
                Iterator<ITree> it = srcToDst.keySet().iterator();
                return new Iterator<Mapping>() {
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public Mapping next() {
                        ITree src = it.next();
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

    public void addMapping(ITree src, ITree dst) {
        srcToDst.put(src, dst);
        dstToSrc.put(dst, src);
    }

    public void addMappingRecursively(ITree src, ITree dst) {
        addMapping(src, dst);
        for (int i = 0; i < src.getChildren().size(); i++)
            addMappingRecursively(src.getChild(i), dst.getChild(i));
    }

    public void removeMapping(ITree src, ITree dst) {
        srcToDst.remove(src);
        dstToSrc.remove(dst);
    }

    public ITree getDstForSrc(ITree src) {
        return srcToDst.get(src);
    }

    public ITree getSrcForDst(ITree dst) {
        return dstToSrc.get(dst);
    }

    public boolean isSrcMapped(ITree src) {
        return srcToDst.containsKey(src);
    }

    public boolean isDstMapped(ITree dst) {
        return dstToSrc.containsKey(dst);
    }

    public boolean areBothUnmapped(ITree src, ITree dst) {
        return !(isSrcMapped(src) || isDstMapped(dst));
    }

    public boolean areSrcsUnmapped(Collection<ITree> srcs) {
        for (ITree src : srcs)
            if (isSrcMapped(src))
                return false;

        return true;
    }

    public boolean areDstsUnmapped(Collection<ITree> dsts) {
        for (ITree dst : dsts)
            if (isDstMapped(dst))
                return false;

        return true;
    }

    public boolean has(ITree src, ITree dst) {
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

    public boolean isMappingAllowed(ITree src, ITree dst) {
        return src.hasSameType(dst) && areBothUnmapped(src, dst);
    }
}
