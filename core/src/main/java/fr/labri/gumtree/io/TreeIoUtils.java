package fr.labri.gumtree.io;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Stack;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import com.google.gson.stream.JsonWriter;

import fr.labri.gumtree.matchers.MappingStore;
import fr.labri.gumtree.tree.ITree;
import fr.labri.gumtree.tree.Tree;
import fr.labri.gumtree.tree.TreeUtils;

public final class TreeIoUtils {
	
	private final static QName TYPE = new QName("type");
	private final static QName LABEL = new QName("label");
	private final static QName TYPE_LABEL = new QName("typeLabel");
	private final static QName POS = new QName("pos");
	private final static QName LENGTH = new QName("length");
	private final static QName LINE_BEFORE = new QName("line_before");
	private final static QName LINE_AFTER = new QName("line_after");
	private final static QName COL_BEFORE = new QName("col_before");
	private final static QName COL_AFTER = new QName("col_after");

	private TreeIoUtils() {
	}
	
	public static Tree fromXml(InputStream iStream) {
		return fromXml(new InputStreamReader(iStream));
	}
	
	public static Tree fromXml(Reader source) {
		XMLInputFactory fact = XMLInputFactory.newInstance();
		try {
			Tree root = null;
			Stack<Tree> trees = new Stack<Tree>();
			XMLEventReader r = fact.createXMLEventReader(source);
			while (r.hasNext()) {
				XMLEvent e = r.nextEvent();
				if (e instanceof StartElement) {
					StartElement s = (StartElement) e;
					int type = Integer.parseInt(s.getAttributeByName(TYPE).getValue());
					
					Tree t = new Tree(type, labelForAttribute(s, LABEL), labelForAttribute(s, TYPE_LABEL));
					
					
					if (s.getAttributeByName(POS) != null) {
						int pos = numberForAttribute(s, POS);
						int length = numberForAttribute(s, LENGTH);
						t.setPos(pos);
						t.setLength(length);
					}
					
					if (s.getAttributeByName(LINE_BEFORE) != null) {
						int l0 = numberForAttribute(s, LINE_BEFORE);
						int c0 = numberForAttribute(s, COL_BEFORE);
						int l1 = numberForAttribute(s, LINE_AFTER);
						int c1 = numberForAttribute(s, COL_AFTER);
						t.setLcPosStart(new int[] {l0, c0});
						t.setLcPosEnd(new int[] {l1, c1});
					}
					
					if (root == null) root = t;
					else {
						Tree parent = trees.peek();
						t.setParentAndUpdateChildren(parent);
					}
					trees.push(t);
				} else if (e instanceof EndElement) trees.pop();
			}
			root.refresh();
			TreeUtils.postOrderNumbering(root);
			return root;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static Tree fromXmlString(String xml) {
		return fromXml(new StringReader(xml));
	}
	
	public static ITree fromXmlFile(String path) {
		try {
			return fromXml(new FileReader(path));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void toXml(ITree t, String file) {
		try {
			FileWriter f = new FileWriter(file);
			f.append(toXml(t));
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String toXml(ITree t) {
		XMLOutputFactory f = XMLOutputFactory.newInstance();
		StringWriter s = new StringWriter();
		String result = null;
		try {
			XMLStreamWriter w = new IndentingXMLStreamWriter(f.createXMLStreamWriter(s));
			w.writeStartDocument();
			writeTree(t, w);
			w.writeEndDocument();
			w.close();
			result = s.toString();
			s.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public static String toAnnotatedXml(ITree t, MappingStore m, boolean isSrc) {
		XMLOutputFactory f = XMLOutputFactory.newInstance();
		StringWriter s = new StringWriter();
		String result = null;
		try {
			XMLStreamWriter w = new IndentingXMLStreamWriter(f.createXMLStreamWriter(s));
			w.writeStartDocument();
			writeTree(t, m, isSrc, w);
			w.writeEndDocument();
			w.close();
			result = s.toString();
			s.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private static void writeTree(ITree t, XMLStreamWriter w) throws XMLStreamException {
		w.writeStartElement("tree");
		w.writeAttribute("type", Integer.toString(t.getType()));
		if (!ITree.NO_LABEL.equals(t.getLabel())) w.writeAttribute("label", t.getLabel());
		if (!ITree.NO_LABEL.equals(t.getTypeLabel())) w.writeAttribute("typeLabel", t.getTypeLabel());
		if (ITree.NO_VALUE != t.getPos()) {
			w.writeAttribute("pos", Integer.toString(t.getPos()));
			w.writeAttribute("length", Integer.toString(t.getLength()));
		}
		if (t.getLcPosStart() != null) {
			w.writeAttribute("line_before", Integer.toString(t.getLcPosStart()[0]));
			w.writeAttribute("col_before", Integer.toString(t.getLcPosStart()[1]));
			w.writeAttribute("line_after", Integer.toString(t.getLcPosEnd()[0]));
			w.writeAttribute("col_after", Integer.toString(t.getLcPosEnd()[1]));
		}
		for (ITree c: t.getChildren())
			writeTree(c, w);
		w.writeEndElement();
	}
	
	private static void writeTree(ITree t, MappingStore m, boolean isSrc, XMLStreamWriter w) throws XMLStreamException {
		w.writeStartElement("tree");
		w.writeAttribute("type", Integer.toString(t.getType()));
		if (!ITree.NO_LABEL.equals(t.getLabel())) w.writeAttribute("label", t.getLabel());
		if (!ITree.NO_LABEL.equals(t.getTypeLabel())) w.writeAttribute("typeLabel", t.getTypeLabel());
		if (ITree.NO_VALUE != t.getPos()) {
			w.writeAttribute("pos", Integer.toString(t.getPos()));
			w.writeAttribute("length", Integer.toString(t.getLength()));
		}
		if (t.getLcPosStart() != null) {
			w.writeAttribute("line_before", Integer.toString(t.getLcPosStart()[0]));
			w.writeAttribute("col_before", Integer.toString(t.getLcPosStart()[1]));
			w.writeAttribute("line_after", Integer.toString(t.getLcPosEnd()[0]));
			w.writeAttribute("col_after", Integer.toString(t.getLcPosEnd()[1]));
		}
		ITree o = null;
		if (isSrc && m.hasSrc(t)) o = m.getDst(t);
		if (!isSrc && m.hasDst(t)) o = m.getSrc(t);
		
		if (o != null) {
			if (ITree.NO_VALUE != o.getPos()) {
				w.writeAttribute("other_pos", Integer.toString(o.getPos()));
				w.writeAttribute("other_length", Integer.toString(o.getLength()));
			}
			if (o.getLcPosStart() != null) {
				w.writeAttribute("other_line_before", Integer.toString(o.getLcPosStart()[0]));
				w.writeAttribute("other_col_before", Integer.toString(o.getLcPosStart()[1]));
				w.writeAttribute("other_line_after", Integer.toString(o.getLcPosEnd()[0]));
				w.writeAttribute("other_col_after", Integer.toString(o.getLcPosEnd()[1]));
			}
		}
		
		for (ITree c: t.getChildren())
			writeTree(c, m, isSrc, w);
		w.writeEndElement();
	}
	
	public static void toCompactXml(ITree t, String file) {
		try {
			FileWriter f = new FileWriter(file);
			f.append(toCompactXml(t));
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String toCompactXml(ITree t) {
		XMLOutputFactory f = XMLOutputFactory.newInstance();
		StringWriter s = new StringWriter();
		String result = null;
		try {
			XMLStreamWriter w = new IndentingXMLStreamWriter(f.createXMLStreamWriter(s));
			w.writeStartDocument();
			writeCompactTree(t, w);
			w.writeEndDocument();
			w.close();
			result = s.toString();
			s.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	private static void writeCompactTree(ITree t, XMLStreamWriter w) throws XMLStreamException {
		w.writeStartElement(t.getTypeLabel());
		if (!"".equals(t.getLabel())) w.writeAttribute("label", t.getLabel());
		for (ITree c: t.getChildren())
			writeCompactTree(c, w);
		w.writeEndElement();
	}
	
	public static String toDot(Tree root) {
		StringBuffer b = new StringBuffer();
		TreeUtils.preOrderNumbering(root);
		b.append("digraph G {\n");
		for (ITree t : root.getTrees()) {
			String label = t.toString();
			if (label.contains("\"") || label.contains("\\s"))
				label = label.replaceAll("\"", "").replaceAll("\\s", "").replaceAll("\\\\", "");
			if (label.length() > 30)
				label = label.substring(0, 30);
			b.append(t.getId() + " [label=\"" + label + "\"");
			if (t.isMatched())
				b.append(",style=filled,fillcolor=cadetblue1");
			b.append("];\n");
		}

		for (ITree t : root.getTrees())
			if (t.getParent() != null)
				b.append(t.getParent().getId() + " -> " + t.getId() + ";\n");
		b.append("}");
		return b.toString();
	}
	
	public static void toDot(Tree root, String path) {
		try {
			FileWriter fw = new FileWriter(path);
			fw.append(toDot(root));
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void toJSON(ITree t, String file) {
		try {
			FileWriter f = new FileWriter(file);
			f.append(toJSON(t));
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String toJSON(ITree t) {
		StringWriter s = new StringWriter();
		String result = null;
		try {
			JsonWriter w = new JsonWriter(s);
			w.setIndent("\t");			
			writeJSONTree(t, w);
			w.close();
			
			result = s.toString();
			s.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private static void writeJSONTree(ITree t, JsonWriter w) throws IOException {
		w.beginObject();
		
		w.name("type").value(Integer.toString(t.getType()));

		if (!ITree.NO_LABEL.equals(t.getLabel())) w.name("label").value(t.getLabel());
		if (!ITree.NO_LABEL.equals(t.getTypeLabel())) w.name("typeLabel").value(t.getTypeLabel());
		
		if (ITree.NO_VALUE != t.getPos()) {
			w.name("pos").value(Integer.toString(t.getPos()));
			w.name("length").value(Integer.toString(t.getLength()));
		}
		
		if (t.getLcPosStart() != null) {
			w.name("line_before").value(Integer.toString(t.getLcPosStart()[0]));
			w.name("col_before").value(Integer.toString(t.getLcPosStart()[1]));
			w.name("line_after").value(Integer.toString(t.getLcPosEnd()[0]));
			w.name("col_after").value(Integer.toString(t.getLcPosEnd()[1]));
		}
		
		w.name("children");
		w.beginArray();
		for (ITree c: t.getChildren())
			writeJSONTree(c, w);
		w.endArray();
		
		w.endObject();
	}

	static String labelForAttribute(StartElement s, QName attrName) {
		Attribute attr = s.getAttributeByName(attrName);
		return attr == null ? ITree.NO_LABEL : attr.getValue();
	}

	static int numberForAttribute(StartElement s, QName attrName) {
		return Integer.parseInt(s.getAttributeByName(attrName).getValue());
	}
}
