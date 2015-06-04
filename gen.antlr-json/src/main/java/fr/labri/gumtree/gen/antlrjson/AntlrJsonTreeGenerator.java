package fr.labri.gumtree.gen.antlrjson;

import java.io.IOException;
import java.io.Reader;

import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.tree.CommonTree;

import fr.labri.gumtree.gen.antlr.AbstractAntlrTreeGenerator;

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
	final protected String[] getTokenNames() {
		return JSONParser.tokenNames;
	}
	
	@Override
	public boolean handleFile(String file) {
		return file.toLowerCase().endsWith(".json");
	}

	@Override
	public String getName() {
		return "antlr-json";
	}

}
