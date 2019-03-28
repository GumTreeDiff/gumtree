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

package com.github.gumtreediff.gen.c;

import com.github.gumtreediff.gen.ExternalProcessTreeGenerator;
import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TreeContext.MetadataSerializers;
import com.github.gumtreediff.tree.TreeContext.MetadataUnserializers;

import java.io.*;
import java.util.Arrays;
import java.util.regex.Pattern;

@Register(id = "c-cocci", accept = "\\.[ch]$")
public class CTreeGenerator extends ExternalProcessTreeGenerator {

    private static final String COCCI_CMD = System.getProperty("gt.cgum.path", "cgum");

    private static final MetadataSerializers defaultSerializers = new MetadataSerializers();
    private static final MetadataUnserializers defaultUnserializers = new MetadataUnserializers();

    static {
        defaultSerializers.add("lines", x -> Arrays.toString((int[]) x));
        Pattern comma = Pattern.compile(", ");
        defaultUnserializers.add("lines", x -> {
            String[] v = comma.split(x.substring(1, x.length() - 2), 4);
            int[] ints = new int[v.length];
            for (int i = 0; i < ints.length; i++) {
                ints[i] = Integer.parseInt(v[i]);
            }
            return ints;
        });
    }

    @Override
    public TreeContext generate(Reader r) throws IOException {
        String output = readStandardOutput(r);
        return TreeIoUtils.fromXml(CTreeGenerator.defaultUnserializers).generateFrom().string(output);
    }

    protected String[] getCommandLine(String file) {
        return new String[]{COCCI_CMD, file};
    }
}
