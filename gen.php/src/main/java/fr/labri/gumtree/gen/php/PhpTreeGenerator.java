package fr.labri.gumtree.gen.php;

import java.io.IOException;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.tree.CommonTree;

import fr.labri.gumtree.gen.antlr.AbstractAntlrTreeGenerator;

public class PhpTreeGenerator extends AbstractAntlrTreeGenerator {

	@Override
	protected CommonTree getStartSymbol(String file) throws RecognitionException, IOException {
		ANTLRStringStream stream = new ANTLRFileStream(file);
		PhpLexer l = new PhpLexer(stream);
		tokens = new TokenRewriteStream(l);
		PhpParser p = new PhpParser(tokens);
		return (CommonTree) p.prog().getTree();
	}

	@Override
	final protected String[] getTokenNames() {
		return PhpParser.tokenNames;
	}
	
	@Override
	public final boolean handleFile(String file) {
		return file.toLowerCase().endsWith(".php");
	}

	@Override
	public final String getName() {
		return "php-antlr";
	}

}
