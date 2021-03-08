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

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;

public class LineReader extends Reader {

    /**
     * Parent reader
     */
    private Reader reader;

    /**
     * Current offset position of the reader
     */
    private int currentPos = 0;

    /**
     * Array with the stream offsets of each line
     */
    private ArrayList<Integer> lines = new ArrayList<>(Arrays.asList(-1));

    /**
     * Instantiate a new LineReader
     * 
     * @param parent reader
     */
    public LineReader(Reader parent) {
        reader = parent;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int r = reader.read(cbuf, off, len);
        for (int i = 0; i < len; i++)
            if (cbuf[off + i] == '\n')
                lines.add(currentPos + i);
        currentPos += len;
        return r;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    /**
     * Converts a position given as a (line, column) into an offset.
     * 
     * @param line   in the associated stream
     * @param column in the associated stream
     * @return position as offset in the stream
     */
    public int positionFor(int line, int column) {
        if (lines.size() < line)
            return -1;

        return lines.get(line - 1) + column; // Line and column starts at 1
    }

    /**
     * Converts a position given as an offset into a (line, column) array.
     * 
     * @param offset in the associated stream
     * @return position as (line, column) in the stream
     */
    public int[] positionFor(int offset) {
        int line = Arrays.binarySearch(lines.toArray(), offset);
        int off;

        if (line < 0) {
            line = -(line) - 1; // If the offset is not in the lines array
            off = lines.get(line - 1); // Get offset of previous line
        } 
        else {
            off = lines.get(line) - 1; // Get offset of current line - 1
        }

        int column = offset - off;
        return new int[] { line, column }; // Line and column starts at 1
    }

}
