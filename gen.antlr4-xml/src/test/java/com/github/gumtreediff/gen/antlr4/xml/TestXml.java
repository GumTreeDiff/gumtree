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
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 * Copyright 2017 Svante Schubert <svante.schubert gmail com>
 */

package com.github.gumtreediff.gen.antlr4.xml;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;

public class TestXml {

    @Test
    public void testXml() {
        Run.initGenerators();
        String file1 = "src" + File.separatorChar + "test" + File.separatorChar + "resources" + File.separatorChar + "books1.xml";
        String file2 = "src" + File.separatorChar + "test" + File.separatorChar + "resources" + File.separatorChar + "books2.xml";
        ITree src = null;
        ITree dst = null;
        try {
            src = new XmlTreeGenerator().generateFromFile(file1).getRoot();
            dst = new XmlTreeGenerator().generateFromFile(file2).getRoot();
        } catch (Exception ex) {
            Logger.getLogger(TestXml.class.getName()).log(Level.SEVERE, null, ex);
        }
        Matcher m = Matchers.getInstance().getMatcher(src, dst); // retrieve the default matcher
        m.match();
        ActionGenerator g = new ActionGenerator(src, dst, m.getMappings());
        g.generate();
        java.util.List<Action> actions = g.getActions(); // return the actions
        for (Action a : actions) {
            System.err.println("**************ACTION: " + a.toString());
        }
    }

}


