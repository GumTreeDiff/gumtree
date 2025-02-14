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
 * Copyright 2025 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.gen.json;

import com.fasterxml.jackson.core.*;
import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.utils.Registry;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;

import java.io.*;
import java.util.Stack;

import static com.github.gumtreediff.tree.TypeSet.type;

@Register(id = "json-jackson", accept = {"\\.json"}, priority = Registry.Priority.MAXIMUM)
public class JsonTreeGenerator extends TreeGenerator {
    private Stack<Tree> trees;
    private TreeContext ctx;

    @Override
    public TreeContext generate(Reader r) throws IOException {
        ctx = new TreeContext();
        trees = new Stack<>();
        JsonFactory factory = JsonFactory.builder().build();
        JsonParser p = factory.createParser(r);
        try {
            JsonToken tk = p.nextToken();
            while (tk != null) {
                consumeToken(tk, p, ctx);
                tk = p.nextToken();
            }
        }
        catch (Exception e) {
            throw new SyntaxException(this, r, e);
        }

        p.close();

        return ctx;
    }

    public void consumeToken(JsonToken tk, JsonParser p, TreeContext ctx) throws IOException {
        JsonLocation loc = p.currentTokenLocation();
        if (tk.id() == JsonTokenId.ID_START_OBJECT) {
            Tree tree = ctx.createTree(type("Object"));
            if (!trees.empty())
                tree.setParentAndUpdateChildren(trees.peek());
            else
                ctx.setRoot(tree);
            trees.push(tree);
            tree.setPos((int) loc.getCharOffset());
        }
        else if (tk.id() == JsonTokenId.ID_END_OBJECT) {
            Tree tree = trees.pop();
            tree.setLength((int) loc.getCharOffset() - tree.getPos());
        }
        else if (tk.id() == JsonTokenId.ID_START_ARRAY) {
            Tree tree = ctx.createTree(type("Array"));
            if (!trees.empty())
                tree.setParentAndUpdateChildren(trees.peek());
            else
                ctx.setRoot(tree);
            trees.push(tree);
            tree.setPos((int) loc.getCharOffset());
        }
        else if (tk.id() == JsonTokenId.ID_END_ARRAY) {
            Tree tree = trees.pop();
            tree.setLength((int) loc.getCharOffset() - tree.getPos());
        }
        else if (tk.id() == JsonTokenId.ID_FIELD_NAME) {
            Tree synth = ctx.createTree(type("FieldDefinition"));
            synth.setParentAndUpdateChildren(trees.peek());
            synth.setPos((int) loc.getCharOffset());
            trees.push(synth);

            Tree tree = ctx.createTree(type("Field"), p.currentName());
            tree.setParentAndUpdateChildren(synth);
            tree.setPos((int) loc.getCharOffset());
            tree.setLength(p.currentName().length());
        }
        else { // here the token is a value
            Tree tree = ctx.createTree(type("Value"), p.getValueAsString());
            tree.setParentAndUpdateChildren(trees.peek());
            tree.setPos((int) loc.getCharOffset());
            tree.setLength( p.getValueAsString().length());

            if (trees.peek().getType().equals(type("FieldDefinition"))) {
                Tree synth = trees.pop();
                synth.setLength(tree.getEndPos() - synth.getPos());
            }
        }
    }
}
