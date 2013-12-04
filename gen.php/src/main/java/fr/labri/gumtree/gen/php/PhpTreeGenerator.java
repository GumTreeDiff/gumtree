package fr.labri.gumtree.gen.php;

import java.io.IOException;

import org.antlr.runtime.*;
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
	protected Parser getEmptyParser() {
		ANTLRStringStream stream = new ANTLRStringStream();
		PhpLexer l = new PhpLexer(stream);
		CommonTokenStream tokens = new TokenRewriteStream(l);
		return new PhpParser(tokens);
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
