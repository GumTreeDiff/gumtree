package fr.labri.gumtree.gen.antlrcss;

import fr.labri.gumtree.gen.antlr.AbstractAntlrTreeGenerator;
import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.tree.CommonTree;

import java.io.IOException;
import java.io.Reader;

public class CSSGrammarTreeGenerator extends AbstractAntlrTreeGenerator {

	@Override
	protected CommonTree getStartSymbol(Reader r) throws RecognitionException, IOException {
		ANTLRStringStream stream = new ANTLRReaderStream(r);
		CSSLexer l = new CSSLexer(stream);
		tokens = new TokenRewriteStream(l);
		CSSParser p = new CSSParser(tokens);
		return p.styleSheet().getTree();
	}

	@Override
	final protected String[] getTokenNames() {
		return CSSParser.tokenNames;
	}
	
	@Override
	public boolean handleFile(String file) {
		return file.toLowerCase().endsWith(".css");
	}

	@Override
	public String getName() {
		return "antlr-css";
	}

}
