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
 * along with GumTree. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2022 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */
package com.github.gumtreediff.gen.treesitterng;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.github.gumtreediff.gen.treesitterng.AbstractTreeSitterNgGenerator.matchNodeOrAncestorTypes;
import static org.junit.jupiter.api.Assertions.*;

public class AbstractTreeSitterNgGeneratorTest {
    @Test
    public void testMatchNodeOrAncestorTypes() {
        MockTypeOnlyTreeSitterNode root = new MockTypeOnlyTreeSitterNode();
        root.setType("root");
        MockTypeOnlyTreeSitterNode depth1Node0 = new MockTypeOnlyTreeSitterNode();
        depth1Node0.setType("depth1Node0");
        MockTypeOnlyTreeSitterNode depth1Node1 = new MockTypeOnlyTreeSitterNode();
        depth1Node1.setType("depth1Node1");

        MockTypeOnlyTreeSitterNode depth2Node0 = new MockTypeOnlyTreeSitterNode();
        depth2Node0.setType("depth2Node0");
        MockTypeOnlyTreeSitterNode depth2Node1 = new MockTypeOnlyTreeSitterNode();
        depth2Node1.setType("depth2Node1");

        root.addChild(depth1Node0);
        root.addChild(depth1Node1);
        depth1Node0.addChild(depth2Node0);
        depth1Node1.addChild(depth2Node1);

        List<String> ruleSet = new ArrayList<>();
        ruleSet.add("root depth1Node0");
        ruleSet.add("depth2Node1");
        ruleSet.add("root");
        ruleSet.add("root depth1Node0 depth2Node0");
        assertEquals("root", matchNodeOrAncestorTypes(ruleSet, root));
        assertEquals("root depth1Node0", matchNodeOrAncestorTypes(ruleSet, depth1Node0));
        assertNull(matchNodeOrAncestorTypes(ruleSet, depth1Node1));
        assertEquals("root depth1Node0 depth2Node0", matchNodeOrAncestorTypes(ruleSet, depth2Node0));
        assertEquals("depth2Node1", matchNodeOrAncestorTypes(ruleSet, depth2Node1));
    }
}