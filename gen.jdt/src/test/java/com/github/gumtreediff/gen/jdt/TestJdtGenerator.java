package com.github.gumtreediff.gen.jdt;

import java.io.IOException;

import org.junit.Test;

import com.github.gumtreediff.tree.ITree;
import static org.junit.Assert.*;

public class TestJdtGenerator {

    @Test
    public void testSimpleSyntax() throws IOException {
        String input = "public class Foo { public int foo; }";
        ITree tree = new JdtTreeGenerator().generateFromString(input).getRoot();
        assertEquals(15, tree.getType());
        assertEquals(9, tree.getSize());
    }

    @Test
    public void testJava5Syntax() throws IOException {
        String input = "public class Foo<A> { public List<A> foo; public void foo() "
                + "{ for (A f : foo) { System.out.println(f); } } }";
        ITree tree = new JdtTreeGenerator().generateFromString(input).getRoot();
        assertEquals(15, tree.getType());
        assertEquals(32, tree.getSize());
    }

    @Test
    public void testJava8Syntax() throws IOException {
        String input = "public class Foo { public void foo(){ new ArrayList<Object>().stream().forEach(a -> {}); } }";
        ITree tree = new JdtTreeGenerator().generateFromString(input).getRoot();
        assertEquals(15, tree.getType());
        assertEquals(24, tree.getSize());
    }

}
