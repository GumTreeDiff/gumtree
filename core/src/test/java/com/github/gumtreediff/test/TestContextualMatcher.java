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
 * Copyright 2026 GumTree contributors
 */

package com.github.gumtreediff.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.github.gumtreediff.actions.ChawatheScriptGenerator;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.heuristic.gt.ContextualMatcher;
import com.github.gumtreediff.tree.DefaultTree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TypeSet;

public class TestContextualMatcher {
    @Test
    public void mapsChangedChildrenAndKeepsOneToOneMappings() {
        Tree src = tree("before");
        Tree dst = tree("after");

        MappingStore mappings = new ContextualMatcher().match(src, dst);

        assertTrue(mappings.has(src, dst));
        assertEquals(4, mappings.size());
        Set<Tree> destinations = new HashSet<>();
        for (Mapping mapping : mappings)
            assertTrue(destinations.add(mapping.second));
        assertEquals(1, new ChawatheScriptGenerator().computeActions(mappings).size());
    }

    private Tree tree(String label) {
        Tree root = new DefaultTree(TypeSet.type("root"));
        Tree block = new DefaultTree(TypeSet.type("block"));
        Tree first = new DefaultTree(TypeSet.type("statement"));
        first.setLabel(label);
        Tree second = new DefaultTree(TypeSet.type("statement"));
        second.setLabel("unchanged");
        root.addChild(block);
        block.addChild(first);
        block.addChild(second);
        return root;
    }
}
