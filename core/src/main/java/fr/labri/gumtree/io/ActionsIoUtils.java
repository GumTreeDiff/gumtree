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

	//FIXME this output format does not work with AST other than C (missing LcPosStart attribute)
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
			w.writeAttribute("tree", a.getNode().getTypeLabel());
			if (a instanceof Move || a instanceof Update) {
				Tree src = a.getNode();
				Tree dst = mappings.getDst(src);
				writeTreePos(w, true, src);
				writeTreePos(w, false, dst);
			} else if (a instanceof Insert) {
				Tree dst = a.getNode();
				if (dst.isRoot()) writeInsertPos(w, true, new int[] {0, 0});
				else {
					int idx = dst.getParent().getChildPosition(dst);
					if (idx == 0) writeInsertPos(w, true, dst.getParent().getLcPosStart());
					else writeInsertPos(w, true, dst.getParent().getChildren().get(idx -1).getLcPosEnd());
				}
				writeTreePos(w, false, dst);
			} else if (a instanceof Delete) {
				Tree src = a.getNode();
				writeTreePos(w, true, src);
			}
			w.writeEndElement();
		}
	}

	private static void writeTreePos(XMLStreamWriter w, boolean isBefore, Tree tree) throws XMLStreamException {
		if (isBefore) w.writeEmptyElement("before"); else w.writeEmptyElement("after");
		if (tree.getLcPosStart() != null) {
			w.writeAttribute("begin_line", Integer.toString(tree.getLcPosStart()[0]));
			w.writeAttribute("begin_col", Integer.toString(tree.getLcPosStart()[1]));
			w.writeAttribute("end_line", Integer.toString(tree.getLcPosEnd()[0]));
			w.writeAttribute("end_col", Integer.toString(tree.getLcPosEnd()[1]));
		}
	}
	
	private static void writeInsertPos(XMLStreamWriter w, boolean isBefore, int[] pos) throws XMLStreamException {
		if (isBefore) w.writeEmptyElement("before"); else w.writeEmptyElement("after");
		w.writeAttribute("begin_line", Integer.toString(pos[0]));
		w.writeAttribute("begin_col", Integer.toString(pos[1]));
		w.writeAttribute("end_line", Integer.toString(pos[0]));
		w.writeAttribute("end_col", Integer.toString(pos[1]));
	}

}
