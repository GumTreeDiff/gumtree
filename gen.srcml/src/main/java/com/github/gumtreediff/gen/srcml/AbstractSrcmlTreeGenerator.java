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

package com.github.gumtreediff.gen.srcml;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.tree.TreeContext;

import java.io.*;

public abstract class AbstractSrcmlTreeGenerator extends TreeGenerator {

    private static final String SRCML_CMD = "srcml";

    @Override
    public TreeContext generate(Reader r) throws IOException {
        SrcmlVisitor v = new SrcmlVisitor(new StringReader(getXml(r)));
        return v.getTreeContext();
    }

    public String getXml(Reader r) throws IOException {
        //FIXME this is not efficient but I am not sure how to speed up things here.
        File f = File.createTempFile("gumtree", "");
        FileWriter w = new FileWriter(f);
        BufferedReader br = new BufferedReader(r);
        String line = br.readLine();
        while (line != null) {
            w.append(line);
            w.append(System.lineSeparator());
            line = br.readLine();
        }
        w.close();
        br.close();
        ProcessBuilder b = new ProcessBuilder(getArguments(f.getAbsolutePath()));
        b.directory(f.getParentFile());
        try {
            Process p = b.start();
            StringBuffer buf = new StringBuffer();
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            // TODO Why do we need to read and bufferize eveything, when we could/should only use generateFromStream
            line = null;
            while ((line = br.readLine()) != null)
                buf.append(line + "\n");
            p.waitFor();
            if (p.exitValue() != 0)  throw new RuntimeException();
            r.close();
            String xml = buf.toString();
            return xml;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            f.delete();
        }
    }

    public abstract String getLanguage();

    public String[] getArguments(String file) {
        return new String[] {SRCML_CMD, "-l", getLanguage(), "--position", file};
    }
}
