package fr.labri.gumtree.gen.r;

import java.io.IOException;
import java.io.Reader;

import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.tree.CommonTree;

import fr.labri.gumtree.gen.antlr.AbstractAntlrTreeGenerator;

public class RTreeGenerator extends AbstractAntlrTreeGenerator {

	@Override
	protected CommonTree getStartSymbol(Reader r) throws RecognitionException, IOException {
		ANTLRStringStream stream = new ANTLRReaderStream(r);
		RLexer rl = new RLexer(stream);
		tokens = new TokenRewriteStream(rl);
		RParser rp = new RParser(tokens);
		return (CommonTree) rp.script().getTree();
	}
	
	@Override
	public boolean handleFile(String file) {
		return file.toLowerCase().endsWith(".r");
	}

	@Override
	public String getName() {
		return "r-antlr";
	}

	@Override
	final protected String[] getTokenNames() {
		return RParser.tokenNames;
	}
}
