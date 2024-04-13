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
 * Copyright 2021 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.gen.treesitter;

import com.github.gumtreediff.gen.ExternalProcessTreeGenerator;
import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;

import java.io.IOException;
import java.io.Reader;

public abstract class AbstractTreeSitterGenerator extends ExternalProcessTreeGenerator {
    private static final String TREESITTER_CMD = System.getProperty("gt.ts.path",
            "tree-sitter-parser.py");

    @Override
    protected TreeContext generate(Reader r) throws IOException {
        String output = readStandardOutput(r);
        TreeContext context = TreeIoUtils.fromXml().generateFrom().string(output);

        for (Tree t : context.getRoot().preOrder())
            if (t.getType().name.equals("ERROR"))
                throw new SyntaxException(this, r,
                        new IllegalArgumentException("Syntax error at pos: " + t.getPos()));


        return context;
    }

    public abstract String getParserName();

    @Override
    protected String[] getCommandLine(String file) {
        return new String[]{TREESITTER_CMD, file, getParserName()};
    }
}
