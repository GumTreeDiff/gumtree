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
 * Copyright 2020 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.matchers.heuristic.gt;

import com.github.gumtreediff.tree.Tree;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

public class DefaultPriorityTreeQueue implements PriorityTreeQueue {
    private Function<Tree, Integer> priorityCalculator;
    private SortedMap<Integer, List<Tree>> trees;
    private int minimumPriority;

    public DefaultPriorityTreeQueue(Tree root, int minimumPriority, Function<Tree, Integer> priorityCalculator) {
        this.trees = new TreeMap<>();
        this.setMinimumPriority(minimumPriority);
        this.setPriorityCalculator(priorityCalculator);
        add(root);
    }

    @Override
    public List<Tree> popOpen() {
        List<Tree> pop = pop();
        for (Tree t: pop)
            open(t);
        return pop;
    }

    @Override
    public void setPriorityCalculator(Function<Tree, Integer> priorityCalculator) {
        this.priorityCalculator = priorityCalculator;
    }

    @Override
    public List<Tree> pop() {
        return trees.remove(currentPriority());
    }

    @Override
    public void open(Tree tree) {
        for (Tree c: tree.getChildren())
            add(c);
    }

    @Override
    public int currentPriority() {
        return trees.lastKey();
    }

    @Override
    public void setMinimumPriority(int minimumPriority) {
        this.minimumPriority = minimumPriority;
    }

    @Override
    public int getMinimumPriority() {
        return this.minimumPriority;
    }

    @Override
    public boolean isEmpty() {
        return trees.isEmpty();
    }

    @Override
    public void clear() {
        trees.clear();
    }

    private void add(Tree t) {
        int priority = priorityCalculator.apply(t);
        if (priority < this.getMinimumPriority())
            return;

        if (trees.get(priority) == null)
            trees.put(priority, new ArrayList<>());
        trees.get(priority).add(t);
    }
}
