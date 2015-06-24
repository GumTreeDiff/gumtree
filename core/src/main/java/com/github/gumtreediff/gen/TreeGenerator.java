package com.github.gumtreediff.gen;

import com.github.gumtreediff.tree.TreeContext;

import java.io.*;

public abstract class TreeGenerator {

    protected abstract TreeContext generate(Reader r) throws IOException;

    public TreeContext generateFromReader(Reader r) throws IOException {
        TreeContext ctx = generate(r);
        ctx.validate();
        return ctx;
    }

    public TreeContext generateFromFile(String path) throws IOException {
        return generateFromReader(new FileReader(path));
    }

    public TreeContext generateFromFile(File file) throws IOException {
        return generateFromReader(new FileReader(file));
    }

    public TreeContext generateFromString(String content) throws IOException {
        return generateFromReader(new StringReader(content));
    }
}
