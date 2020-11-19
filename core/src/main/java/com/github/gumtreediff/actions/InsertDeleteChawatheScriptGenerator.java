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

/**
 * A script generator, based upon the simplified Chawathe script generator,
 * that replaces moved and updated nodes by inserted and deleted nodes.
 *
 * @see SimplifiedChawatheScriptGenerator
 */
public class InsertDeleteChawatheScriptGenerator implements EditScriptGenerator {
    private EditScript actions;
    private MappingStore origMappings;

    @Override
    public EditScript computeActions(MappingStore ms) {
        this.origMappings = ms;
        this.actions = new SimplifiedChawatheScriptGenerator().computeActions(ms);
        return removeMovesAndUpdates();
    }

    private EditScript removeMovesAndUpdates() {
        EditScript actionsCpy = new EditScript();
        for (Action a: actions) {
            if (a instanceof Update) {
                Tree src = a.getNode();
                Tree dst = origMappings.getDstForSrc(src);
                actionsCpy.add(new Insert(
                        dst,
                        dst.getParent(),
                        dst.positionInParent()));
                actionsCpy.add(new Delete(a.getNode()));
            }
            else if (a instanceof Move) {
                Move m = (Move) a;
                Tree src = a.getNode();
                Tree dst = origMappings.getDstForSrc(src);
                actionsCpy.add(new TreeInsert(
                        dst,
                        dst.getParent(),
                        m.getPosition()));
                actionsCpy.add(new TreeDelete(a.getNode()));
            }
            else
                actionsCpy.add(a);
        }

        return actionsCpy;
    }
}
