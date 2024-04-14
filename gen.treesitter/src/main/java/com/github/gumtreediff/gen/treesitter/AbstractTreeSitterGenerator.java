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
    // On windows, python commands are usually used to execute the python interpreter
    // if python is configured in the PATH environment variable
    public static final String PYTHON = "python";
    /* In Linux or macOS, Python2 is usually built in for compatibility reasons,
     * and Python commands are also occupied by Python2 (although Python2 has been removed
     * from newer distributions), so Python3 commands are usually used to call Python3 interpreters,
     * although this is not absolute
     */
    public static final String PYTHON3 = "python3";

    private static final String TREESITTER_CMD = System.getProperty("gt.ts.path",
            "tree-sitter-parser.py");
    private static final String CUSTOM_PYTHON_PATH = System.getProperty("gt.ts.py.path");
    private static final String SYSTEM_TYPE = System.getProperty("os.name").toLowerCase();

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
        if (!TREESITTER_CMD.endsWith(".py")) {
            return new String[]{TREESITTER_CMD, file, getParserName()};
        } else {
            if (CUSTOM_PYTHON_PATH != null) {
                return new String[]{CUSTOM_PYTHON_PATH, TREESITTER_CMD, file, getParserName()};
            } else {
                return SYSTEM_TYPE.startsWith("windows")
                        ? new String[]{PYTHON, TREESITTER_CMD, file, getParserName()} :
                        new String[]{PYTHON3, TREESITTER_CMD, file, getParserName()};
            }
        }
    }
}
