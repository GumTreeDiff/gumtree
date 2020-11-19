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
import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.DefaultTree;
import com.github.gumtreediff.tree.Type;
import com.github.gumtreediff.tree.TypeSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestClassicGumtreeStability {
    static int posNum = 0;

    @Test
    public void testStability() {
        EditScript previousScript = null;
        Tree src = getSrcTree();
        Tree dst = getDstTree();
        for (int i = 0; i < 20; i++ ) {
            Matcher matcher = new CompositeMatchers.ClassicGumtree();
            MappingStore mappingStore = matcher.match(src, dst);
            ChawatheScriptGenerator scriptGenerator = new ChawatheScriptGenerator();
            EditScript currentScript = scriptGenerator.computeActions(mappingStore);
            if (previousScript == null) {
                previousScript = currentScript;
            }
            else {
                assertIterableEquals(previousScript, currentScript);
                previousScript = currentScript;
            }
        }
    }

    private static Tree getSrcTree() {
        posNum = 0;
        Tree tree0 = generateTree("Empty");
        Tree tree1 = generateTree("Identifier", "delete");
        Tree tree2 = generateTree("Identifier", "value");
        Tree tree3 = generateTree("Call");
        tree3.addChild(tree0);
        tree3.addChild(tree1);
        tree3.addChild(tree2);

        Tree tree4 = generateTree("Integer", "1");
        Tree tree5 = generateTree("GreaterThan");
        tree5.addChild(tree3);
        tree5.addChild(tree4);

        Tree tree6 = generateTree("Empty");
        Tree tree7 = generateTree("Identifier", "move");
        Tree tree8 = generateTree("Identifier", "value");
        Tree tree9 = generateTree("TextElement");
        Tree tree10 = generateTree("Call");
        tree10.addChild(tree6);
        tree10.addChild(tree7);
        tree10.addChild(tree8);
        tree10.addChild(tree9);

        Tree tree11 = generateTree("Boolean");
        Tree tree12 = generateTree("Return");
        tree12.addChild(tree11);

        Tree tree13 = generateTree("Boolean");
        Tree tree14 = generateTree("Return");
        tree14.addChild(tree13);

        Tree tree15 = generateTree("If");
        tree15.addChild(tree5);
        tree15.addChild(tree10);
        tree15.addChild(tree12);
        tree15.addChild(tree14);

        Tree tree17 = generateTree("Identifier", "value");
        Tree tree18 = generateTree("Identifier", "str");
        Tree tree19 = generateTree("Annotation");
        tree19.addChild(tree17);
        tree19.addChild(tree18);

        Tree tree20 = generateTree("Function");
        tree20.addChild(tree15);
        Tree tree16 = generateTree("Identifier", "update");
        tree20.addChild(tree16);
        tree20.addChild(tree19);

        return tree20;
    }

    private static Tree getDstTree() {
        posNum = 0;
        Tree tree21 = generateTree("Empty");
        Tree tree22 = generateTree("Identifier", "add");
        Tree tree23 = generateTree("Identifier", "value");
        Tree tree24 = generateTree("Call");
        tree24.addChild(tree21);
        tree24.addChild(tree22);
        tree24.addChild(tree23);

        Tree tree25 = generateTree("Integer", "10");
        Tree tree26 = generateTree("GreaterThan");
        tree26.addChild(tree24);
        tree26.addChild(tree25);

        Tree tree27 = generateTree("Empty");
        Tree tree28 = generateTree("Identifier", "move");
        Tree tree29 = generateTree("Identifier", "value");
        Tree tree30 = generateTree("TextElement");
        Tree tree31 = generateTree("Call");
        tree31.addChild(tree27);
        tree31.addChild(tree28);
        tree31.addChild(tree29);
        tree31.addChild(tree30);

        Tree tree32 = generateTree("Boolean");
        Tree tree33 = generateTree("Return");
        tree33.addChild(tree32);

        Tree tree34 = generateTree("Empty");
        Tree tree35 = generateTree("Identifier", "map");
        Tree tree36 = generateTree("Identifier", "value");
        Tree tree37 = generateTree("Call");
        tree37.addChild(tree34);
        tree37.addChild(tree35);
        tree37.addChild(tree36);

        Tree tree38 = generateTree("Return");
        tree38.addChild(tree37);

        Tree tree39 = generateTree("If");
        tree39.addChild(tree26);
        tree39.addChild(tree31);
        tree39.addChild(tree33);
        tree39.addChild(tree38);



        Tree tree41 = generateTree("Identifier", "value");
        Tree tree42 = generateTree("Identifier", "str");
        Tree tree43 = generateTree("Annotation");
        tree43.addChild(tree41);
        tree43.addChild(tree42);

        Tree tree44 = generateTree("Function");
        tree44.addChild(tree39);
        Tree tree40 = generateTree("Identifier", "update");
        tree44.addChild(tree40);
        tree44.addChild(tree43);

        return tree44;
    }

    private static Tree generateTree(String type) {
        return generateTree(type, "");
    }

    private static Tree generateTree(String type, String label) {
        Type itype = TypeSet.type(type);
        Tree iTree = new DefaultTree(itype, label);
        iTree.setPos(posNum);
        iTree.setLength(posNum + 1);
        posNum++;
        return iTree;
    }
}
