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

package com.github.gumtreediff.gen.ruby;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.Registry;
import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.Type;
import com.github.gumtreediff.tree.TreeContext;
import org.jrubyparser.CompatVersion;
import org.jrubyparser.Parser;
import org.jrubyparser.ast.*;
import org.jrubyparser.parser.ParserConfiguration;

import java.io.IOException;
import java.io.Reader;

import static com.github.gumtreediff.tree.TypeSet.type;

@Register(id = "ruby-jruby", accept = {"\\.ruby$", "\\.rb$"}, priority = Registry.Priority.MAXIMUM)
public class RubyTreeGenerator extends TreeGenerator {

    @Override
    public TreeContext generate(Reader r) throws IOException {
        Parser p = new Parser();
        CompatVersion version = CompatVersion.RUBY2_0;
        ParserConfiguration config = new ParserConfiguration(0, version);
        try {
            Node n = p.parse("<code>", r, config);
            return extractTreeContext(new TreeContext(), n, null);
        }
        catch (org.jrubyparser.lexer.SyntaxException e) {
            throw new SyntaxException(this, r, e);
        }
    }

    private TreeContext extractTreeContext(TreeContext treeContext, Node node, Tree parent) {
        Type type = type(node.getNodeType().name());
        String label = extractLabel(node);
        Tree tree = treeContext.createTree(type, label);
        if (parent == null)
            treeContext.setRoot(tree);
        else
            tree.setParentAndUpdateChildren(parent);

        int pos = node.getPosition().getStartOffset();
        int length = node.getPosition().getEndOffset() - node.getPosition().getStartOffset();
        tree.setPos(pos);
        tree.setLength(length);

        for (Node childNode: node.childNodes())
            extractTreeContext(treeContext, childNode, tree);

        return treeContext;
    }

    private static String extractLabel(Node node) {
        if (node instanceof INameNode)
            return ((INameNode) node).getName();
        return "";
    }
}
