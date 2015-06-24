package com.github.gumtreediff.gen.r;

import com.github.gumtreediff.gen.antlr.AbstractAntlrTreeGenerator;
import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.antlr.AbstractAntlrTreeGenerator;
import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.tree.CommonTree;

import java.io.IOException;
import java.io.Reader;

@Register(id = "r-antlr", accept = "\\.[rR]$")
public class RTreeGenerator extends AbstractAntlrTreeGenerator {

    @Override
    protected CommonTree getStartSymbol(Reader r) throws RecognitionException, IOException {
        ANTLRStringStream stream = new ANTLRReaderStream(r);
        RLexer rl = new RLexer(stream);
        tokens = new TokenRewriteStream(rl);
        RParser rp = new RParser(tokens);
        return rp.script().getTree();
    }

    @Override
    protected final String[] getTokenNames() {
        return RParser.tokenNames;
    }
}
