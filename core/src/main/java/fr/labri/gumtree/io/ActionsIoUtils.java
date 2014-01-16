package fr.labri.gumtree.io;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import fr.labri.gumtree.actions.model.Action;
import fr.labri.gumtree.actions.model.Delete;
import fr.labri.gumtree.actions.model.Insert;
import fr.labri.gumtree.actions.model.Move;
import fr.labri.gumtree.actions.model.Update;
import fr.labri.gumtree.matchers.MappingStore;
import fr.labri.gumtree.tree.Tree;

public final class ActionsIoUtils {

	private ActionsIoUtils() {
	}

	public static String toText(List<Action> script) {
		StringWriter w = new StringWriter();
		for (Action a: script) w.append(a.toString() + "\n");
		String result = w.toString();
		try {
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	public static void toText(List<Action> script, String file) {
		try {
			FileWriter w = new FileWriter(file);
			for (Action a : script) w.append(a.toString() + "\n");
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void toXml(List<Action> actions, MappingStore mappings, String file) {
		try {
			FileWriter f = new FileWriter(file);
			f.append(toXml(actions, mappings));
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String toXml(List<Action> actions, MappingStore mappings) {
		XMLOutputFactory fact = XMLOutputFactory.newInstance();
		StringWriter s = new StringWriter();
		String result = null;
		try {
			XMLStreamWriter w = fact.createXMLStreamWriter(s);
			w.writeStartDocument();
			w.writeStartElement("actions");
			writeActions(actions, mappings, w);
			w.writeEndElement();
			w.writeEndDocument();
			w.close();
			result = s.toString();
			s.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private static void writeActions(List<Action> actions, MappingStore mappings, XMLStreamWriter w) throws XMLStreamException {
		for (Action a : actions) {
			w.writeStartElement("action");
			w.writeAttribute("type", a.getClass().getSimpleName());
			if (a instanceof Move || a instanceof Update) {
				Tree src = a.getNode();
				Tree dst = mappings.getDst(src);
				w.writeStartElement("before");
				w.writeAttribute("pos", Integer.toString(src.getPos()));
				w.writeAttribute("length", Integer.toString(src.getLength()));
				w.writeEndElement();
				w.writeStartElement("after");
				w.writeAttribute("pos", Integer.toString(dst.getPos()));
				w.writeAttribute("length", Integer.toString(dst.getLength()));
				w.writeEndElement();
			} else if (a instanceof Insert) {
				Tree dst = a.getNode();
				w.writeStartElement("after");
				w.writeAttribute("pos", Integer.toString(dst.getPos()));
				w.writeAttribute("length", Integer.toString(dst.getLength()));
				w.writeEndElement();
			} else if (a instanceof Delete) {
				Tree src = a.getNode();
				w.writeStartElement("before");
				w.writeAttribute("pos", Integer.toString(src.getPos()));
				w.writeAttribute("length", Integer.toString(src.getLength()));
				w.writeEndElement();
			}
			w.writeEndElement();
		}
	}

}
