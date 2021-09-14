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

import com.github.gumtreediff.gen.ExternalProcessTreeGenerator;
import com.github.gumtreediff.io.LineReader;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.Type;
import com.github.gumtreediff.tree.TreeContext;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.*;
import java.io.*;
import java.util.*;

import static com.github.gumtreediff.tree.TypeSet.type;

public abstract class AbstractSrcmlTreeGenerator extends ExternalProcessTreeGenerator {
    private static final String SRCML_CMD = System.getProperty("gt.srcml.path", "srcml");

    private static final QName POS_START = new  QName("http://www.srcML.org/srcML/position", "start", "pos");

    private static final QName POS_END = new  QName("http://www.srcML.org/srcML/position", "end", "pos");

    private LineReader lr;

    private Set<Type> labeled = new HashSet<>(
            Arrays.asList(
                    type("specifier"),
                    type("name"),
                    type("comment"),
                    type("literal"),
                    type("operator"),
                    type("file"),
                    type("directive"),
                    type("modifier")
            ));

    Type position = type("position");

    private StringBuilder currentLabel;

    private TreeContext context;

    @Override
    public TreeContext generate(Reader r) throws IOException {
        lr = new LineReader(r);
        String output = readStandardOutput(lr);
        return getTreeContext(output);
    }

    public TreeContext getTreeContext(String xml) {
        XMLInputFactory fact = XMLInputFactory.newInstance();
        context = new TreeContext();
        currentLabel = new StringBuilder();
        try {
            ArrayDeque<Tree> trees = new ArrayDeque<>();
            XMLEventReader r = fact.createXMLEventReader(new StringReader(xml));
            while (r.hasNext()) {
                XMLEvent ev = r.nextEvent();
                if (ev.isStartElement()) {
                    StartElement s = ev.asStartElement();
                    Type type = type(s.getName().getLocalPart());
                    if (type.equals(position))
                        setLength(trees.peekFirst(), s);
                    else {
                        Tree t = context.createTree(type, "");

                        if (trees.isEmpty()) {
                            context.setRoot(t);
                            t.setPos(0);
                        } else {
                            t.setParentAndUpdateChildren(trees.peekFirst());
                            setPos(t, s);
                        }
                        trees.addFirst(t);
                    }
                } else if (ev.isEndElement()) {
                    EndElement end = ev.asEndElement();
                    if (type(end.getName().getLocalPart()) != position) {
                        if (isLabeled(trees))
                            trees.peekFirst().setLabel(currentLabel.toString());
                        trees.removeFirst();
                        currentLabel = new StringBuilder();
                    }
                } else if (ev.isCharacters()) {
                    Characters chars = ev.asCharacters();
                    if (!chars.isWhiteSpace() && isLabeled(trees))
                        currentLabel.append(chars.getData().trim());
                }
            }
            fixPos(context);
            return context;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isLabeled(ArrayDeque<Tree> trees) {
        return labeled.contains(trees.peekFirst().getType());
    }

    private void fixPos(TreeContext ctx) {
        for (Tree t : ctx.getRoot().postOrder()) {
            if (!t.isLeaf()) {
                if (t.getPos() == Tree.NO_POS || t.getLength() == Tree.NO_POS) {
                    Tree firstChild = t.getChild(0);
                    t.setPos(firstChild.getPos());
                    if (t.getChildren().size() == 1)
                        t.setLength(firstChild.getLength());
                    else {
                        Tree lastChild = t.getChild(t.getChildren().size() - 1);
                        t.setLength(lastChild.getEndPos() - firstChild.getPos());
                    }
                }
            }
        }
    }

    private void setPos(Tree t, StartElement e) {
        if (e.getAttributeByName(POS_START) != null) {
            String posStr = e.getAttributeByName(POS_START).getValue();
            String[] chunks = posStr.split(":");
            int line = Integer.parseInt(chunks[0]);
            int column = Integer.parseInt(chunks[1]);
            t.setPos(lr.positionFor(line, column));
            setLength(t, e);
        }
    }

    private void setLength(Tree t, StartElement e) {
        if (t.getPos() == -1)
            return;
        if ( e.getAttributeByName(POS_END) != null) {
            String posStr = e.getAttributeByName(POS_END).getValue();
            String[] chunks = posStr.split(":");
            int line = Integer.parseInt(chunks[0]);
            int column = Integer.parseInt(chunks[1]);
            t.setLength(lr.positionFor(line, column) - t.getPos() + 1);
        }
    }

    public abstract String getLanguage();

    public String[] getCommandLine(String file) {
        return new String[]{SRCML_CMD, "-l", getLanguage(), "--position", file, "--tabs=1"};
    }
}
