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
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.test;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.io.ActionsIoUtils;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.utils.Pair;
import com.github.gumtreediff.tree.TreeContext;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class TestActionIo {
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
        src.getRoot().breadthFirst().forEach(
                x -> System.out.printf("%d) %s [%d:%d]:%d\n",
                        x.getType(), x.getLabel(), x.getPos(), x.getEndPos(), x.getLength()));
    }

    @Test
    public void testBasicXmlActions() throws IOException {
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
