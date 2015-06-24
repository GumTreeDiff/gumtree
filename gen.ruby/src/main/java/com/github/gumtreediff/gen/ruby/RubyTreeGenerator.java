/*
 * Copyright 2011 Jean-Rémy Falleri
 * 
 * This file is part of Praxis.
 * Praxis is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Praxis is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Praxis.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.gumtreediff.gen.ruby;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import org.jrubyparser.CompatVersion;
import org.jrubyparser.Parser;
import org.jrubyparser.ast.Node;
import org.jrubyparser.parser.ParserConfiguration;

import java.io.IOException;
import java.io.Reader;

@Register(id = "ruby-jruby", accept = {"\\.ruby$", "\\.rb$"})
public class RubyTreeGenerator extends TreeGenerator {

    public TreeContext generate(Reader r) throws IOException {
        Parser p = new Parser();
        CompatVersion version = CompatVersion.RUBY2_0;
        ParserConfiguration config = new ParserConfiguration(0, version);
        Node n = p.parse("<code>", r, config);
        return toTree(new TreeContext(), n, null);
    }

    private TreeContext toTree(TreeContext ctx, Node n, ITree parent) {
        String label = "";
        String typeLabel = n.getNodeType().name();
        int type = n.getNodeType().ordinal() + 1;
        ITree t = ctx.createTree(type, label, typeLabel);
        if (parent == null)
            ctx.setRoot(t);
        else
            t.setParentAndUpdateChildren(parent);

        int pos = n.getPosition().getStartOffset();
        int length = n.getPosition().getEndOffset() - n.getPosition().getStartOffset();
        t.setPos(pos);
        t.setLength(length);

        for (Node c: n.childNodes())
            toTree(ctx, c, t);

        return ctx;
    }
}
