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

package com.github.gumtreediff.gen.antlr4.xml;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.antlr4.AbstractAntlr4TreeGenerator;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

@Register(id = "xml-antlr4", accept = {"\\.xml$", "\\.xsd$", "\\.wadl$"})
public class XmlTreeGenerator extends AbstractAntlr4TreeGenerator {

    private CommonTokenStream tokens;
    private final HashMap<Integer, String> rules;

    public XmlTreeGenerator() {
        this.rules = new HashMap<>(8);
    }

    @Override
    protected ParseTree getTree(Reader r)  throws RecognitionException, IOException {
        //CodePointCharStream stream = CharStreams.fromReader(r);
        ANTLRInputStream stream = new ANTLRInputStream(r);
        XMLLexer l = new XMLLexer(stream);

        tokens = new CommonTokenStream(l);
        XMLParser p = new XMLParser(tokens);
        p.setBuildParseTree(true);

        // reverse name->index rule map
        for (Map.Entry<String, Integer> entry : p.getRuleIndexMap().entrySet()) {
            rules.put(entry.getValue(), entry.getKey());
        }
        // calling start rule of XML
        return p.document();
    }

    @Override
    protected String[] getTokenNames() {
        return XMLParser.tokenNames;
    }

    /** Preferable against XMLParser.tokenNames */
    protected Vocabulary getVocabulary() {
        return XMLLexer.VOCABULARY;
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

        int childrenCount = ct.getChildCount();
        for (int childIndex = 0; childIndex < childrenCount; childIndex++) {
            ParseTree cct = ct.getChild(childIndex);
            buildTree(context, tree, cct);
        }
    }
}


