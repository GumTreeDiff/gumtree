package com.github.gumtreediff.gen.xml;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.antlr.AbstractAntlrTreeGenerator;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.tree.CommonTree;

import java.io.IOException;
import java.io.Reader;

@Register(id = "xml-antlr", accept = {"\\.xml$", "\\.xsd$", "\\.wadl$"})
public class XmlTreeGenerator extends AbstractAntlrTreeGenerator {

    @Override
    public TreeContext generate(Reader file) throws IOException {
        TreeContext ctx = super.generate(file);
        ITree t = ctx.getRoot();

        for (ITree c: t.getTrees()) { // Prune top level empty pcdata
            if (c.getType() == XMLParser.PCDATA && c.getLabel().trim().equals("") ) {
                c.setParentAndUpdateChildren(null);
            }
        }
        return ctx;
    }

    @Override
    protected CommonTree getStartSymbol(Reader r) throws RecognitionException, IOException {
        ANTLRStringStream stream = new ANTLRReaderStream(r);
        XMLLexer l = new XMLLexer(stream);
        tokens = new TokenRewriteStream(l);
        XMLParser p = new XMLParser(tokens);
        return p.document().getTree();
    }

    @Override
    protected final String[] getTokenNames() {
        return XMLParser.tokenNames;
    }
}
