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
import java.util.stream.Collectors;

import com.github.gumtreediff.tree.Tree;

public class MultiMappingStore implements Iterable<Mapping> {
    private Map<Tree, Set<Tree>> srcToDsts;
    private Map<Tree, Set<Tree>> dstToSrcs;

    public MultiMappingStore() {
        srcToDsts = new  HashMap<>();
        dstToSrcs = new HashMap<>();
    }

    public Set<Mapping> getMappings() {
        Set<Mapping> mappings = new HashSet<>();
        for (Tree src : srcToDsts.keySet())
            for (Tree dst: srcToDsts.get(src))
                mappings.add(new Mapping(src, dst));
        return mappings;
    }

    public void addMapping(Tree src, Tree dst) {
        if (!srcToDsts.containsKey(src))
            srcToDsts.put(src, new HashSet<>());
        srcToDsts.get(src).add(dst);
        if (!dstToSrcs.containsKey(dst))
            dstToSrcs.put(dst, new HashSet<>());
        dstToSrcs.get(dst).add(src);
    }

    public void removeMapping(Tree src, Tree dst) {
        srcToDsts.get(src).remove(dst);
        dstToSrcs.get(dst).remove(src);
    }

    public int size() {
        return getMappings().size();
    }

    public Set<Tree> getDsts(Tree src) {
        return srcToDsts.get(src);
    }

    public Set<Tree> getSrcs(Tree dst) {
        return dstToSrcs.get(dst);
    }

    public Set<Tree> allMappedSrcs() {
        return srcToDsts.keySet();
    }

    public Set<Tree> allMappedDsts() {
        return dstToSrcs.keySet();
    }

    public boolean hasSrc(Tree src) {
        return srcToDsts.containsKey(src);
    }

    public boolean hasDst(Tree dst) {
        return dstToSrcs.containsKey(dst);
    }

    public boolean has(Tree src, Tree dst) {
        return srcToDsts.get(src).contains(dst);
    }

    public boolean isSrcUnique(Tree src) {
        return getDsts(src).size() == 1;
    }

    public boolean isDstUnique(Tree dst) {
        return getSrcs(dst).size() == 1;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        for (Tree t : srcToDsts.keySet()) {
            String l = srcToDsts.get(t).stream().map(Object::toString).collect(Collectors.joining(", "));
            b.append(String.format("%s -> %s", t.toString(), l)).append('\n');
        }
        return b.toString();
    }

    @Override
    public Iterator<Mapping> iterator() {
        return getMappings().iterator();
    }
}
