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
 * Copyright 2016 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.matchers.heuristic.gt;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.ITree;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractMappingComparator implements Comparator<Mapping> {

    protected List<Mapping> ambiguousMappings;

    protected Map<Mapping, Double> similarities = new HashMap<>();

    protected int maxTreeSize;

    protected MappingStore mappings;

    public AbstractMappingComparator(List<Mapping> ambiguousMappings, MappingStore mappings, int maxTreeSize) {
        this.maxTreeSize = maxTreeSize;
        this.mappings = mappings;
        this.ambiguousMappings = ambiguousMappings;
    }

    public int compare(Mapping m1, Mapping m2) {
        if (similarities.get(m2).compareTo(similarities.get(m1)) != 0) {
            return Double.compare(similarities.get(m2), similarities.get(m1));
        }
        if (m1.first.getId() != m2.first.getId()) {
            return Integer.compare(m1.first.getId(), m2.first.getId());
        }
        return Integer.compare(m1.second.getId(), m2.second.getId());
    }

    protected abstract double similarity(ITree src, ITree dst);

    protected double posInParentSimilarity(ITree src, ITree dst) {
        int posSrc = (src.isRoot()) ? 0 : src.getParent().getChildPosition(src);
        int posDst = (dst.isRoot()) ? 0 : dst.getParent().getChildPosition(dst);
        int maxSrcPos =  (src.isRoot()) ? 1 : src.getParent().getChildren().size();
        int maxDstPos =  (dst.isRoot()) ? 1 : dst.getParent().getChildren().size();
        int maxPosDiff = Math.max(maxSrcPos, maxDstPos);
        return 1D - ((double) Math.abs(posSrc - posDst) / (double) maxPosDiff);
    }

    protected double numberingSimilarity(ITree src, ITree dst) {
        return 1D - ((double) Math.abs(src.getId() - dst.getId())
                / (double) maxTreeSize);
    }

}
