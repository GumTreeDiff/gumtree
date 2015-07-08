package com.github.gumtreediff.gen;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.Reader;
import java.util.Stack;

@Register(id = "xml", accept = "\\.gxml$")
public class XMLInternalGenerator extends TreeGenerator {

    private static final QName TYPE = new QName("type");
    private static final QName LABEL = new QName("label");
    private static final QName TYPE_LABEL = new QName("typeLabel");
    private static final QName POS = new QName("pos");
    private static final QName LENGTH = new QName("length");
    private static final QName LINE_BEFORE = new QName("line_before");
    private static final QName LINE_AFTER = new QName("line_after");
    private static final QName COL_BEFORE = new QName("col_before");
    private static final QName COL_AFTER = new QName("col_after");

    @Override
    protected TreeContext generate(Reader source) throws IOException {
        XMLInputFactory fact = XMLInputFactory.newInstance();
        TreeContext context = new TreeContext();
        try {
            Stack<ITree> trees = new Stack<>();
            XMLEventReader r = fact.createXMLEventReader(source);
            while (r.hasNext()) {
                XMLEvent e = r.nextEvent();
                if (e instanceof StartElement) {
                    StartElement s = (StartElement) e;
                    if (!s.getName().getLocalPart().equals("tree")) // FIXME need to deal with options
                        continue;
                    int type = Integer.parseInt(s.getAttributeByName(TYPE).getValue());

                    ITree t = context.createTree(type, labelForAttribute(s, LABEL), labelForAttribute(s, TYPE_LABEL));


                    if (s.getAttributeByName(POS) != null) {
                        int pos = numberForAttribute(s, POS);
                        int length = numberForAttribute(s, LENGTH);
                        t.setPos(pos);
                        t.setLength(length);
                    }

                    if (trees.isEmpty())
                        context.setRoot(t);
                    else
                        t.setParentAndUpdateChildren(trees.peek());
                    trees.push(t);
                } else if (e instanceof EndElement) {
                    if (!((EndElement)e).getName().getLocalPart().equals("tree")) // FIXME need to deal with options
                        continue;
                    trees.pop();
                }
            }
            context.validate();
            return context;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String labelForAttribute(StartElement s, QName attrName) {
        Attribute attr = s.getAttributeByName(attrName);
        return attr == null ? ITree.NO_LABEL : attr.getValue();
    }

    private static int numberForAttribute(StartElement s, QName attrName) {
        return Integer.parseInt(s.getAttributeByName(attrName).getValue());
    }
}
