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
 * Copyright 2019 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.test;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.utils.Registry;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.gen.TreeGenerators;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.DefaultTree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TypeSet;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestRegistries {
    @Test
    public void testTreeGenerators() {
        TreeGenerators generators = TreeGenerators.getInstance();
        assertFalse(generators.has("foo"));
        assertFalse(generators.has("bar"));
        generators.install(FooTreeGenerator.class,
                FooTreeGenerator.class.getAnnotation(com.github.gumtreediff.gen.Register.class));
        assertTrue(generators.has("foo"));
        assertFalse(generators.has("bar"));
        assertTrue(generators.hasGeneratorForFile("foo.foo"));
        assertEquals(FooTreeGenerator.class, generators.get("foo.foo").getClass());
        generators.install(BarTreeGenerator.class,
                BarTreeGenerator.class.getAnnotation(com.github.gumtreediff.gen.Register.class));
        assertTrue(generators.has("foo"));
        assertTrue(generators.has("bar"));
        assertTrue(generators.hasGeneratorForFile("foo.foo"));
        assertEquals(BarTreeGenerator.class, generators.get("foo.foo").getClass());
    }

    @Test
    public void testMatchers() {
        Matchers matchers = Matchers.getInstance();
        assertNull(matchers.getMatcher("foo"));
        assertNull(matchers.getMatcher("bar"));
        matchers.install(FooMatcher.class,
                FooMatcher.class.getAnnotation(com.github.gumtreediff.matchers.Register.class));
        assertNotNull(matchers.getMatcher("foo"));
        assertNull(matchers.getMatcher("bar"));
        assertEquals(FooMatcher.class, matchers.getMatcherWithFallback("baz").getClass());
        matchers.install(BarMatcher.class,
                BarMatcher.class.getAnnotation(com.github.gumtreediff.matchers.Register.class));
        assertNotNull(matchers.getMatcher("foo"));
        assertNotNull(matchers.getMatcher("bar"));
        assertEquals(BarMatcher.class, matchers.getMatcherWithFallback("baz").getClass());
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

    @com.github.gumtreediff.matchers.Register(id = "foo")
    public static class FooMatcher implements Matcher {
        @Override
        public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
            return null;
        }
    }

    @com.github.gumtreediff.matchers.Register(id = "bar", priority = Registry.Priority.MAXIMUM)
    public static class BarMatcher implements Matcher {
        @Override
        public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
            return null;
        }
    }
}
