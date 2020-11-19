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

import com.github.gumtreediff.actions.ChawatheScriptGenerator;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.io.ActionsIoUtils;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import com.github.gumtreediff.tree.TreeContext;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

public class TestActionIo {
    private Pair<TreeContext, TreeContext> p;
    private Tree src;
    private Tree dst;
    private MappingStore ms;
    private EditScript actions;

    @BeforeEach
    public void setUp() throws Exception {
        p = TreeLoader.getActionPair();
        src = p.first.getRoot();
        dst = p.second.getRoot();
        ms = new MappingStore(src, dst);
        ms.addMapping(src, dst);
        ms.addMapping(src.getChild(1), dst.getChild(0));
        ms.addMapping(src.getChild("1.0"), dst.getChild("0.0"));
        ms.addMapping(src.getChild("1.1"), dst.getChild("0.1"));
        ms.addMapping(src.getChild(0), dst.getChild(1).getChild(0));
        ms.addMapping(src.getChild("0.0"), dst.getChild("1.0.0"));
        ms.addMapping(src.getChild(4), dst.getChild(3));
        ms.addMapping(src.getChild("4.0"), dst.getChild("3.0.0.0"));
        actions = new ChawatheScriptGenerator().computeActions(ms);
    }

    @Test
    public void testBasicXmlActions() throws IOException {
        System.out.println(ActionsIoUtils.toXml(p.first, actions, ms));
    }

    @Test
    public void testBasicTextActions() throws IOException {
        System.out.println(ActionsIoUtils.toText(p.first, actions, ms));
    }

    @Test
    public void testBasicJsonActions() throws IOException {
        System.out.println(ActionsIoUtils.toJson(p.first, actions, ms));
    }
}
