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

package com.github.gumtreediff.gen.antlr3.json;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.antlr3.AbstractAntlr3TreeGenerator;
import org.antlr.runtime.*;

@Register(id = "json-antlr", accept =  "\\.json$")
public class AntlrJsonTreeGenerator extends AbstractAntlr3TreeGenerator<JSONLexer, JSONParser> {

    @Override
    protected JSONLexer getLexer(ANTLRStringStream stream) {
        return new JSONLexer(stream);
    }

    @Override
    protected JSONParser getParser(TokenStream tokens) {
        return new JSONParser(tokens);
    }

    @Override
    protected RuleReturnScope getStartRule(JSONParser parser) throws RecognitionException {
        return parser.value();
    }

    @Override
    protected final String[] getTokenNames() {
        return JSONParser.tokenNames;
    }
}
