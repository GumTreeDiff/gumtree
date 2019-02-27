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

package com.github.gumtreediff.gen.python;

import com.github.gumtreediff.gen.ExternalProcessTreeGenerator;
import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.Registry;
import com.github.gumtreediff.io.LineReader;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.*;
import java.io.*;
import java.util.*;

@Register(id = "python-pythonparser", accept = {"\\.py$"}, priority = Registry.Priority.MAXIMUM)
public class PythonTreeGenerator extends ExternalProcessTreeGenerator {

    private static final String PYTHONPARSER_CMD = System.getProperty("gt.pp.path", "pythonparser");

    private static final QName VALUE = new QName("value");

    private static final QName LINENO = new QName("lineno");

    private static final QName COL = new QName("col");

    private static final QName END_LINENO = new QName("end_line_no");

    private static final QName END_COL = new QName("end_col");

    private LineReader lr;

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
        try {
            ArrayDeque<ITree> trees = new ArrayDeque<>();
            XMLEventReader r = fact.createXMLEventReader(new StringReader(xml));
            while (r.hasNext()) {
                XMLEvent ev = r.nextEvent();
                if (ev.isStartElement()) {
                    StartElement s = ev.asStartElement();
                    String typeLabel = s.getName().getLocalPart();
                    String label = "";
                    if (s.getAttributeByName(VALUE) != null)
                        label = s.getAttributeByName(VALUE).getValue();
                    int type = typeLabel.hashCode();
                    ITree t = context.createTree(type, label, typeLabel);
                    if (trees.isEmpty()) {
                        context.setRoot(t);
                    } else {
                        t.setParentAndUpdateChildren(trees.peekFirst());
                    }
                    setPos(t, s);
                    trees.addFirst(t);
                } else if (ev.isEndElement())
                    trees.removeFirst();
            }
            context.validate();
            return context;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void setPos(ITree t, StartElement e) {
        if (e.getAttributeByName(LINENO) == null) { //FIXME some nodes have start position
            System.out.println(t.getLabel());
            return;
        }
        int line = Integer.parseInt(e.getAttributeByName(LINENO).getValue());
        int column = Integer.parseInt(e.getAttributeByName(COL).getValue());
        t.setPos(lr.positionFor(line, column) + 2);
        if (e.getAttributeByName(END_LINENO) == null) { //FIXME some nodes have no end position
            System.out.println(t.getLabel());
            return;
        }
        int endLine = Integer.parseInt(e.getAttributeByName(END_LINENO).getValue());
        int endColumn = Integer.parseInt(e.getAttributeByName(END_COL).getValue());
        t.setLength(lr.positionFor(endLine, endColumn) - lr.positionFor(line, column));
    }

    public String[] getCommandLine(String file) {
        return new String[]{PYTHONPARSER_CMD, file};
    }
}
