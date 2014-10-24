package fr.labri.gumtree.gen.antlrantlr;

import java.io.IOException;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.tree.CommonTree;

import fr.labri.gumtree.gen.antlr.AbstractAntlrTreeGenerator;

public class AntlrGrammarTreeGenerator extends AbstractAntlrTreeGenerator {

	@Override
	protected CommonTree getStartSymbol(String file) throws RecognitionException, IOException {
		ANTLRStringStream stream = new ANTLRFileStream(file);
		ANTLRv3Lexer l = new ANTLRv3Lexer(stream);
		tokens = new TokenRewriteStream(l);
		ANTLRv3Parser p = new ANTLRv3Parser(tokens);
		return (CommonTree) p.grammarDef().getTree();
	}

	@Override
	final protected String[] getTokenNames() {
		return ANTLRv3Parser.tokenNames;
	}
	
	@Override
	public boolean handleFile(String file) {
		return file.toLowerCase().endsWith(".g");
	}

	@Override
	public String getName() {
		return "antlr-antlr";
	}

}
