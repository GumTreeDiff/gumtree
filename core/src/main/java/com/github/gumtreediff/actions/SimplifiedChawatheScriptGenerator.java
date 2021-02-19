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

package com.github.gumtreediff.actions;

import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;

import java.util.HashMap;
import java.util.Map;

/**
 * A script generator, based upon the Chawathe algorithm,
 * that makes use of deleted and inserted subtrees actions.
 *
 * @see ChawatheScriptGenerator
 */
public class SimplifiedChawatheScriptGenerator implements EditScriptGenerator {
    @Override
    public EditScript computeActions(MappingStore ms) {
        EditScript actions = new ChawatheScriptGenerator().computeActions(ms);
        return simplify(actions);
    }

    private static EditScript simplify(EditScript actions) {
        Map<Tree, Action> addedTrees = new HashMap<>();
        Map<Tree, Action> deletedTrees = new HashMap<>();

        for (Action a: actions)
            if (a instanceof Insert)
                addedTrees.put(a.getNode(), a);
            else if (a instanceof Delete)
                deletedTrees.put(a.getNode(), a);


        for (Tree t : addedTrees.keySet()) {
            if (addedTrees.keySet().contains(t.getParent())
                    && addedTrees.keySet().containsAll(t.getParent().getDescendants()))
                actions.remove(addedTrees.get(t));
            else {
                if (t.getChildren().size() > 0 && addedTrees.keySet().containsAll(t.getDescendants())) {
                    Insert originalAction = (Insert) addedTrees.get(t);
                    TreeInsert ti = new TreeInsert(originalAction.getNode(),
                            originalAction.getParent(), originalAction.getPosition());
                    int index = actions.lastIndexOf(originalAction);
                    actions.add(index, ti);
                    actions.remove(index +  1);
                }
            }
        }

        for (Tree t : deletedTrees.keySet()) {
            if (deletedTrees.keySet().contains(t.getParent())
                    && deletedTrees.keySet().containsAll(t.getParent().getDescendants()))
                actions.remove(deletedTrees.get(t));
            else {
                if (t.getChildren().size() > 0 && deletedTrees.keySet().containsAll(t.getDescendants())) {
                    Delete originalAction = (Delete) deletedTrees.get(t);
                    TreeDelete ti = new TreeDelete(originalAction.getNode());
                    int index = actions.lastIndexOf(originalAction);
                    actions.add(index, ti);
                    actions.remove(index +  1);
                }
            }
        }

        return actions;
    }
}
