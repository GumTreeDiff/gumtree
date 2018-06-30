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

package com.github.gumtreediff.gen.jdt;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestIssue100 {

    @Test
    public void shouldHaveAModifierNode() throws IOException {
        String left = "import com.github.gumtreediff.gen.jdt;";
        String right = "import static com.github.gumtreediff.gen.jdt;";

        ITree leftTree = new JdtTreeGenerator().generateFromString(left).getRoot();
        ITree rightTree = new JdtTreeGenerator().generateFromString(right).getRoot();
        Matcher m = Matchers.getInstance().getMatcher(leftTree, rightTree);
        m.match();

        ActionGenerator g = new ActionGenerator(leftTree, rightTree, m.getMappings());
        List<Action> actions = g.generate();

        assertEquals(1, actions.size());
        assertEquals("INS", actions.get(0).getName());
        assertEquals("static", actions.get(0).getNode().getLabel());
    }

    @Test
    public void shouldBeDifferentKindOfImport() throws IOException {
        String left = "import com.github.gumtreediff.gen.jdt;";
        String right = "import com.github.gumtreediff.gen.jdt.*;";

        ITree leftTree = new JdtTreeGenerator().generateFromString(left).getRoot();
        ITree rightTree = new JdtTreeGenerator().generateFromString(right).getRoot();
        Matcher m = Matchers.getInstance().getMatcher(leftTree, rightTree);
        m.match();

        ActionGenerator g = new ActionGenerator(leftTree, rightTree, m.getMappings());
        List<Action> actions = g.generate();

        assertEquals(1, actions.size());
        assertEquals("on-demand", ((Update) actions.get(0)).getValue());
        assertEquals("single-type", ((Update) actions.get(0)).getNode().getLabel());
    }

}
