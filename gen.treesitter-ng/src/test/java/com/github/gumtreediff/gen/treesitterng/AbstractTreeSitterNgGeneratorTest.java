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

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.github.gumtreediff.gen.treesitterng.AbstractTreeSitterNgGenerator.matchNodeOrAncestorTypes;
import static org.junit.jupiter.api.Assertions.*;

public class AbstractTreeSitterNgGeneratorTest {
    private final PythonTreeSitterNgTreeGenerator generator = new PythonTreeSitterNgTreeGenerator();

    @Test
    public void testLfOffsetConsistency() throws IOException {
        // Line 1: "x = 1\n" (5 chars + 1 LF = 6 bytes)
        // Line 2: "y = 2"
        String content = "x = 1\ny = 2";
        TreeContext ctx = generator.generateFrom().string(content);

        // Find the second assignment (y = 2)
        // Root (module) -> children[1] (expression_statement)
        Tree yAssignment = ctx.getRoot().getChild(1);
        assertEquals("expression_statement", yAssignment.getType().name);
        assertEquals(6, yAssignment.getPos(), "Line 2 should start at byte offset 6 for LF content");
    }

    @Test
    public void testCrlfOffsetConsistency() throws IOException {
        // Line 1: "x = 1\r\n" (5 chars + 2 CRLF = 7 bytes)
        // Line 2: "y = 2"
        String content = "x = 1\r\ny = 2";
        TreeContext ctx = generator.generateFrom().string(content);

        Tree yAssignment = ctx.getRoot().getChild(1);
        assertEquals("expression_statement", yAssignment.getType().name);
        assertEquals(7, yAssignment.getPos(), "Line 2 should start at byte offset 7 for CRLF content");
    }

    @Test
    public void testMultiByteOffsetConsistency() throws IOException {
        // Line 1: "# 🐍\n"
        // '#' (1) + ' ' (1) + '🐍' (2 UTF-16 chars, surrogate pair) + '\n' (1) = 5 chars total
        // Line 2: "x = 1"
        // Offsets must be char-based (UTF-16 code units) to match how the rest of GumTree
        // (e.g. AbstractJdtVisitor, VanillaDiffHtmlBuilder) indexes source text, not UTF-8 bytes.
        String content = "# 🐍\nx = 1";
        TreeContext ctx = generator.generateFrom().string(content);

        Tree xAssignment = ctx.getRoot().getChild(1);
        assertEquals("expression_statement", xAssignment.getType().name);
        assertEquals(5, xAssignment.getPos(),
                "Line 2 should start at char offset 5 after a surrogate-pair emoji and LF");
    }

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
