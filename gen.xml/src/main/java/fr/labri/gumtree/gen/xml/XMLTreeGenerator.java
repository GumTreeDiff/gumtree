package fr.labri.gumtree.gen.xml;

import java.io.IOException;

import org.antlr.runtime.*;
import org.antlr.runtime.tree.CommonTree;

import fr.labri.gumtree.gen.antlr.AbstractAntlrTreeGenerator;
import fr.labri.gumtree.tree.Tree;

public class XMLTreeGenerator extends AbstractAntlrTreeGenerator {
	
	@Override
	public Tree doGenerate(String file) throws IOException {
		Tree t = super.doGenerate(file);
		for(Tree c: t.getTrees()) {
			if (c.getTypeLabel().equals("PCDATA") && c.getLabel().trim().equals("") ) {
				c.setParentAndUpdateChildren(null);
			}
		}
		return t;
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
	protected Parser getEmptyParser() {
		ANTLRStringStream stream = new ANTLRStringStream();
		XMLLexer l = new XMLLexer(stream);
		CommonTokenStream tokens = new TokenRewriteStream(l);
		return new XMLParser(tokens);
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
