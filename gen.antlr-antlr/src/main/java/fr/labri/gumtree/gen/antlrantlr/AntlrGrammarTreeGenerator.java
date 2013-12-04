package fr.labri.gumtree.gen.antlrantlr;

import java.io.IOException;

import org.antlr.runtime.*;
import org.antlr.runtime.tree.CommonTree;

import fr.labri.gumtree.gen.antlr.AbstractAntlrTreeGenerator;
import fr.labri.gumtree.gen.antlrantlr.ANTLRv3Lexer;
import fr.labri.gumtree.gen.antlrantlr.ANTLRv3Parser;

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
	protected Parser getEmptyParser() {
		ANTLRStringStream stream = new ANTLRStringStream();
		ANTLRv3Lexer l = new ANTLRv3Lexer(stream);
		CommonTokenStream tokens = new TokenRewriteStream(l);
		return new ANTLRv3Parser(tokens);
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
