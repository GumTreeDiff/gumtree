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

package com.github.gumtreediff.gen.antlr3.json;

import static com.github.gumtreediff.tree.TypeSet.type;
import static org.junit.jupiter.api.Assertions.*;

import com.github.gumtreediff.tree.*;
import org.junit.jupiter.api.Test;

public class TestJsonParsing {
    public static final Type ARRAY = type(JSONParser.tokenNames[JSONParser.ARRAY]);

    @Test
    public void testJsonParsing() throws Exception {
        TreeContext tc = new AntlrJsonTreeGenerator().generateFrom().charset("UTF-8")
                .stream(getClass().getResourceAsStream("/sample.json"));
        Tree tree = tc.getRoot();

        assertEquals(ARRAY, tree.getType());
        assertEquals(37, tree.getMetrics().size);
    }
}
