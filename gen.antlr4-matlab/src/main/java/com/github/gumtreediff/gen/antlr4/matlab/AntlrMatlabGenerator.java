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

package com.github.gumtreediff.gen.antlr4.matlab;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.antlr4.AbstractAntlr4TreeGenerator;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.io.Reader;

@Register(id = "matlab-antlr", accept =  "\\.m$")
public class AntlrMatlabGenerator extends AbstractAntlr4TreeGenerator {
    @Override
    protected ParseTree getStartSymbol(Reader r) throws RecognitionException, IOException {
        MatlabLexer lexer = new MatlabLexer(new ANTLRInputStream(r));
        MatlabParser parser = new MatlabParser(new CommonTokenStream(lexer));
        MatlabParser.ScriptMFileContext script = parser.scriptMFile();
        return script;
    }

    @Override
    protected String[] getTokenNames() {
        return new String[0];
    }
}
