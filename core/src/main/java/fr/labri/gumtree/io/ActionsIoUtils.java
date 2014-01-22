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
		XMLOutputFactory f = XMLOutputFactory.newInstance();
		StringWriter s = new StringWriter();
		String result = null;
		try {
			XMLStreamWriter w = new IndentingXMLStreamWriter(f.createXMLStreamWriter(s));
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
				w.writeEmptyElement("before");
				if (src.getLcPosStart() != null) {
					w.writeAttribute("begin_line", Integer.toString(src.getLcPosStart()[0]));
					w.writeAttribute("begin_col", Integer.toString(src.getLcPosStart()[1]));
					w.writeAttribute("end_line", Integer.toString(src.getLcPosEnd()[0]));
					w.writeAttribute("end_col", Integer.toString(src.getLcPosEnd()[1]));
				}
				//w.writeEndElement();
				w.writeEmptyElement("after");
				if (dst.getLcPosStart() != null) {
					w.writeAttribute("begin_line", Integer.toString(dst.getLcPosStart()[0]));
					w.writeAttribute("begin_col", Integer.toString(dst.getLcPosStart()[1]));
					w.writeAttribute("end_line", Integer.toString(dst.getLcPosEnd()[0]));
					w.writeAttribute("end_col", Integer.toString(dst.getLcPosEnd()[1]));
				}
				//w.writeEndElement();
			} else if (a instanceof Insert) {
				Tree dst = a.getNode();
				w.writeEmptyElement("after");
				if (dst.getLcPosStart() != null) {
					w.writeAttribute("begin_line", Integer.toString(dst.getLcPosStart()[0]));
					w.writeAttribute("begin_col", Integer.toString(dst.getLcPosStart()[1]));
					w.writeAttribute("end_line", Integer.toString(dst.getLcPosEnd()[0]));
					w.writeAttribute("end_col", Integer.toString(dst.getLcPosEnd()[1]));
				}
				//w.writeEndElement();
			} else if (a instanceof Delete) {
				Tree src = a.getNode();
				w.writeEmptyElement("before");
				if (src.getLcPosStart() != null) {
					w.writeAttribute("begin_line", Integer.toString(src.getLcPosStart()[0]));
					w.writeAttribute("begin_col", Integer.toString(src.getLcPosStart()[1]));
					w.writeAttribute("end_line", Integer.toString(src.getLcPosEnd()[0]));
					w.writeAttribute("end_col", Integer.toString(src.getLcPosEnd()[1]));
				}
				//w.writeEndElement();
			}
			w.writeEndElement();
		}
	}

}
