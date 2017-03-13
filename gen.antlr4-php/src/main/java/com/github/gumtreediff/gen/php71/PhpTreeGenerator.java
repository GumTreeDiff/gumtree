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
 * Copyright 2017      Mikulas Dite <ditemiku@fit.cvut.cz>
 */

package com.github.gumtreediff.gen.php71;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.Registry;
import com.github.gumtreediff.gen.antlr4.AbstractAntlr4TreeGenerator;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

@Register(id = "php-antlr", accept = "\\.php.?$")
public class PhpTreeGenerator extends AbstractAntlr4TreeGenerator {

    private CommonTokenStream tokens;
    private HashMap<Integer, String> rules = new HashMap<Integer, String>();

    @Override
    protected ParseTree getTree(Reader r) throws RecognitionException, IOException {
        ANTLRInputStream stream = new ANTLRInputStream(r);
        PHPLexer l = new PHPLexer(stream);

        tokens = new CommonTokenStream(l);
        PHPParser p = new PHPParser(tokens);
        p.setBuildParseTree(true);

        // reverse name->index rule map
        for (Map.Entry<String, Integer> entry : p.getRuleIndexMap().entrySet()) {
            rules.put(entry.getValue(), entry.getKey());
        }

        return p.phpBlock();
    }

    @Override
    protected final String[] getTokenNames() {
        return PHPParser.tokenNames;
    }

    private ITree getTree(TreeContext context, ParseTree ct) {
        int index;
        String name = ct.getClass().getSimpleName();

        if (ct instanceof ParserRuleContext) {
            index = ((ParserRuleContext) ct).getRuleIndex();
            // Some simple rules extend rules without changing rule index,
            // if that is the case, set node name to that parent.
            name = rules.get(index);

        } else {
            assert ct instanceof TerminalNode;
            index = 100000; // assumes there are not more rules in parser than this
        }

        return context.createTree(index, null, name);
    }

    @Override
    protected void buildTree(TreeContext context, ITree root, ParseTree ct) {
        int childrenCount = ct.getChildCount();

        ITree tree = getTree(context, ct);
        tree.setParentAndUpdateChildren(root);

        Token firstToken = tokens.get(ct.getSourceInterval().a);
        Token lastToken = tokens.get(ct.getSourceInterval().b == -1
                ? ct.getSourceInterval().a : ct.getSourceInterval().b);

        tree.setPos(firstToken.getStartIndex());
        tree.setLength(lastToken.getStopIndex() - tree.getPos() + 1); // count last char

        if (ct instanceof TerminalNode) {
            tree.setLabel(ct.getText());
        } else {
            tree.setLabel(ct.getClass().getSimpleName());
        }

        for (int childIndex = 0; childIndex < childrenCount; childIndex++) {
            ParseTree cct = ct.getChild(childIndex);

            buildTree(context, tree, cct);
        }
    }
}
