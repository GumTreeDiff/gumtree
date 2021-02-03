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

/**
 * Class containing several metrics information regarding a node of an AST.
 * The metrics are immutables but lazily computed.
 *
 * @see Tree#getMetrics()
 */
public class TreeMetrics {
    /**
     * The number of nodes in the subtree rooted at the node.
     */
    public final int size;

    /**
     * The size of the longer branch in the subtree rooted at the node.
     */
    public final int height;

    /**
     * The hashcode of the subtree rooted at the node.
     */
    public final int hash;

    /**
     * The hashcode of the subtree rooted at the node, excluding labels.
     */
    public final int structureHash;

    /**
     * The number of ancestors of a node.
     */
    public final int depth;

    /**
     * An absolute position for the node. Usually computed via the postfix order.
     */
    public final int position;

    public TreeMetrics(int size, int height, int hash, int structureHash, int depth, int position) {
        this.size = size;
        this.height = height;
        this.hash = hash;
        this.structureHash = structureHash;
        this.depth = depth;
        this.position = position;
    }
}
