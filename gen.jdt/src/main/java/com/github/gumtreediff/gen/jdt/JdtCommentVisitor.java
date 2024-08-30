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

package com.github.gumtreediff.gen.jdt;

import com.github.gumtreediff.tree.*;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.dom.*;

import java.util.List;

/* Created by pourya on 2024-08-28*/
class JdtCommentVisitor extends JdtVisitor {
    final TreeContext context;
    final char[] source;

    public JdtCommentVisitor(IScanner scanner, TreeContext context) {
        super(scanner);
        this.source = scanner.getSource();
        this.context = context;
    }

    public boolean visit(BlockComment node) {
        return visitComment(node);
    }

    public boolean visit(LineComment node) {
        return visitComment(node);
    }

    public boolean visitComment(Comment node) {
        int start = node.getStartPosition();
        int end = start + node.getLength();
        Tree parent = findMostInnerEnclosingParent(context.getRoot(), start, end);
        Tree t = context.createTree(nodeAsSymbol(node), new String(source, start, end - start));
        t.setPos(start);
        t.setLength(node.getLength());
        insertChildProperly(parent, t);
        return true;
    }

    public void insertChildProperly(Tree parent, Tree newChild) {
        int position = 0;
        for (Tree child : parent.getChildren())
        {
            if (child.getPos() < newChild.getPos()) {
                position += 1;
            }
            else
                break;
        }
        parent.insertChild(newChild,position);
    }

    private Tree findMostInnerEnclosingParent(Tree root, int start, int end) {
        Tree mostInnerParent = root;
        List<Tree> children = root.getChildren();

        for (Tree child : children) {
            if (child.getPos() <= start && child.getEndPos() >= end) {
                Tree candidate = findMostInnerEnclosingParent(child, start, end);
                if (candidate.getPos() >= mostInnerParent.getPos()
                        && candidate.getEndPos() <= mostInnerParent.getEndPos()) {
                    mostInnerParent = candidate;
                }
            }
        }

        return mostInnerParent;
    }

}