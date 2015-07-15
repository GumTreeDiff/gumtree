package com.github.gumtreediff.test;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.io.ActionsIoUtils;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.Pair;
import com.github.gumtreediff.tree.TreeContext;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class TestActionIO {
    private TreeContext src;
    private TreeContext dst;
    private MappingStore mappings;
    private List<Action> actions;

    @Before
    public void setUp() throws Exception {
        Pair<TreeContext, TreeContext> p = TreeLoader.getActionPair();
        src = p.getFirst();
        dst = p.getSecond();
        Matcher m = Matchers.getInstance().getMatcher(src.getRoot(), dst.getRoot());
        mappings = m.getMappings();
        actions = new ActionGenerator(src.getRoot(), dst.getRoot(), mappings).generate();
    }

    @Test
    public void testPos() throws Exception {
        src.getRoot().breadthFirst().forEach(x -> System.out.printf("%d) %s [%d:%d]:%d\n", x.getType(), x.getLabel(), x.getPos(), x.getEndPos(), x.getLength()));
    }

    @Test
    public void testBasicXMLActions() throws IOException {
        System.out.println(ActionsIoUtils.toXml(src, actions, mappings));
    }


    @Test
    public void testBasicTextActions() throws IOException {
        System.out.println(ActionsIoUtils.toText(src, actions, mappings));
    }

    @Test
    public void testBasicJsonActions() throws IOException {
        System.out.println(ActionsIoUtils.toJson(src, actions, mappings));
    }
}
