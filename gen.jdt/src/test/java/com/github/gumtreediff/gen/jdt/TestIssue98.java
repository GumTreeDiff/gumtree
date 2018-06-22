package com.github.gumtreediff.gen.jdt;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestIssue98 {

    @Test
    public void shouldHaveDifferentTree() throws IOException {
        String left = "public interface Main { }";
        String right = "public class Main { }";

        ITree leftTree = new JdtTreeGenerator().generateFromString(left).getRoot();
        ITree rightTree = new JdtTreeGenerator().generateFromString(right).getRoot();
        Matcher m = Matchers.getInstance().getMatcher(leftTree, rightTree);
        m.match();

        ActionGenerator g = new ActionGenerator(leftTree, rightTree, m.getMappings());
        List<Action> actions = g.generate();

        assertEquals(1, actions.size());
    }

}
