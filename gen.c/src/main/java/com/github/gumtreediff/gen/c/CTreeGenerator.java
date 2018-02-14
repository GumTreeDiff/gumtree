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

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TreeContext.MetadataSerializers;
import com.github.gumtreediff.tree.TreeContext.MetadataUnserializers;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.regex.Pattern;

@Register(id = "c-cocci", accept = "\\.[ch]$")
public class CTreeGenerator extends TreeGenerator {

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
        //FIXME this is not efficient but I am not sure how to speed up things here.
        File f = File.createTempFile("gumtree", ".c");
        try (
                Writer w = Files.newBufferedWriter(f.toPath(), Charset.forName("UTF-8"));
                BufferedReader br = new BufferedReader(r);
        ) {
            String line = br.readLine();
            while (line != null) {
                w.append(line + System.lineSeparator());
                line = br.readLine();
            }
        }
        ProcessBuilder b = new ProcessBuilder(COCCI_CMD, f.getAbsolutePath());
        b.directory(f.getParentFile());
        Process p = b.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"))) {
            StringBuilder buf = new StringBuilder();
            // TODO Why do we need to read and bufferize eveything, when we could/should only use generateFromStream
            String line = null;
            while ((line = br.readLine()) != null)
                buf.append(line + System.lineSeparator());
            p.waitFor();
            if (p.exitValue() != 0)
                throw new RuntimeException(
                    String.format("cgum Error [%d] %s\n", p.exitValue(), buf.toString())
                );
            String xml = buf.toString();
            return TreeIoUtils.fromXml(CTreeGenerator.defaultUnserializers).generateFromString(xml);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            f.delete();
        }
    }
}
