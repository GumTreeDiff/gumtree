package fr.labri.gumtree.gen.antlrjson;

import fr.labri.gumtree.gen.Register;
import fr.labri.gumtree.gen.antlr.AbstractAntlrTreeGenerator;
import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.tree.CommonTree;

import java.io.IOException;
import java.io.Reader;

@Register(id = "json-antlr", accept =  "\\.json$")
public class AntlrJsonTreeGenerator extends AbstractAntlrTreeGenerator {

    @Override
    protected CommonTree getStartSymbol(Reader r) throws RecognitionException, IOException {
        ANTLRStringStream stream = new ANTLRReaderStream(r);
        JSONLexer l = new JSONLexer(stream);
        tokens = new TokenRewriteStream(l);
        JSONParser p = new JSONParser(tokens);
        return (CommonTree) p.value().getTree();
    }

    @Override
    protected final String[] getTokenNames() {
        return JSONParser.tokenNames;
    }
}
