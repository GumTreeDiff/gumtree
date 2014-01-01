package fr.labri.gumtree.io;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import fr.labri.gumtree.tree.Tree;
import fr.labri.gumtree.tree.TreeUtils;

public final class TreeIoUtils {

	private TreeIoUtils() {
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
					int type = Integer.parseInt(s.getAttributeByName(new QName("type")).getValue());
					String label = s.getAttributeByName(new QName("label")).getValue();
					String typeLabel = s.getAttributeByName(new QName("typeLabel")).getValue();
					int pos = Integer.parseInt(s.getAttributeByName(new QName("pos")).getValue());
					int length = Integer.parseInt(s.getAttributeByName(new QName("length")).getValue());
					Tree t = new Tree(type, label, typeLabel);
					t.setPos(pos);
					t.setLength(length);
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
	
	public static Tree fromXmlFile(String path) {
		try {
			return fromXml(new FileReader(path));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void toXml(Tree t, String file) {
		try {
			FileWriter f = new FileWriter(file);
			f.append(toXml(t));
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String toXml(Tree t) {
		XMLOutputFactory fact = XMLOutputFactory.newInstance();
		StringWriter s = new StringWriter();
		String result = null;
		try {
			XMLStreamWriter w = fact.createXMLStreamWriter(s);
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

	private static void writeTree(Tree t, XMLStreamWriter w) throws XMLStreamException {
		w.writeStartElement("tree");
		w.writeAttribute("type", Integer.toString(t.getType()));
		w.writeAttribute("label", t.getLabel());
		w.writeAttribute("typeLabel", t.getTypeLabel());
		w.writeAttribute("pos", Integer.toString(t.getPos()));
		w.writeAttribute("length", Integer.toString(t.getLength()));
		for (Tree c: t.getChildren())
			writeTree(c, w);
		w.writeEndElement();
	}
	
	public static void toCompactXml(Tree t, String file) {
		try {
			FileWriter f = new FileWriter(file);
			f.append(toCompactXml(t));
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String toCompactXml(Tree t) {
		XMLOutputFactory fact = XMLOutputFactory.newInstance();
		StringWriter s = new StringWriter();
		String result = null;
		try {
			XMLStreamWriter w = fact.createXMLStreamWriter(s);
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
	
	private static void writeCompactTree(Tree t, XMLStreamWriter w) throws XMLStreamException {
		w.writeStartElement(t.getTypeLabel());
		if (!"".equals(t.getLabel())) w.writeAttribute("label", t.getLabel());
		for (Tree c: t.getChildren())
			writeCompactTree(c, w);
		w.writeEndElement();
	}
	
	public static String toDot(Tree root) {
		StringBuffer b = new StringBuffer();
		TreeUtils.preOrderNumbering(root);
		b.append("digraph G {\n");
		for (Tree t : root.getTrees()) {
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

		for (Tree t : root.getTrees())
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

}
