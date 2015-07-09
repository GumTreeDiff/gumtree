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

package com.github.gumtreediff.gen.sax;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.io.LineReader;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

import static com.github.gumtreediff.tree.ITree.NO_LABEL;

@Register(id = "xml-sax", accept = {"\\.xml$", "\\.xsd$", "\\.wadl$"})
public class SaxTreeGenerator extends TreeGenerator {
    public static final String DOCUMENT = "Document";
    public static final String ATTR = "Attr";
    public static final String CDATA = "CData";
    public static final String ELT = "Elt";
    public static final String VALUE = "Value";

    public static final int CDATA_ID = 3;
    public static final int DOCUMENT_ID = 0;
    public static final int ATTR_ID = 2;
    public static final int ELT_ID = 1;
    public static final int VALUE_ID = 4;

    public TreeContext generate(Reader reader) throws IOException {
        try {
//			TreeContext tc = new TreeContext();
            XMLReader xr = XMLReaderFactory.createXMLReader();
            LineReader lr = new LineReader(reader);
            XmlHandlers hdl = new XmlHandlers(lr);

            xr.setContentHandler(hdl);
            xr.setErrorHandler(hdl);
            xr.parse(new InputSource(lr));
            return hdl.tc;
        } catch (SAXException e) {
            e.printStackTrace();
        } finally {
            // close resources
        }
        return null;
    }

    class XmlHandlers extends DefaultHandler {
        public int[] lastPosition = {1, 1};

        Locator locator;
        Deque<ITree> stack = new ArrayDeque<ITree>();
        TreeContext tc = new TreeContext();
        Map<String, Integer> names = new HashMap<>();
        LineReader lineReader;

        public XmlHandlers(LineReader lr) {
            lineReader = lr;
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        @Override
        public void startDocument() throws SAXException {
            debug("startdoc");

            ITree t = tc.createTree(DOCUMENT_ID, NO_LABEL, DOCUMENT);
            t.setPos(0);
//            t.setLcPosStart(lastPosition);
            tc.setRoot(t);
            stack.push(t);
        }

        public void processingInstruction(String target, String data) {
            System.out.println(target + " " + data);
        }

        @Override
        public void endDocument() throws SAXException {
            debug("enddoc");
            ITree t = stack.pop();
            int line = locator.getLineNumber();
            int col = locator.getColumnNumber();
//            t.setLcPosEnd(new int[]{line, col});
            t.setLength(lineReader.positionFor(line, col));
            assert stack.isEmpty();
        }

        @Override
        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) throws SAXException {
            debug("startElt", qName);
            ITree t = tc.createTree(ELT_ID, qName, ELT);
            addAttributes(t, attributes);
            setStartPosition(t);
            ITree p = stack.peek();
            p.addChild(t);
            stack.push(t);
        }

        void debug(Object... o) {
            System.out.println("lc: " + locator.getLineNumber() + ":" + locator.getColumnNumber());
            System.out.println("last: " + Arrays.toString(lastPosition));
            System.out.println("stack: " + stack);
            if (o.length > 0)
                System.out.println("=> " + Arrays.toString(o));
        }

        private void setStartPosition(ITree t) {
            int[] pos = currentPosition();
//            t.setLcPosStart(pos);
            t.setPos(lineReader.positionFor(pos[0], pos[1]));
        }

        private void setEndPosition(ITree t) {
            int[] pos = currentPosition();
//            t.setLcPosEnd(new int[]{locator.getLineNumber(), locator.getColumnNumber()}); //FIXME
            t.setPos(lineReader.positionFor(locator.getLineNumber(), locator.getColumnNumber()) - t.getPos());
        }

        private int[] currentPosition() {
            int[] lp = lastPosition;
            lastPosition = new int[]{locator.getLineNumber(), locator.getColumnNumber()};
            return lp;
        }

        private void addAttributes(ITree tree, Attributes attrs) {
            int len = attrs.getLength();
            for (int i = 0; i < len; i++) {
                ITree at = tc.createTree(ATTR_ID, attrs.getQName(i), ATTR);
                at.addChild(tc.createTree(VALUE_ID, attrs.getValue(i), VALUE));
                tree.addChild(at);
                setStartPosition(at);
                setEndPosition(at); // FIXME grab next
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            debug("endelt", localName);

            ITree t = stack.pop();
            setEndPosition(t);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            debug("char", start, length);
            tc.createTree(CDATA_ID, new String(ch, start, length), CDATA);
        }
    }
}
