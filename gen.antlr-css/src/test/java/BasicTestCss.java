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
 */

import com.github.gumtreediff.gen.antlrcss.CssGrammarTreeGenerator;
import com.github.gumtreediff.tree.TreeContext;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;

public class BasicTestCss {

    @Test
    public void testParse() throws IOException {
        CssGrammarTreeGenerator gen = new CssGrammarTreeGenerator();
        TreeContext res = gen.generate(new InputStreamReader(getClass().getResourceAsStream("trivial.css")));
        System.out.println(res);
    }
}
