package fr.labri.gumtree.gen.antlrjson;

import java.io.IOException;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.tree.CommonTree;

import fr.labri.gumtree.gen.antlr.AbstractAntlrTreeGenerator;

public class AntlrJsonTreeGenerator extends AbstractAntlrTreeGenerator {

	@Override
	protected CommonTree getStartSymbol(String file) throws RecognitionException, IOException {
		ANTLRStringStream stream = new ANTLRFileStream(file);
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
