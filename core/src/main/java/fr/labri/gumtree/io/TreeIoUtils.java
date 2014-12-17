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
import java.io.Writer;
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
import fr.labri.gumtree.tree.TreeContext;

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

	private TreeIoUtils() {} // Forbids instantiation of TreeIOUtils
	
	public static TreeContext fromXml(InputStream iStream) {
		return fromXml(new InputStreamReader(iStream));
	}
	
	public static TreeContext fromXmlString(String xml) {
		return fromXml(new StringReader(xml));
	}
	
	public static TreeContext fromXmlFile(String path) {
		try {
			return fromXml(new FileReader(path));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static TreeContext fromXml(Reader source) {
		XMLInputFactory fact = XMLInputFactory.newInstance();
		TreeContext context = new TreeContext();
		try {
			Stack<ITree> trees = new Stack<>();
			XMLEventReader r = fact.createXMLEventReader(source);
			while (r.hasNext()) {
				XMLEvent e = r.nextEvent();
				if (e instanceof StartElement) {
					StartElement s = (StartElement) e;
					int type = Integer.parseInt(s.getAttributeByName(TYPE).getValue());
					
					ITree t = context.createTree(type, labelForAttribute(s, LABEL), labelForAttribute(s, TYPE_LABEL));
					
					
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
					
					if (trees.isEmpty())
						context.setRoot(t);
					else
						t.setParentAndUpdateChildren(trees.peek());
					trees.push(t);
				} else if (e instanceof EndElement)
					trees.pop();
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
	
	public static TreeSerializer toXml(TreeContext ctx) {
		return new TreeSerializer(ctx) {
			@Override
			protected TreeFormater newFormater(TreeContext ctx, Writer writer) throws XMLStreamException {
				return new XMLFormater(writer, ctx);
			}
		};
	}

	public static TreeSerializer toAnnotatedXml(TreeContext ctx, boolean isSrc, MappingStore m) {
		return new TreeSerializer(ctx) {
			@Override
			protected TreeFormater newFormater(TreeContext ctx, Writer writer) throws XMLStreamException {
				return new XMLAnnotatedFormater(writer, ctx, isSrc, m);
			}
		};
	}
	
	public static TreeSerializer toCompactXML(TreeContext ctx) {
		return new TreeSerializer(ctx) {
			@Override
			protected TreeFormater newFormater(TreeContext ctx, Writer writer) throws Exception {
				return new XMLCompactFormater(writer, ctx);
			}
		};
	}
	
	public static TreeSerializer toJSON(TreeContext ctx) {
		return new TreeSerializer(ctx) {
			@Override
			protected TreeFormater newFormater(TreeContext ctx, Writer writer) throws Exception {
				return new JSONFormater(writer, ctx);
			}
		};
	}
	
	public static TreeSerializer toDot(TreeContext ctx) {
		return new TreeSerializer(ctx) {
			@Override
			protected TreeFormater newFormater(TreeContext ctx, Writer writer) throws Exception {
				return new DotFormater(writer, ctx);
			}
		};
	}

	static public abstract class TreeSerializer {
		TreeContext context;
		
		public TreeSerializer(TreeContext ctx) {
			context = ctx;
		}
		
		protected abstract TreeFormater newFormater(TreeContext ctx, Writer writer) throws Exception;
		
		public void writeTo(Writer writer) throws Exception {
			TreeFormater formater = newFormater(context, writer);
			try {
				serialize(formater);
			} finally {
				formater.close();
			}
		}
		
		public String toString() {
			StringWriter s = new StringWriter();
			try {
				writeTo(s);
				s.close(); // FIXME this is useless (do nothing) but thows an exception, thus I dont' put it in the finally block where it belongs
			} catch(Exception e) { }
			return s.toString();
		}
		
		public void writeTo(String file) throws Exception {
			FileWriter w = new FileWriter(file);
			try {
				writeTo(w);
			} finally {
				w.close();
			}
		}
		
		private void serialize(TreeFormater serializer) throws Exception {
			serializer.startSerialization();
			writeTree(serializer, context.getRoot());
			serializer.stopSerialization();
		}
		
		private void writeTree(TreeFormater serializer, ITree t) throws Exception {
			serializer.startTree(t);
			for (ITree c: t.getChildren()) // FIXME change by a preOrder / postOrder / BFSIterator
				writeTree(serializer, c);
			serializer.endTree(t);
		}
	}

	interface TreeFormater{ // TODO or not, add context as argument of method instead of capturing it
		void startSerialization() throws Exception;
		void startTree(ITree tree) throws Exception;
		void endTree(ITree tree) throws Exception;
		void stopSerialization() throws Exception;
		
		void close() throws Exception;
	}
	
	static class TreeFormaterAdapter implements TreeFormater  {
		final protected TreeContext context;
		protected TreeFormaterAdapter(TreeContext ctx) {
			context = ctx;
		}

		@Override
		public void startSerialization() throws Exception { }

		@Override
		public void startTree(ITree tree) throws Exception { }

		@Override
		public void endTree(ITree tree) throws Exception { }

		@Override
		public void stopSerialization() throws Exception { }

		@Override
		public void close() throws Exception { }
	}
	
	static abstract class AbsXMLFormater extends TreeFormaterAdapter {
		final protected XMLStreamWriter writer;

		protected AbsXMLFormater(Writer w, TreeContext ctx) throws XMLStreamException {
			super(ctx);
			XMLOutputFactory f = XMLOutputFactory.newInstance();
			writer = new IndentingXMLStreamWriter(f.createXMLStreamWriter(w));
		}

		@Override
		public void startSerialization() throws XMLStreamException {
			writer.writeStartDocument();
		}

		@Override
		public void stopSerialization() throws XMLStreamException {
			writer.writeEndDocument();
		}
		
		@Override
		public void close() throws XMLStreamException {
			writer.close();
		}
	}
	
	static class XMLFormater extends AbsXMLFormater {
		public XMLFormater(Writer w, TreeContext ctx) throws XMLStreamException {
			super(w, ctx);
		}

		@Override
		public void startTree(ITree tree) throws XMLStreamException {
			writer.writeStartElement("tree");
			writer.writeAttribute("type", Integer.toString(tree.getType()));
			if (tree.hasLabel()) writer.writeAttribute("label", tree.getLabel());
			if (context.hasLabelFor(tree.getType())) writer.writeAttribute("typeLabel", context.getTypeLabel(tree.getType()));
			if (ITree.NO_VALUE != tree.getPos()) {
				writer.writeAttribute("pos", Integer.toString(tree.getPos()));
				writer.writeAttribute("length", Integer.toString(tree.getLength()));
			}
			if (tree.getLcPosStart() != null) {
				writer.writeAttribute("line_before", Integer.toString(tree.getLcPosStart()[0]));
				writer.writeAttribute("col_before", Integer.toString(tree.getLcPosStart()[1]));
				writer.writeAttribute("line_after", Integer.toString(tree.getLcPosEnd()[0]));
				writer.writeAttribute("col_after", Integer.toString(tree.getLcPosEnd()[1]));
			}
		}

		@Override
		public void endTree(ITree tree) throws XMLStreamException {
			writer.writeEndElement();
		}
	}
	
	static class XMLAnnotatedFormater extends XMLFormater {
		final SearchOther searchOther;
		public XMLAnnotatedFormater(Writer w, TreeContext ctx, boolean isSrc, MappingStore m) throws XMLStreamException {
			super(w, ctx);
			
			if (isSrc)
				searchOther = (tree) -> {
					return m.hasSrc(tree) ? m.getDst(tree) : null; 
				};
			else
				searchOther = (tree) -> {
					return m.hasDst(tree) ? m.getSrc(tree) : null;
				};
		}
		
		interface SearchOther {
			ITree lookup(ITree tree);
		}
		
		@Override
		public void startTree(ITree tree) throws XMLStreamException {
			super.startTree(tree);
			ITree o = searchOther.lookup(tree);
			
			if (o != null) {
				if (ITree.NO_VALUE != o.getPos()) {
					writer.writeAttribute("other_pos", Integer.toString(o.getPos()));
					writer.writeAttribute("other_length", Integer.toString(o.getLength()));
				}
				if (o.getLcPosStart() != null) {
					writer.writeAttribute("other_line_before", Integer.toString(o.getLcPosStart()[0]));
					writer.writeAttribute("other_col_before", Integer.toString(o.getLcPosStart()[1]));
					writer.writeAttribute("other_line_after", Integer.toString(o.getLcPosEnd()[0]));
					writer.writeAttribute("other_col_after", Integer.toString(o.getLcPosEnd()[1]));
				}
			}
		}
	}
	
	static class XMLCompactFormater extends AbsXMLFormater {
		public XMLCompactFormater(Writer w, TreeContext ctx) throws XMLStreamException {
			super(w, ctx);
		}

		@Override
		public void startTree(ITree tree) throws XMLStreamException {
			writer.writeStartElement(context.getTypeLabel(tree.getType()));
			if (tree.hasLabel()) writer.writeAttribute("label", tree.getLabel());
		}

		@Override
		public void endTree(ITree tree) throws XMLStreamException {
			writer.writeEndElement();
		}
	}
	
	static class DotFormater extends TreeFormaterAdapter {
		final protected Writer writer;

		protected DotFormater(Writer w, TreeContext ctx) {
			super(ctx);
			writer = w;
		}

		@Override
		public void startSerialization() throws Exception {
			writer.write("digraph G {\n");
		}

		@Override
		public void startTree(ITree tree) throws Exception {
			String label = tree.toPrettyString(context);
			if (label.contains("\"") || label.contains("\\s"))
				label = label.replaceAll("\"", "").replaceAll("\\s", "").replaceAll("\\\\", "");
			if (label.length() > 30)
				label = label.substring(0, 30);
			writer.write(tree.getId() + " [label=\"" + label + "\"");
			if (tree.isMatched())
				writer.write(",style=filled,fillcolor=cadetblue1");
			writer.write("];\n");
			
			if (tree.getParent() != null)
				writer.write(tree.getParent().getId() + " -> " + tree.getId() + ";\n");
		}
		@Override
		public void stopSerialization() throws Exception {
			writer.write("}");
		}
	}
	
	static class JSONFormater extends TreeFormaterAdapter {
		final private JsonWriter writer;

		public JSONFormater(Writer w, TreeContext ctx) {
			super(ctx);
			writer = new JsonWriter(w);
		}

		@Override
		public void startTree(ITree t) throws IOException {
			writer.beginObject();
			
			writer.name("type").value(Integer.toString(t.getType()));

			if (t.hasLabel()) writer.name("label").value(t.getLabel());
			if (context.hasLabelFor(t.getType())) writer.name("typeLabel").value(context.getTypeLabel(t.getType()));
			
			if (ITree.NO_VALUE != t.getPos()) {
				writer.name("pos").value(Integer.toString(t.getPos()));
				writer.name("length").value(Integer.toString(t.getLength()));
			}
			
			if (t.getLcPosStart() != null) {
				writer.name("line_before").value(Integer.toString(t.getLcPosStart()[0]));
				writer.name("col_before").value(Integer.toString(t.getLcPosStart()[1]));
				writer.name("line_after").value(Integer.toString(t.getLcPosEnd()[0]));
				writer.name("col_after").value(Integer.toString(t.getLcPosEnd()[1]));
			}
			
			writer.name("children");
			writer.beginArray();
		}

		@Override
		public void endTree(ITree tree) throws IOException {
			writer.endArray();
			writer.endObject();
		}

		@Override
		public void startSerialization() throws Exception {
			writer.setIndent("\t");					
		}

		@Override
		public void close() throws Exception {
			try { writer.close(); } catch (Exception e) {};
		}
	}
}