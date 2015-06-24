package com.github.gumtreediff.gen.php;

import com.github.gumtreediff.gen.antlr.AbstractAntlrTreeGenerator;
import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.antlr.AbstractAntlrTreeGenerator;
import org.antlr.runtime.*;
import org.antlr.runtime.tree.CommonTree;

import java.io.IOException;
import java.io.Reader;

@Register(id = "php-antlr", accept = "\\.php.?$")
public class PhpTreeGenerator extends AbstractAntlrTreeGenerator {

    @Override
    protected CommonTree getStartSymbol(Reader r) throws RecognitionException, IOException {
        ANTLRStringStream stream = new ANTLRReaderStream(r);
        PhpLexer l = new PhpLexer(stream);
        tokens = new TokenRewriteStream(l);
        PhpParser p = new PhpParser(tokens);
        return p.prog().getTree();
    }

    @Override
    protected final String[] getTokenNames() {
        return PhpParser.tokenNames;
    }
}
