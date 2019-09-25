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

import com.github.gumtreediff.actions.model.Action;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EditScript implements Iterable<Action> {
    private List<Action> actions;

    public EditScript() {
        actions = new ArrayList<>();
    }

    public Iterator<Action> iterator() {
        return actions.iterator();
    }

    public void add(Action action) {
        actions.add(action);
    }

    public void add(int index, Action action) {
        actions.add(index, action);
    }

    public Action get(int index) {
        return actions.get(index);
    }

    public int size() {
        return actions.size();
    }

    public boolean remove(Action action) {
        return actions.remove(action);
    }

    public Action remove(int index) {
        return actions.remove(index);
    }

    public List<Action> asList() {
        return actions;
    }

    public int lastIndexOf(Action action) {
        return actions.lastIndexOf(action);
    }
}
