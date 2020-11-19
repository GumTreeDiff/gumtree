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
 * Copyright 2016 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.gen.python;

import com.github.gumtreediff.gen.ExternalProcessTreeGenerator;
import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.Registry;
import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.tree.TreeContext;

import java.io.*;

@Register(id = "python-pythonparser", accept = {"\\.py$"}, priority = Registry.Priority.MAXIMUM)
public class PythonTreeGenerator extends ExternalProcessTreeGenerator {

    private static final String PYTHONPARSER_CMD = System.getProperty("gt.pp.path", "pythonparser");

    @Override
    public TreeContext generate(Reader r) throws IOException {
        String output = readStandardOutput(r);
        return TreeIoUtils.fromXml().generateFrom().string(output);
    }

    public String[] getCommandLine(String file) {
        return new String[]{PYTHONPARSER_CMD, file};
    }
}
