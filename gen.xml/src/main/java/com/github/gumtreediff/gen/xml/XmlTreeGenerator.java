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
 * Copyright 2023 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.gen.xml;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.utils.Registry;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.Parser;
import org.jsoup.select.NodeVisitor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Stack;

import static com.github.gumtreediff.tree.TypeSet.type;

@Register(id = "xml-jsoup", accept = {"\\.xml$"}, priority = Registry.Priority.MAXIMUM)
public class XmlTreeGenerator extends TreeGenerator {
    private Stack<Tree> trees;
    private TreeContext ctx;

    @Override
    public TreeContext generate(Reader r) throws IOException {
        ctx = new TreeContext();
        trees = new Stack<>();
        StringBuilder builder = new StringBuilder();

        try (Reader reader = new BufferedReader(r)) {
            char[] charBuffer = new char[8 * 1024];
            int numCharsRead;
            while ((numCharsRead = reader.read(charBuffer, 0,
                    charBuffer.length)) != -1) {
                builder.append(charBuffer, 0, numCharsRead);
            }
        }

        try (InputStream inputStream = new ByteArrayInputStream(
                builder.toString().getBytes(StandardCharsets.UTF_8))) {
            Parser parser = Parser.xmlParser();
            parser.setTrackPosition(true);
            Document doc = Jsoup.parse(inputStream, StandardCharsets.UTF_8.name(), "", parser);
            doc.traverse(new GumtreeNodeVisitor());
        }

        ctx.setRoot(ctx.getRoot().getChild(0));

        return ctx;
    }

    private class GumtreeNodeVisitor implements NodeVisitor {
        @Override
        public void head(Node node, int depth) {
            Tree tree = null;
            if (node instanceof Element)
                tree = asTree((Element) node);
            else if (node instanceof DataNode)
                tree = asTree((DataNode) node);
            else if (node instanceof TextNode)
                tree = asTree((TextNode) node);

            if (tree != null)
                insertTree(tree);
        }

        private void insertTree(Tree tree) {
            if (!trees.isEmpty()) {
                if (!dummyTextNode(tree))
                    trees.peek().addChild(tree);
            }
            else
                ctx.setRoot(tree);

            trees.push(tree);
        }

        private boolean dummyTextNode(Tree tree) {
            return tree.getType() == type("xml-text")
                    && tree.getLabel().trim().isEmpty();
        }

        @Override
        public void tail(Node node, int depth) {
            if (node instanceof Element  || node instanceof DataNode || node instanceof TextNode)
                trees.pop();
        }

        private Tree asTree(Element element) {
            Tree tree = ctx.createTree(type(element.tagName()));
            int startPos = element.sourceRange().startPos();
            int length = element.endSourceRange().endPos() - startPos;
            tree.setPos(startPos);
            tree.setLength(length);
            for (Attribute attrXml : element.attributes()) {
                Tree attrTree = ctx.createTree(type(attrXml.getKey()), attrXml.getValue());
                startPos = attrXml.sourceRange().nameRange().startPos();
                length = attrXml.sourceRange().valueRange().endPos() - startPos;
                attrTree.setPos(startPos);
                attrTree.setLength(length);
                tree.addChild(attrTree);
            }
            return tree;
        }

        private Tree asTree(DataNode dataNode) {
            Tree tree = ctx.createTree(type("xml-data"));
            int startPos = dataNode.sourceRange().startPos();
            int length = dataNode.sourceRange().endPos() - startPos;
            tree.setPos(startPos);
            tree.setLength(length);
            return tree;
        }

        private Tree asTree(TextNode textXml) {
            Tree tree = ctx.createTree(type("xml-text"), textXml.text());
            int startPos = textXml.sourceRange().startPos();
            int length = textXml.sourceRange().endPos() - startPos;
            tree.setPos(startPos);
            tree.setLength(length);
            return tree;
        }
    }
}
