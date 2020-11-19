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

import com.github.gumtreediff.actions.*;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestTreeClassifier {
    @Test
    public void testAllNodesClassifier() {
        Pair<TreeContext, TreeContext> trees = TreeLoader.getActionPair();
        Tree src = trees.first.getRoot();
        Tree dst = trees.second.getRoot();
        MappingStore ms = new MappingStore(src, dst);
        ms.addMapping(src, dst);
        ms.addMapping(src.getChild(1), dst.getChild(0));
        ms.addMapping(src.getChild("1.0"), dst.getChild("0.0"));
        ms.addMapping(src.getChild("1.1"), dst.getChild("0.1"));
        ms.addMapping(src.getChild(0), dst.getChild(1).getChild(0));
        ms.addMapping(src.getChild("0.0"), dst.getChild("1.0.0"));
        ms.addMapping(src.getChild(4), dst.getChild(3));
        ms.addMapping(src.getChild("4.0"), dst.getChild("3.0.0.0"));
        EditScript actions = new SimplifiedChawatheScriptGenerator().computeActions(ms);
        Diff diff = new Diff(trees.first, trees.second, ms, actions);
        TreeClassifier c = diff.createAllNodeClassifier();
        assertThat(c.getUpdatedSrcs(), hasSize(1));
        assertThat(c.getUpdatedSrcs(), hasItems(
                src.getChild("0.0")));
        assertThat(c.getDeletedSrcs(), hasSize(3));
        assertThat(c.getDeletedSrcs(), hasItems(
                src.getChild("2"), src.getChild("2.0"), src.getChild("3")));
        assertThat(c.getMovedSrcs(), hasSize(3));
        assertThat(c.getMovedSrcs(), hasItems(
                src.getChild("0"), src.getChild("0.0"), src.getChild("4.0")));
        assertThat(c.getInsertedDsts(), hasSize(5));
        assertThat(c.getInsertedDsts(), hasItems(dst.getChild("2"),
                dst.getChild("3.0"), dst.getChild("1"), dst.getChild("2.0"), dst.getChild("3.0.0")));
        assertThat(c.getUpdatedDsts(), hasSize(1));
        assertThat(c.getUpdatedDsts(), hasItems(
                dst.getChild("1.0.0")));
        assertThat(c.getMovedDsts(), hasSize(3));
        assertThat(c.getMovedDsts(), hasItems(
                dst.getChild("1.0"), dst.getChild("1.0.0"), dst.getChild("3.0.0.0")));
    }

    @Test
    public void testOnlyRootsClassifier() {
        Pair<TreeContext, TreeContext> trees = TreeLoader.getActionPair();
        Tree src = trees.first.getRoot();
        Tree dst = trees.second.getRoot();
        MappingStore ms = new MappingStore(src, dst);
        ms.addMapping(src, dst);
        ms.addMapping(src.getChild(1), dst.getChild(0));
        ms.addMapping(src.getChild("1.0"), dst.getChild("0.0"));
        ms.addMapping(src.getChild("1.1"), dst.getChild("0.1"));
        ms.addMapping(src.getChild(0), dst.getChild(1).getChild(0));
        ms.addMapping(src.getChild("0.0"), dst.getChild("1.0.0"));
        ms.addMapping(src.getChild(4), dst.getChild(3));
        ms.addMapping(src.getChild("4.0"), dst.getChild("3.0.0.0"));
        EditScript actions = new SimplifiedChawatheScriptGenerator().computeActions(ms);
        Diff diff = new Diff(trees.first, trees.second, ms, actions);
        TreeClassifier c = diff.createRootNodesClassifier();
        assertThat(c.getUpdatedSrcs(), hasSize(1));
        assertThat(c.getUpdatedSrcs(), hasItems(
                src.getChild("0.0")));
        assertThat(c.getDeletedSrcs(), hasSize(2));
        assertThat(c.getDeletedSrcs(), hasItems(
                src.getChild("2"), src.getChild("3")));
        assertThat(c.getMovedSrcs(), hasSize(2));
        assertThat(c.getMovedSrcs(), hasItems(
                src.getChild("0"), src.getChild("4.0")));
        assertThat(c.getInsertedDsts(), hasSize(4));
        assertThat(c.getInsertedDsts(), hasItems(dst.getChild("2"),
                dst.getChild("3.0"), dst.getChild("1"), dst.getChild("3.0.0")));
        assertThat(c.getUpdatedDsts(), hasSize(1));
        assertThat(c.getUpdatedDsts(), hasItems(
                dst.getChild("1.0.0")));
        assertThat(c.getMovedDsts(), hasSize(2));
        assertThat(c.getMovedDsts(), hasItems(
                dst.getChild("1.0"), dst.getChild("3.0.0.0")));
    }
}
