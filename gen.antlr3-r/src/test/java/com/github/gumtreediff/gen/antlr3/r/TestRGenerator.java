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

package com.github.gumtreediff.gen.antlr3.r;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Symbol;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static com.github.gumtreediff.tree.Symbol.symbol;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class TestRGenerator {

    public static final Symbol SEQUENCE = symbol(RParser.tokenNames[RParser.SEQUENCE]);

    private final String input;
    private final Symbol expectedRootSymbol;
    private final int expectedSize;

    public TestRGenerator(String input, Symbol expectedRootSymbol, int expectedSize) {
        this.input = input;
        this.expectedRootSymbol = expectedRootSymbol;
        this.expectedSize = expectedSize;
    }

    @Parameterized.Parameters
    public static Collection provideStringAndExpectedLength() {
        return Arrays.asList(new Object[][] {
                { "v <- c(1,2,3);", SEQUENCE, 8 },
        });
    }

    @Test
    public void testSimpleParse() throws IOException {
        ITree t = new RTreeGenerator().generateFrom().string(input).getRoot();
        assertEquals(expectedRootSymbol, t.getType());
        assertEquals(expectedSize, t.getSize());
    }

}
