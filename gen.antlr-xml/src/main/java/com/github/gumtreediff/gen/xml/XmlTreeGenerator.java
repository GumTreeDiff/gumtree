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

package com.github.gumtreediff.gen.xml;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.antlr.AbstractAntlrTreeGenerator;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.tree.CommonTree;

import java.io.IOException;
import java.io.Reader;

@Register(id = "xml-antlr", accept = {"\\.xml$", "\\.xsd$", "\\.wadl$"})
public class XmlTreeGenerator extends AbstractAntlrTreeGenerator {

    @Override
    public TreeContext generate(Reader file) throws IOException {
        TreeContext ctx = super.generate(file);
        ITree t = ctx.getRoot();

        for (ITree c: t.getTrees()) { // Prune top level empty pcdata
            if (c.getType() == XMLParser.PCDATA && c.getLabel().trim().equals("") ) {
                c.setParentAndUpdateChildren(null);
            }
        }
        return ctx;
    }

    @Override
    protected CommonTree getStartSymbol(Reader r) throws RecognitionException, IOException {
        ANTLRStringStream stream = new ANTLRReaderStream(r);
        XMLLexer l = new XMLLexer(stream);
        tokens = new TokenRewriteStream(l);
        XMLParser p = new XMLParser(tokens);
        return p.document().getTree();
    }

    @Override
    protected final String[] getTokenNames() {
        return XMLParser.tokenNames;
    }
}
