package fr.labri.gumtree.gen.xml;

import java.io.IOException;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.tree.CommonTree;

import fr.labri.gumtree.gen.antlr.AbstractAntlrTreeGenerator;
import fr.labri.gumtree.tree.ITree;
import fr.labri.gumtree.tree.TreeContext;

public class XMLTreeGenerator extends AbstractAntlrTreeGenerator {
	
	@Override
	public TreeContext generate(String file) throws IOException {
		TreeContext ctx = super.generate(file);
		ITree t = ctx.getRoot();
		
		for(ITree c: t.getTrees()) { // Prune top level empty pcdata
			if (c.getType() == XMLParser.PCDATA && c.getLabel().trim().equals("") ) {
				c.setParentAndUpdateChildren(null);
			}
		}
		return ctx;
	}

	@Override
	protected CommonTree getStartSymbol(String file) throws RecognitionException, IOException {
		ANTLRStringStream stream = new ANTLRFileStream(file);
		XMLLexer l = new XMLLexer(stream);
		tokens = new TokenRewriteStream(l);
		XMLParser p = new XMLParser(tokens);
		return (CommonTree) p.document().getTree();
	}

	@Override
	final protected String[] getTokenNames() {
		return XMLParser.tokenNames;
	}
	
	@Override
	public final boolean handleFile(String file) {
		return 
				file.toLowerCase().endsWith(".xml") ||
				file.toLowerCase().endsWith(".wadl");
	}

	@Override
	public final String getName() {
		return "xml-antlr";
	}

}
