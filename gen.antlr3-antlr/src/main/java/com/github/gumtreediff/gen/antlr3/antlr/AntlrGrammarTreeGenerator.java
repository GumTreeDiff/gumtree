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

package com.github.gumtreediff.gen.antlr3.antlr;

import com.github.gumtreediff.gen.Register;

import com.github.gumtreediff.gen.antlr3.AbstractAntlr3TreeGenerator;
import org.antlr.runtime.*;

@Register(id = "antlr-antlr", accept = "\\.[gG]$")
public class AntlrGrammarTreeGenerator extends AbstractAntlr3TreeGenerator<ANTLRv3Lexer, ANTLRv3Parser> {

    @Override
    protected ANTLRv3Lexer getLexer(ANTLRStringStream stream) {
        return new ANTLRv3Lexer(stream);
    }

    @Override
    protected ANTLRv3Parser getParser(TokenStream tokens) {
        return new ANTLRv3Parser(tokens);
    }

    @Override
    protected RuleReturnScope getStartRule(ANTLRv3Parser parser) throws RecognitionException {
        return parser.grammarDef();
    }

    @Override
    protected final String[] getTokenNames() {
        return ANTLRv3Parser.tokenNames;
    }
}
