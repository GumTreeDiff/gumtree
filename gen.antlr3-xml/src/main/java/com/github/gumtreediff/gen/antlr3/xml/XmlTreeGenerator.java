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

package com.github.gumtreediff.gen.antlr3.xml;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.antlr3.AbstractAntlr3TreeGenerator;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.Type;
import com.github.gumtreediff.tree.TreeContext;
import org.antlr.runtime.*;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import static com.github.gumtreediff.tree.TypeSet.type;

@Register(id = "xml-antlr", accept = {"\\.xml$", "\\.xsd$", "\\.wadl$"})
public class XmlTreeGenerator extends AbstractAntlr3TreeGenerator<XMLLexer, XMLParser> {

    private static final Type PCDATA = type(XMLParser.tokenNames[XMLParser.PCDATA]);

    @Override
    public TreeContext generate(Reader file) throws IOException {
        TreeContext ctx = super.generate(file);
        Tree t = ctx.getRoot();

        List<Tree> temp = new ArrayList<>();
        for (Tree c: t.preOrder()) { // Prune top level empty pcdata
            if (c.getType() == PCDATA && c.getLabel().trim().equals("") ) {
                temp.add(c);
            }
        }
        temp.forEach(c -> c.setParentAndUpdateChildren(null));
        
        return ctx;
    }

    @Override
    protected XMLLexer getLexer(ANTLRStringStream stream) {
        return new XMLLexer(stream);
    }

    @Override
    protected XMLParser getParser(TokenStream tokens) {
        return new XMLParser(tokens);
    }

    @Override
    protected RuleReturnScope getStartRule(XMLParser parser) throws RecognitionException {
        return parser.document();
    }

    @Override
    protected final String[] getTokenNames() {
        return XMLParser.tokenNames;
    }
}
