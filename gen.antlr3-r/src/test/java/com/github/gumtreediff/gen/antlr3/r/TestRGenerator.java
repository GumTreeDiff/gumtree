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
import java.util.stream.Stream;

import com.github.gumtreediff.tree.Tree;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.github.gumtreediff.tree.TypeSet.type;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class TestRGenerator {

    public static final String SEQUENCE = RParser.tokenNames[RParser.SEQUENCE];

    static Stream<Arguments> provideStringAndExpectedLength() {
        return Stream.of(
                arguments("v <- c(1,2,3);", SEQUENCE, 8)
        );
    }

    @ParameterizedTest
    @MethodSource("provideStringAndExpectedLength")
    public void testSimpleParse(String input, String expectedRootType, int expectedSize) throws IOException {
        Tree t = new RTreeGenerator().generateFrom().string(input).getRoot();
        assertEquals(type(expectedRootType), t.getType());
        assertEquals(expectedSize, t.getMetrics().size);
    }
}
