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

import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.io.LineReader;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public abstract class AbstractSrcmlTreeGenerator extends TreeGenerator {

    private static final String SRCML_CMD = System.getProperty("gt.srcml.path", "srcml");

    private static final QName LINE = new  QName("http://www.srcML.org/srcML/position", "line", "pos");

    private static final QName COLUMN = new  QName("http://www.srcML.org/srcML/position", "column", "pos");

    private LineReader lr;

    private Set<String> labeled = new HashSet<String>(
            Arrays.asList("specifier", "name", "comment", "literal", "operator"));

    private StringBuilder currentLabel;

    private TreeContext context;

    @Override
    public TreeContext generate(Reader r) throws IOException {
        lr = new LineReader(r);
        String xml = getXml(lr);
        return getTreeContext(xml);
    }

    public TreeContext getTreeContext(String xml) {
        XMLInputFactory fact = XMLInputFactory.newInstance();
        context = new TreeContext();
        currentLabel = new StringBuilder();
        try {
            ArrayDeque<ITree> trees = new ArrayDeque<>();
            XMLEventReader r = fact.createXMLEventReader(new StringReader(xml));
            while (r.hasNext()) {
                XMLEvent ev = r.nextEvent();
                if (ev.isStartElement()) {
                    StartElement s = ev.asStartElement();
                    String typeLabel = s.getName().getLocalPart();
                    if (typeLabel.equals("position"))
                        setLength(trees.peekFirst(), s);
                    else {
                        int type = typeLabel.hashCode();
                        ITree t = context.createTree(type, "", typeLabel);

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
                    if (!end.getName().getLocalPart().equals("position")) {
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
            context.validate();
            return context;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isLabeled(ArrayDeque<ITree> trees) {
        return labeled.contains(context.getTypeLabel(trees.peekFirst().getType()));
    }

    private void fixPos(TreeContext ctx) {
        for (ITree t : ctx.getRoot().postOrder()) {
            if (!t.isLeaf()) {
                if (t.getPos() == ITree.NO_VALUE || t.getLength() == ITree.NO_VALUE) {
                    ITree firstChild = t.getChild(0);
                    t.setPos(firstChild.getPos());
                    if (t.getChildren().size() == 1)
                        t.setLength(firstChild.getLength());
                    else {
                        ITree lastChild = t.getChild(t.getChildren().size() - 1);
                        t.setLength(lastChild.getEndPos() - firstChild.getPos());
                    }
                }
            }
        }
    }

    private void setPos(ITree t, StartElement e) {
        if (e.getAttributeByName(LINE) != null) {
            int line = Integer.parseInt(e.getAttributeByName(LINE).getValue());
            int column = Integer.parseInt(e.getAttributeByName(COLUMN).getValue());
            t.setPos(lr.positionFor(line, column));
        }
    }

    private void setLength(ITree t, StartElement e) {
        if (t.getPos() == -1)
            return;
        if (e.getAttributeByName(LINE) != null) {
            int line = Integer.parseInt(e.getAttributeByName(LINE).getValue());
            int column = Integer.parseInt(e.getAttributeByName(COLUMN).getValue());
            t.setLength(lr.positionFor(line, column) - t.getPos() + 1);
        }
    }

    public String getXml(Reader r) throws IOException {
        //FIXME this is not efficient but I am not sure how to speed up things here.
        File f = File.createTempFile("gumtree", "");
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
        ProcessBuilder b = new ProcessBuilder(getArguments(f.getAbsolutePath()));
        b.directory(f.getParentFile());
        Process p = b.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));) {
            StringBuilder buf = new StringBuilder();
            // TODO Why do we need to read and bufferize everything, when we could/should only use generateFromStream
            String line = null;
            while ((line = br.readLine()) != null)
                buf.append(line + System.lineSeparator());
            p.waitFor();
            if (p.exitValue() != 0) throw new RuntimeException(buf.toString());
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
        return new String[]{SRCML_CMD, "-l", getLanguage(), "--position", file, "--tabs=1"};
    }
}
