package fr.labri.gumtree.gen.r;

import fr.labri.gumtree.gen.Register;
import fr.labri.gumtree.gen.antlr.AbstractAntlrTreeGenerator;
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
	final protected String[] getTokenNames() {
		return RParser.tokenNames;
	}
}
