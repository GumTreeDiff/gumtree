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

package com.github.gumtreediff.gen;

import com.github.gumtreediff.tree.TreeContext;
import org.atteo.classindex.IndexSubclasses;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@IndexSubclasses
public abstract class TreeGenerator {

    protected abstract TreeContext generate(Reader r) throws IOException;

    protected TreeContext generateTree(Reader r) throws IOException {
        return generate(r);
    }

    public ReaderConfigurator generateFrom() {
        return new ReaderConfigurator();
    }

    public class ReaderConfigurator {

        private String charsetName = "UTF-8";
        private Charset charset;

        private Charset charset() {
            return (charset != null) ? charset : Charset.forName(charsetName);
        }

        public ReaderConfigurator charset(Charset charset) {
            this.charset = charset;
            return this;
        }

        public ReaderConfigurator charset(String name) {
            charsetName = name;
            return this;
        }

        public TreeContext file(Path path) throws IOException {
            return reader(Files.newBufferedReader(path, charset()));
        }

        public TreeContext file(String path) throws IOException {
            return file(Paths.get(path));
        }

        public TreeContext file(File file) throws IOException {
            return file(file.toPath());
        }

        public TreeContext reader(Reader stream) throws IOException {
            return generateTree(stream);
        }

        public TreeContext stream(InputStream stream) throws IOException {
            return reader(new InputStreamReader(stream, charset()));
        }

        public TreeContext string(String content) throws IOException {
            return reader(new StringReader(content));
        }
    }
}
