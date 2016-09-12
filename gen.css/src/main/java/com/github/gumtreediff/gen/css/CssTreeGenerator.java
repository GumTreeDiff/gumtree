package com.github.gumtreediff.gen.css;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.io.LineReader;
import com.github.gumtreediff.tree.TreeContext;
import com.helger.commons.io.IHasReader;
import com.helger.css.ECSSVersion;
import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.decl.visit.CSSVisitor;
import com.helger.css.decl.visit.DefaultCSSVisitor;
import com.helger.css.decl.visit.ICSSVisitor;
import com.helger.css.handler.CSSHandler;
import com.helger.css.parser.*;
import com.helger.css.reader.CSSReader;
import com.helger.css.reader.CSSReaderSettings;


import javax.annotation.Nullable;
import java.io.*;

@Register(id = "css-phcss", accept = {"\\.css"})
public class CssTreeGenerator extends TreeGenerator {

    public TreeContext generate(Reader r) throws IOException {
        LineReader lr = new LineReader(r);
        CSSCharStream s = new CSSCharStream(new LineReader(lr));
        s.setTabSize(1);
        final ParserCSS30TokenManager th = new ParserCSS30TokenManager(s);
        final ParserCSS30 p = new ParserCSS30(th);
        p.setCustomErrorHandler(null);
        p.setBrowserCompliantMode(false);
        try {
            CascadingStyleSheet sheet = CSSHandler.readCascadingStyleSheetFromNode(ECSSVersion.LATEST, p.styleSheet());
            GtCssVisitor v = new GtCssVisitor(sheet, lr);
            CSSVisitor.visitCSS(sheet, v);
            return v.getTreeContext();
        } catch (ParseException e) {
            throw new IOException(e);
        }
    }
}
