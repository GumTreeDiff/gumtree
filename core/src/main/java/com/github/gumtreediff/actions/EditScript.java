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

/**
 * Edit scripts are sequence of tree edit actions between two ASTs.
 * Edit scripts are iterable.
 * @see Action
 */
public class EditScript implements Iterable<Action> {
    private List<Action> actions;

    /**
     * Instantiate a new edit script.
     */
    public EditScript() {
        actions = new ArrayList<>();
    }

    @Override
    public Iterator<Action> iterator() {
        return actions.iterator();
    }

    /**
     * Add an action in the script.
     */
    public void add(Action action) {
        actions.add(action);
    }

    /**
     * Add an action in the script at the provided index.
     */
    public void add(int index, Action action) {
        actions.add(index, action);
    }

    /**
     * Return the at the given index.
     */
    public Action get(int index) {
        return actions.get(index);
    }

    /**
     * Return the number of actions.
     */
    public int size() {
        return actions.size();
    }

    /**
     * Remove the provided action from the script.
     */
    public boolean remove(Action action) {
        return actions.remove(action);
    }

    /**
     * Remove the action at the provided index from the script.
     */
    public Action remove(int index) {
        return actions.remove(index);
    }

    /**
     * Convert the edit script as a list of actions.
     */
    public List<Action> asList() {
        return actions;
    }

    /**
     * Return the index of the last occurence of the action in the script.
     */
    public int lastIndexOf(Action action) {
        return actions.lastIndexOf(action);
    }
}
