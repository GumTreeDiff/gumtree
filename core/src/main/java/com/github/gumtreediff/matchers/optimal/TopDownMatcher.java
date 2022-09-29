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

package com.github.gumtreediff.matchers.optimal;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.Tree;

public class TopDownMatcher implements Matcher {
    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        recursiveMatch(src, dst, mappings);
        return mappings;
    }

    private void recursiveMatch(Tree src, Tree dst, MappingStore mappings) {
        if (!src.hasSameType(dst))
            throw new RuntimeException(String.format(
                    "Top down matching aborted due to a type difference between %s and %s", src, dst));
        if (src.getChildren().size() != dst.getChildren().size())
            throw new RuntimeException(String.format(
                    "Top down matching aborted due to a children difference between %s and %s",
                    src.toTreeString(), dst.toTreeString()));

        mappings.addMapping(src, dst);
        for (int i = 0; i < src.getChildren().size(); i++)
            recursiveMatch(src.getChild(i), dst.getChild(i), mappings);
    }
}
