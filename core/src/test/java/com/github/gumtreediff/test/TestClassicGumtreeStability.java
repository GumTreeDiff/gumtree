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
 * Copyright 2011-2019 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.test;

import com.github.gumtreediff.actions.ChawatheScriptGenerator;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.heuristic.gt.GreedySubtreeMatcher;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.Type;
import com.github.gumtreediff.tree.TypeSet;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class TestClassicGumtreeStability {
    static int posNum = 0;

    @Test
    public void testStability() {
        EditScript previousScript = null;

        for (int i = 0; i < 20; i++ ) {
            ITree src = getSrcTree();
            ITree dst = getDstTree();
            Matcher matcher = new CompositeMatchers.ClassicGumtree();
            MappingStore mappingStore = matcher.match(src, dst);
            ChawatheScriptGenerator scriptGenerator = new ChawatheScriptGenerator();
            EditScript currentScript = scriptGenerator.computeActions(mappingStore);
            if (previousScript == null) {
                previousScript = currentScript;
            }
            else {
                assertThat(previousScript.size(), equalTo(currentScript.size()));
                previousScript = currentScript;
            }
        }
    }

    private static ITree getSrcTree() {
        posNum = 0;
        ITree tree0 = generateTree("Empty");
        ITree tree1 = generateTree("Identifier", "delete");
        ITree tree2 = generateTree("Identifier", "value");
        ITree tree3 = generateTree("Call");
        tree3.addChild(tree0);
        tree3.addChild(tree1);
        tree3.addChild(tree2);

        ITree tree4 = generateTree("Integer", "1");
        ITree tree5 = generateTree("GreaterThan");
        tree5.addChild(tree3);
        tree5.addChild(tree4);

        ITree tree6 = generateTree("Empty");
        ITree tree7 = generateTree("Identifier", "move");
        ITree tree8 = generateTree("Identifier", "value");
        ITree tree9 = generateTree("TextElement");
        ITree tree10 = generateTree("Call");
        tree10.addChild(tree6);
        tree10.addChild(tree7);
        tree10.addChild(tree8);
        tree10.addChild(tree9);

        ITree tree11 = generateTree("Boolean");
        ITree tree12 = generateTree("Return");
        tree12.addChild(tree11);

        ITree tree13 = generateTree("Boolean");
        ITree tree14 = generateTree("Return");
        tree14.addChild(tree13);

        ITree tree15 = generateTree("If");
        tree15.addChild(tree5);
        tree15.addChild(tree10);
        tree15.addChild(tree12);
        tree15.addChild(tree14);

        ITree tree17 = generateTree("Identifier", "value");
        ITree tree18 = generateTree("Identifier", "str");
        ITree tree19 = generateTree("Annotation");
        tree19.addChild(tree17);
        tree19.addChild(tree18);

        ITree tree20 = generateTree("Function");
        tree20.addChild(tree15);
        ITree tree16 = generateTree("Identifier", "update");
        tree20.addChild(tree16);
        tree20.addChild(tree19);

        return tree20;
    }

    private static ITree getDstTree() {
        posNum = 0;
        ITree tree21 = generateTree("Empty");
        ITree tree22 = generateTree("Identifier", "add");
        ITree tree23 = generateTree("Identifier", "value");
        ITree tree24 = generateTree("Call");
        tree24.addChild(tree21);
        tree24.addChild(tree22);
        tree24.addChild(tree23);

        ITree tree25 = generateTree("Integer", "10");
        ITree tree26 = generateTree("GreaterThan");
        tree26.addChild(tree24);
        tree26.addChild(tree25);

        ITree tree27 = generateTree("Empty");
        ITree tree28 = generateTree("Identifier", "move");
        ITree tree29 = generateTree("Identifier", "value");
        ITree tree30 = generateTree("TextElement");
        ITree tree31 = generateTree("Call");
        tree31.addChild(tree27);
        tree31.addChild(tree28);
        tree31.addChild(tree29);
        tree31.addChild(tree30);

        ITree tree32 = generateTree("Boolean");
        ITree tree33 = generateTree("Return");
        tree33.addChild(tree32);

        ITree tree34 = generateTree("Empty");
        ITree tree35 = generateTree("Identifier", "map");
        ITree tree36 = generateTree("Identifier", "value");
        ITree tree37 = generateTree("Call");
        tree37.addChild(tree34);
        tree37.addChild(tree35);
        tree37.addChild(tree36);

        ITree tree38 = generateTree("Return");
        tree38.addChild(tree37);

        ITree tree39 = generateTree("If");
        tree39.addChild(tree26);
        tree39.addChild(tree31);
        tree39.addChild(tree33);
        tree39.addChild(tree38);



        ITree tree41 = generateTree("Identifier", "value");
        ITree tree42 = generateTree("Identifier", "str");
        ITree tree43 = generateTree("Annotation");
        tree43.addChild(tree41);
        tree43.addChild(tree42);

        ITree tree44 = generateTree("Function");
        tree44.addChild(tree39);
        ITree tree40 = generateTree("Identifier", "update");
        tree44.addChild(tree40);
        tree44.addChild(tree43);

        return tree44;
    }

    private static ITree generateTree(String type) {
        return generateTree(type, "");
    }

    private static ITree generateTree(String type, String label) {
        Type itype = TypeSet.type(type);
        ITree iTree = new Tree(itype, label);
        iTree.setPos(posNum);
        iTree.setLength(posNum + 1);
        posNum++;
        return iTree;
    }
}
