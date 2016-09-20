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

package com.github.gumtreediff.actions;

import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

import java.util.List;

public class ActionUtil {
    private ActionUtil() {}

    public static TreeContext apply(TreeContext context, List<Action> actions) {
        for (Action a: actions) {
            if (a instanceof Insert) {
                Insert action = ((Insert) a);
                action.getParent().insertChild(action.getNode(), action.getPosition());
            } else if (a instanceof Update) {
                Update action = ((Update) a);
                action.getNode().setLabel(action.getValue());
            } else if (a instanceof Move) {
                Move action = ((Move) a);
                action.getNode().getParent().getChildren().remove(action.getNode());
                action.getParent().insertChild(action.getNode(), action.getPosition());
            } else if (a instanceof Delete) {
                Delete action = ((Delete) a);
                action.getNode().getParent().getChildren().remove(action.getNode());
            } else throw new RuntimeException("No such action: " + a );
        }
        return context;
    }
}
