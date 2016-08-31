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
 * Copyright 2016 Thomas Durieux
 */

package com.github.gumtreediff.gen.srcml;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class SrcmlVisitor {
    private final List<String> codeToken = Arrays.asList("{", "}", "(", ")", ",", ".", ";");
    private final Reader reader;

    TreeContext treeContext = new TreeContext();
    private Stack<ITree> nodes;
    public ITree root;
    private Document doc;
    private Node currentUnit;

    public SrcmlVisitor(Reader reader) {
        this.reader = reader;
        init();
        visit(doc);
    }

    private static String readerToCharArray(Reader r) throws IOException {
        StringBuilder fileData = new StringBuilder(1000);
        BufferedReader br = new BufferedReader(r);

        char[] buf = new char[10];
        int numRead = 0;
        while ((numRead = br.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        br.close();

        return  fileData.toString();
    }

    // cleans all nodes
    private void init() {
        nodes = new Stack<ITree>();
        root = treeContext.createTree(-1, "", "root");
        getTreeContext().setRoot(root);
        nodes.push(root);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
            this.doc = db.parse(new ByteArrayInputStream(readerToCharArray(reader).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void visit(Node node) {
        if (node.getNodeName().equals("pos:position")) {
            int column = Integer.parseInt(node.getAttributes().getNamedItem("pos:column").getNodeValue());
            int line = Integer.parseInt(node.getAttributes().getNamedItem("pos:line").getNodeValue());
            nodes.peek().setPos(positionFor(line, column));
            nodes.peek().setLength(node.getParentNode().getTextContent().length());
            return;
        }

        ITree iTree = enter(node);
        if (iTree == null) {
            return;
        }
        if ("comment".equals(node.getNodeName())
                || "literal".equals(node.getNodeName())
                || "operator".equals(node.getNodeName())
                || ("name".equals(node.getNodeName())
                && node.getFirstChild().getNodeValue() != null)) {
            nodes.peek().setLabel(node.getFirstChild().getNodeValue().replaceAll("[\n\r \t]", ""));
        } else {
            visitChildren(node);
        }
        exit(node);
    }

    private void visitChildren(Node node) {
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node childNode = list.item(i);
            visit(childNode);
        }
    }

    private ITree enter(Node node) {
        if (node instanceof Document) {
            nodes = new Stack<ITree>();
            root = treeContext.createTree(-1, "", "root");
            getTreeContext().setRoot(root);
            nodes.push(root);
            return root;
        } else if (node.getNodeName().equals("unit")) {
            currentUnit = node;
        }
        ITree iTree = createITree(node);
        if (iTree != null) {
            if (node.hasAttributes()) {
                Node column = node.getAttributes().getNamedItem("pos:column");
                Node line = node.getAttributes().getNamedItem("pos:line");
                if (line != null) {
                    iTree.setPos(positionFor(Integer.parseInt(
                            line.getNodeValue()), Integer.parseInt(column.getNodeValue())));
                    iTree.setLength(node.getTextContent().length());
                }
            }
            addNodeToTree(iTree);
        }
        return iTree;
    }

    private Map<Node, List<Integer>> linesCache = new HashMap<>();

    private List<Integer> getLines() {
        if (!linesCache.containsKey(currentUnit)) {
            List<Integer> lines = new ArrayList<>(Arrays.asList(0));
            String textContent = currentUnit.getTextContent();
            String[] split = textContent.split("\n");

            int currentPos = 0;
            for (int i = 0; i < split.length; i++) {
                String s = split[i];
                lines.add(currentPos);
                currentPos += s.length() + 1;
            }
            linesCache.put(currentUnit, lines);
        }
        return linesCache.get(currentUnit);
    }

    private int positionFor(int line, int column) {
        return getLines().get(line - 1) + column - 1;
    }

    private void exit(Node node) {
        nodes.pop();
    }

    public int resolveTypeId(String typeClass) {
        return typeClass.hashCode();
    }

    private ITree createNode(String label, String typeLabel) {
        int typeId = resolveTypeId(typeLabel);
        ITree node = treeContext.createTree(typeId, label, typeLabel);
        return node;
    }

    private ITree createITree(Node node) {
        String label = node.getNodeName();
        if (node.getNodeValue() != null) {
            label = node.getNodeValue().replaceAll("[\n\r \t]", "");
            if (codeToken.contains(label)) {
                return null;
            }
        }
        if (label.length() == 0) {
            if ("#text".equals(node.getNodeName())) {
                return null;
            }
        }

        String typeLabel = node.getNodeName();
        ITree iTree = createNode(label, typeLabel);
        iTree.setMetadata("srcML", node);
        return iTree;
    }

    private void addNodeToTree(ITree node) {
        ITree parent = nodes.peek();
        if (parent != null) { // happens when nodes.push(null)
            parent.addChild(node);
        }
        nodes.push(node);
    }

    public TreeContext getTreeContext() {
        return treeContext;
    }
}
