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
 * Copyright 2017 Svante Schubert <svante.schubert gmail com>
 */
package com.github.gumtreediff.gen.antlr4.xml;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.antlr4.AbstractAntlr4TreeGenerator;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Vocabulary;

@Register(id = "xml-antlr4", accept = {"\\.xml$", "\\.xsd$", "\\.wadl$"})
/**
 * Creating XML from the grammar used in ANTLR 4 reference book and available at
 */
public class XmlTreeGenerator extends AbstractAntlr4TreeGenerator<XMLLexer, XMLParser> {

    @Override
    public XMLLexer getLexer(CharStream stream) {
        return new XMLLexer(stream);
    }

    @Override
    public XMLParser getParser(CommonTokenStream tokens) {
        return new XMLParser(tokens);
    }

    @Override
    public ParserRuleContext getStartRule(XMLParser parser) throws RecognitionException {
        return parser.document();
    }

    /**
     * Preferable against XMLParser.tokenNames
     *
     * @return new ANTLR4 token dictionary
     */
    public Vocabulary getVocabulary() {
        return XMLLexer.VOCABULARY;
    }
}
