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

package com.github.gumtreediff.tree;

import java.util.HashMap;
import java.util.Map;

import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.io.TreeIoUtils;

public class TreeContext {

    Map<Integer, String> typeLabels = new HashMap<>();

    ITree root;

    @Override
    public String toString() {
        return TreeIoUtils.toLisp(this).toString();
    }

    public void setRoot(ITree root) {
        this.root = root;
    }

    public ITree getRoot() {
        return root;
    }

    public String getTypeLabel(ITree tree) {
        return getTypeLabel(tree.getType());
    }

    public String getTypeLabel(int type) {
        String tl = typeLabels.get(type);
        if (tl == null)
            tl = Integer.toString(type);
        return tl;
    }

    protected void registerTypeLabel(int type, String name) {
        if (name == null || name.equals(ITree.NO_LABEL))
            return;
        String typeLabel = typeLabels.get(type);
        if (typeLabel == null) {
            typeLabels.put(type, name);
        } else if (!typeLabel.equals(name))
            throw new RuntimeException(String.format("Redefining type %d: '%s' with '%s'", type, typeLabel, name));
    }

    public ITree createTree(int type, String label, String typeLabel) {
        registerTypeLabel(type, typeLabel);

        return new Tree(type, label);
    }

    public ITree createTree(ITree... trees) {
        return new AbstractTree.FakeTree(trees);
    }

    public void validate() {
        root.refresh();
        TreeUtils.postOrderNumbering(root);
    }

    public boolean hasLabelFor(int type) {
        return typeLabels.containsKey(type);
    }
}
