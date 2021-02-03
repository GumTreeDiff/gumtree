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
 * Copyright 2019 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.tree;

import java.util.Objects;

public class TreeMetricComputer extends TreeVisitor.InnerNodesAndLeavesVisitor {
    public static final String ENTER = "enter";
    public static final String LEAVE = "leave";
    public static final int BASE = 33;

    int currentDepth = 0;
    int currentPosition = 0;

    @Override
    public void startInnerNode(Tree tree) {
        currentDepth++;
    }

    @Override
    public void visitLeaf(Tree tree) {
        tree.setMetrics(new TreeMetrics(1, 0, leafHash(tree), leafStructureHash(tree), currentDepth, currentPosition));
        currentPosition++;
    }

    @Override
    public void endInnerNode(Tree tree) {
        currentDepth--;
        int sumSize = 0;
        int maxHeight = 0;
        int currentHash = 0;
        int currentStructureHash = 0;
        for (Tree child : tree.getChildren()) {
            TreeMetrics metrics = child.getMetrics();
            int exponent = 2 * sumSize + 1;
            currentHash += metrics.hash * hashFactor(exponent);
            currentStructureHash += metrics.structureHash * hashFactor(exponent);
            sumSize += metrics.size;
            if (metrics.height > maxHeight)
                maxHeight = metrics.height;
        }
        tree.setMetrics(new TreeMetrics(
                sumSize + 1,
                maxHeight + 1,
                innerNodeHash(tree, 2 * sumSize + 1, currentHash),
                innerNodeStructureHash(tree, 2 * sumSize + 1, currentStructureHash),
                currentDepth, currentPosition));
        currentPosition++;
    }

    private static int hashFactor(int exponent) {
        return fastExponentiation(BASE, exponent);
    }

    private static int fastExponentiation(int base, int exponent) {
        if (exponent == 0)
            return 1;
        if (exponent == 1)
            return base;
        int result = 1;
        while (exponent > 0) {
            if ((exponent & 1) != 0)
                result *= base;
            exponent >>= 1;
            base *= base;
        }
        return result;
    }

    private static int innerNodeHash(Tree tree, int size, int middleHash) {
        return Objects.hash(tree.getType(), tree.getLabel(), ENTER)
                + middleHash
                + Objects.hash(tree.getType(), tree.getLabel(), LEAVE) * hashFactor(size);
    }

    private static int innerNodeStructureHash(Tree tree, int size, int middleHash) {
        return Objects.hash(tree.getType(), ENTER)
               + middleHash
               + Objects.hash(tree.getType(), LEAVE) * hashFactor(size);
    }

    private static int leafHash(Tree tree) {
        return innerNodeHash(tree, 1, 0);
    }

    private static int leafStructureHash(Tree tree) {
        return innerNodeStructureHash(tree, 1, 0);
    }
}
