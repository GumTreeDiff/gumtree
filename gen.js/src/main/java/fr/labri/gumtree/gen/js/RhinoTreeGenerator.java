/*
 * Copyright 2011 Jean-Rémy Falleri
 * 
 * This file is part of Praxis.
 * Praxis is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Praxis is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Praxis.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.labri.gumtree.gen.js;

import fr.labri.gumtree.gen.Register;
import fr.labri.gumtree.gen.TreeGenerator;
import fr.labri.gumtree.tree.TreeContext;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;

import java.io.IOException;
import java.io.Reader;

@Register(id = "js-rhino", accept = "\\.js$")
public class RhinoTreeGenerator extends TreeGenerator {

    public TreeContext generate(Reader r) throws IOException {
        Parser p = new Parser();
        AstRoot root = p.parse(r, null, 1);
        RhinoTreeVisitor visitor = new RhinoTreeVisitor(root);
        root.visit(visitor);
        return visitor.getTree(root);
    }
}
