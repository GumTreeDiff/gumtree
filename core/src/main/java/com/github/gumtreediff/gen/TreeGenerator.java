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

/**
 * An abstract class for tree generators that ASTs from a input stream.
 * @see TreeContext
 */
@IndexSubclasses
public abstract class TreeGenerator {
    protected abstract TreeContext generate(Reader r) throws IOException;

    protected TreeContext generateTree(Reader r) throws IOException {
        return generate(r);
    }

    /**
     * Return a ReaderConfigurator that will allow the client to
     * configure and run the TreeGenerator.
     */
    public ReaderConfigurator generateFrom() {
        return new ReaderConfigurator();
    }

    /**
     * Class to configure the input stream for the tree generator.
     * The default charset is UTF-8.
     */
    public class ReaderConfigurator {

        private String charsetName = "UTF-8";
        private Charset charset;

        private Charset charset() {
            return (charset != null) ? charset : Charset.forName(charsetName);
        }

        /**
         * Set the charset for decoding the supplied reader using the
         * provided charset object.
         */
        public ReaderConfigurator charset(Charset charset) {
            this.charset = charset;
            return this;
        }

        /**
         * Set the charset for decoding the supplied reader using the
         * provided name.
         */
        public ReaderConfigurator charset(String name) {
            charsetName = name;
            return this;
        }

        /**
         * Return the AST computed by tree generator on the provided file.
         * The file is provided using the java Path API.
         *
         * @see Path
         */
        public TreeContext file(Path path) throws IOException {
            return reader(Files.newBufferedReader(path, charset()));
        }

        /**
         * Return the AST computed by tree generator on the provided file.
         * The file is provided using a string containing its path.
         *
         * @see Path
         */
        public TreeContext file(String path) throws IOException {
            return file(Paths.get(path));
        }

        /**
         * Return the AST computed by tree generator on the provided file.
         * The file is provided using the java File API.
         *
         * @see File
         */
        public TreeContext file(File file) throws IOException {
            return file(file.toPath());
        }

        /**
         * Return the AST computed by tree generator on the provided Reader.
         *
         * @see Reader
         */
        public TreeContext reader(Reader stream) throws IOException {
            return generateTree(stream);
        }

        /**
         * Return the AST computed by tree generator on the provided InputStream.
         *
         * @see InputStream
         */
        public TreeContext stream(InputStream stream) throws IOException {
            return reader(new InputStreamReader(stream, charset()));
        }

        /**
         * Return the AST computed by tree generator on the provided String.
         */
        public TreeContext string(String content) throws IOException {
            return reader(new StringReader(content));
        }
    }
}
