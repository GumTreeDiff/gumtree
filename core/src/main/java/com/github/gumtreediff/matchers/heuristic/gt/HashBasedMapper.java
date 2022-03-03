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
 * Copyright 2022 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.matchers.heuristic.gt;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class HashBasedMapper {
    private final Long2ObjectMap<Pair<Set<Tree>, Set<Tree>>> mappings;

    public HashBasedMapper() {
        mappings = new Long2ObjectOpenHashMap<>();
    }

    public void addSrcs(Collection<Tree> srcs) {
        for (Tree t : srcs)
            addSrc(t);
    }

    public void addDsts(Collection<Tree> dsts) {
        for (Tree t : dsts)
            addDst(t);
    }

    public void addSrc(Tree src) {
        mappings.putIfAbsent(src.getMetrics().hash, new Pair<>(new HashSet<>(), new HashSet<>()));
        mappings.get(src.getMetrics().hash).first.add(src);
    }

    public void addDst(Tree dst) {
        mappings.putIfAbsent(dst.getMetrics().hash, new Pair<>(new HashSet<>(), new HashSet<>()));
        mappings.get(dst.getMetrics().hash).second.add(dst);
    }

    public Stream<Pair<Set<Tree>, Set<Tree>>> unique() {
        return mappings.values().stream()
                .filter((value) -> value.first.size() == 1 && value.second.size() == 1);
    }

    public Stream<Pair<Set<Tree>, Set<Tree>>> ambiguous() {
        return mappings.values().stream()
                .filter((value) -> (value.first.size() > 1 && value.second.size() >= 1)
                        || (value.first.size() >= 1 && value.second.size() > 1));
    }

    public Stream<Pair<Set<Tree>, Set<Tree>>> unmapped() {
        return mappings.values().stream()
                .filter((value) -> value.first.size() == 0 || value.second.size() == 0);
    }

    public boolean isSrcMapped(Tree src) {
        return mappings.get(src.getMetrics().hash).second.size() > 0;
    }

    public boolean isDstMapped(Tree dst) {
        return mappings.get(dst.getMetrics().hash).first.size() > 0;
    }
}
