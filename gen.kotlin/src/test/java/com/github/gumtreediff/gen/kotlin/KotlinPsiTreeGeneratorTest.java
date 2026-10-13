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
 * Copyright 2026 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */
package com.github.gumtreediff.gen.kotlin;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.gen.TreeGenerators;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.optimal.TopDownMatcher;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KotlinPsiTreeGeneratorTest {
    private final KotlinPsiTreeGenerator generator = new KotlinPsiTreeGenerator();

    @Test
    public void testFunctionalInterface() throws IOException {
        Tree root = generator.generateFrom().string(
                "fun interface Greeting { fun greet() }").getRoot();

        assertTrue(containsType(root, "KtClass"));
        assertTrue(containsLabel(root, "Greeting"));
        assertFalse(containsType(root, "ERROR"));
    }

    @Test
    public void testFinallyBlock() throws IOException {
        Tree root = generator.generateFrom().string(
                "fun test() { try { work() } finally { cleanup() } }").getRoot();

        assertTrue(containsType(root, "KtFinallySection"));
        assertTrue(containsLabel(root, "cleanup"));
    }

    @Test
    public void testAssignmentOperatorChanges() throws IOException {
        Tree src = generator.generateFrom().string("class Foo { fun foo() { a = b } }").getRoot();
        Tree dst = generator.generateFrom().string("class Foo { fun foo() { a += b } }").getRoot();

        assertTrue(onlyOneUpdate(src, dst));
    }

    @Test
    public void testSourcePositions() throws IOException {
        String source = "fun test() {\n    println(\"Hello\")\n}";
        TreeContext context = generator.generateFrom().string(source);

        assertEquals(0, context.getRoot().getPos());
        assertEquals(source.length(), context.getRoot().getLength());
        for (Tree node : context.getRoot().preOrder()) {
            assertTrue(node.getPos() >= 0);
            assertTrue(node.getEndPos() <= source.length());
        }
    }

    @Test
    public void testPsiGeneratorIsDefaultAndTreeSitterRemainsAvailable() {
        TreeGenerators generators = TreeGenerators.getInstance();

        assertEquals("kotlin-psi", generators.find("Example.kt").id);
        assertNotNull(generators.findById("kotlin-treesitter-ng"));
    }

    private static boolean containsType(Tree root, String type) {
        for (Tree node : root.preOrder()) {
            if (node.getType().name.equals(type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsLabel(Tree root, String label) {
        for (Tree node : root.preOrder()) {
            if (node.getLabel().equals(label)) {
                return true;
            }
        }
        return false;
    }

    private static boolean onlyOneUpdate(Tree src, Tree dst) {
        TopDownMatcher matcher = new TopDownMatcher();
        MappingStore mappings = matcher.match(src, dst);
        EditScript actions = new SimplifiedChawatheScriptGenerator().computeActions(mappings);
        return actions.size() == 1 && actions.get(0) instanceof Update;
    }
}
