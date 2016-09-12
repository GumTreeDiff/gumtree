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

package com.github.gumtreediff.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;

public class LineReader extends Reader {
    private Reader reader;

    private int currentPos = 0;

    private ArrayList<Integer> lines = new ArrayList<>(Arrays.asList(0));

    public LineReader(Reader parent) {
        reader = parent;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int r = reader.read(cbuf, off, len);
        for (int i = 0; i < len; i ++)
            if (cbuf[off + i] == '\n')
                lines.add(currentPos + i);
        currentPos += len;
        return r;
    }

    // Line and column starts at 1
    public int positionFor(int line, int column) {
        return lines.get(line - 1) + column - 1;
    }

    // public int[] positionFor(int offset) { // TODO write this method
    // Arrays.binarySearch(lines., null, null)
    // }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
