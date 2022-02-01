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

package com.github.gumtreediff.test;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.Registry;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.gen.TreeGenerators;
import com.github.gumtreediff.tree.DefaultTree;
import com.github.gumtreediff.tree.TreeContext;
import static org.junit.jupiter.api.Assertions.*;

import com.github.gumtreediff.tree.TypeSet;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;

public class TestTreeGenerators {
    @Test
    public void testTreeGenerators() {
        TreeGenerators generators = TreeGenerators.getInstance();
        assertFalse(generators.has("foo"));
        assertFalse(generators.has("bar"));
        generators.install(FooTreeGenerator.class, FooTreeGenerator.class.getAnnotation(Register.class));
        assertTrue(generators.has("foo"));
        assertFalse(generators.has("bar"));
        assertTrue(generators.hasGeneratorForFile("foo.foo"));
        assertEquals(FooTreeGenerator.class, generators.get("foo.foo").getClass());
        generators.install(BarTreeGenerator.class, BarTreeGenerator.class.getAnnotation(Register.class));
        assertTrue(generators.has("foo"));
        assertTrue(generators.has("bar"));
        assertTrue(generators.hasGeneratorForFile("foo.foo"));
        assertEquals(BarTreeGenerator.class, generators.get("foo.foo").getClass());
    }

    @Register(id = "foo", accept = "\\.foo$", priority = Registry.Priority.HIGH)
    public static class FooTreeGenerator extends TreeGenerator {
        @Override
        protected TreeContext generate(Reader r) throws IOException {
            TreeContext ctx = new TreeContext();
            ctx.setRoot(new DefaultTree(TypeSet.type("foo")));
            return ctx;
        }
    }

    @Register(id = "bar", accept = "\\.foo$", priority = Registry.Priority.MAXIMUM)
    public static class BarTreeGenerator extends TreeGenerator {
        @Override
        protected TreeContext generate(Reader r) throws IOException {
            TreeContext ctx = new TreeContext();
            ctx.setRoot(new DefaultTree(TypeSet.type("bar")));
            return ctx;
        }
    }
}
