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
 * Copyright 2017 Yijun Yu <y.yu@open.ac.uk>
 */

package com.github.gumtreediff.gen.antlr3.smali;

import com.github.gumtreediff.gen.antlr3.AbstractAntlr3TreeGenerator;
import com.github.gumtreediff.gen.Register;
import org.antlr.runtime.*;
import org.jf.smali.smaliParser;
import org.jf.smali.smaliFlexLexer;
import java.io.*;

@Register(id = "smali-antlr", accept = "\\.smali$")
public class SmaliTreeGenerator extends AbstractAntlr3TreeGenerator<smaliFlexLexer, smaliParser> {

    @Override
    protected smaliFlexLexer getLexer(ANTLRStringStream stream) {
	smaliFlexLexer lexer = new smaliFlexLexer(new StringReader(stream.toString()));
	lexer.setSuppressErrors(true);
        return lexer;
    }

    @Override
    protected smaliParser getParser(TokenStream tokens) {
        return new smaliParser(tokens);
    }

    @Override
    protected RuleReturnScope getStartRule(smaliParser parser) throws RecognitionException {
        return parser.smali_file();
    }

    @Override
    protected final String[] getTokenNames() {
        return smaliParser.tokenNames;
    }
}
