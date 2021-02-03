/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2016 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.gen.css;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.Registry;
import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.io.LineReader;
import com.github.gumtreediff.tree.TreeContext;
import com.helger.css.ECSSVersion;
import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.decl.visit.CSSVisitor;
import com.helger.css.handler.CSSHandler;
import com.helger.css.parser.*;
import com.helger.css.reader.CSSReader;

import java.io.*;

@Register(id = "css-phcss", accept = {"\\.css$"}, priority = Registry.Priority.MAXIMUM)
public class CssTreeGenerator extends TreeGenerator {
    @Override
    public TreeContext generate(Reader r) throws IOException {
        LineReader lr = new LineReader(r);
        CSSCharStream s = new CSSCharStream(new LineReader(lr));
        s.setTabSize(1);
        final ParserCSS30TokenManager th = new ParserCSS30TokenManager(s);
        final ParserCSS30 p = new ParserCSS30(th);
        p.setCustomErrorHandler(null);
        p.setBrowserCompliantMode(false);
        try {
            CascadingStyleSheet sheet = CSSHandler.readCascadingStyleSheetFromNode(
                    ECSSVersion.LATEST,
                    CSSReader.getDefaultInterpretErrorHandler(),
                    true,
                    p.styleSheet());
            GtCssVisitor v = new GtCssVisitor(sheet, lr);
            CSSVisitor.visitCSS(sheet, v);
            return v.getTreeContext();
        }
        catch (ParseException e) {
            throw new SyntaxException(this, r, e);
        }
    }
}
