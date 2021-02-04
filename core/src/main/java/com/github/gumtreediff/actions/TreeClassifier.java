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

import com.github.gumtreediff.tree.Tree;

import java.util.Set;

/**
 * An interface to partition the nodes of an AST into sets of updated, deleted, moved,
 * and updated nodes.
 * @see Tree
 */
public interface TreeClassifier {
    /**
     * Return the set of updated nodes in the source AST.
     */
    Set<Tree> getUpdatedSrcs();

    /**
     * Return the set of deleted nodes in the source AST.
     */
    Set<Tree> getDeletedSrcs();

    /**
     * Return the set of moved nodes in the source AST.
     */
    Set<Tree> getMovedSrcs();

    /**
     * Return the set of updated nodes in the destination AST.
     */
    Set<Tree> getUpdatedDsts();

    /**
     * Return the set of inserted nodes in the destination AST.
     */
    Set<Tree> getInsertedDsts();

    /**
     * Return the set of moved nodes in the destination AST.
     */
    Set<Tree> getMovedDsts();
}
