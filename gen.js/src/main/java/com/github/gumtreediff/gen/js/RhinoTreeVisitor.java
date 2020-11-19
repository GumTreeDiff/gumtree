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

package com.github.gumtreediff.gen.js;

import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.*;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;

import static com.github.gumtreediff.tree.TypeSet.type;

public class RhinoTreeVisitor implements NodeVisitor {
    private Map<AstNode, Tree> trees;
    private TreeContext context;

    public RhinoTreeVisitor(AstRoot root) {
        trees = new HashMap<>();
        context = new TreeContext();
        Tree tree = buildTree(root);
        context.setRoot(tree);
    }

    public TreeContext getTreeContext() {
        return context;
    }

    @Override
    public boolean visit(AstNode node) {
        if (node instanceof AstRoot)
            return true;
        else {
            Tree t = buildTree(node);
            Tree p = trees.get(node.getParent());
            p.addChild(t);

            if (node instanceof Name) {
                Name name = (Name) node;
                t.setLabel(name.getIdentifier());
            } else if ( node instanceof StringLiteral) {
                StringLiteral literal = (StringLiteral) node;
                t.setLabel(literal.getValue());
            } else if ( node instanceof NumberLiteral) {
                NumberLiteral l = (NumberLiteral) node;
                t.setLabel(l.getValue());
            } else if ( node instanceof Comment) {
                Comment c = (Comment) node;
                t.setLabel(c.getValue());
            }

            return true;
        }
    }

    private Tree buildTree(AstNode node)  {
        Tree t = context.createTree(type(Token.typeToName(node.getType())), Tree.NO_LABEL);
        t.setPos(node.getAbsolutePosition());
        t.setLength(node.getLength());
        trees.put(node, t);
        return t;
    }
}
