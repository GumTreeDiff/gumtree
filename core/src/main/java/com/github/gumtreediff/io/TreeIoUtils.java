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

package com.github.gumtreediff.io;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.*;
import com.github.gumtreediff.tree.TreeContext.MetadataSerializers;
import com.github.gumtreediff.tree.TreeContext.MetadataUnserializers;
import com.google.gson.stream.JsonWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static com.github.gumtreediff.tree.TypeSet.type;

public final class TreeIoUtils {
    private TreeIoUtils() {
    } // Forbids instantiation of TreeIOUtils

    public static TreeGenerator fromXml() {
        return new XmlInternalGenerator();
    }

    public static TreeGenerator fromXml(MetadataUnserializers unserializers) {
        XmlInternalGenerator generator = new XmlInternalGenerator();
        generator.getUnserializers().addAll(unserializers);
        return generator;
    }

    public static TreeSerializer toXml(TreeContext ctx) {
        return toXml(ctx, ctx.getRoot());
    }

    public static TreeSerializer toXml(TreeContext ctx, ITree root) {
        return new TreeSerializer(ctx, root) {
            @Override
            protected TreeFormatter newFormatter(TreeContext ctx, MetadataSerializers serializers, Writer writer)
                    throws XMLStreamException {
                return new XmlFormatter(writer, ctx);
            }
        };
    }

    public static TreeSerializer toAnnotatedXml(TreeContext ctx, boolean isSrc, MappingStore m) {
        return toAnnotatedXml(ctx, ctx.getRoot(), isSrc, m);
    }

    public static TreeSerializer toAnnotatedXml(TreeContext ctx, ITree root, boolean isSrc, MappingStore m) {
        return new TreeSerializer(ctx, root) {
            @Override
            protected TreeFormatter newFormatter(TreeContext ctx, MetadataSerializers serializers, Writer writer)
                    throws XMLStreamException {
                return new XmlAnnotatedFormatter(writer, ctx, isSrc, m);
            }
        };
    }

    public static TreeSerializer toCompactXml(TreeContext ctx) {
        return toCompactXml(ctx, ctx.getRoot());
    }

    public static TreeSerializer toCompactXml(TreeContext ctx, ITree root) {
        return new TreeSerializer(ctx, root) {
            @Override
            protected TreeFormatter newFormatter(TreeContext ctx, MetadataSerializers serializers, Writer writer)
                    throws Exception {
                return new XmlCompactFormatter(writer, ctx);
            }
        };
    }

    public static TreeSerializer toJson(TreeContext ctx) {
        return toJson(ctx, ctx.getRoot());
    }

    public static TreeSerializer toJson(TreeContext ctx, ITree root) {
        return new TreeSerializer(ctx, root) {
            @Override
            protected TreeFormatter newFormatter(TreeContext ctx, MetadataSerializers serializers, Writer writer) {
                return new JsonFormatter(writer, ctx);
            }
        };
    }

    public static TreeSerializer toLisp(TreeContext ctx) {
        return toLisp(ctx, ctx.getRoot());
    }

    public static TreeSerializer toLisp(TreeContext ctx, ITree tree) {
        return new TreeSerializer(ctx, tree) {
            @Override
            protected TreeFormatter newFormatter(TreeContext ctx, MetadataSerializers serializers, Writer writer) {
                return new LispFormatter(writer, ctx);
            }
        };
    }

    public static TreeSerializer toDot(TreeContext ctx) {
        return toDot(ctx, ctx.getRoot());
    }

    public static TreeSerializer toDot(TreeContext ctx, ITree root) {
        return new TreeSerializer(ctx, root) {
            @Override
            protected TreeFormatter newFormatter(TreeContext ctx, MetadataSerializers serializer, Writer writer) {
                return new DotFormatter(writer, ctx);
            }
        };
    }

    public static TreeSerializer toText(TreeContext ctx) {
        return toText(ctx, ctx.getRoot());
    }

    public static TreeSerializer toText(TreeContext ctx, ITree root) {
        return new TreeSerializer(ctx, root) {
            @Override
            protected TreeFormatter newFormatter(TreeContext ctx, MetadataSerializers serializer, Writer writer) {
                return new TextFormatter(writer, ctx);
            }
        };
    }

    public static TreeSerializer toShortText(ITree root) {
        return new TreeSerializer(null, root) {
            @Override
            protected TreeFormatter newFormatter(TreeContext ctx, MetadataSerializers serializer, Writer writer) {
                return new ShortTextFormatter(writer, ctx);
            }
        };
    }

    public abstract static class AbstractSerializer {

        public abstract void writeTo(Writer writer) throws Exception;

        public void writeTo(OutputStream writer) throws Exception {
            // FIXME Since the stream is already open, we should not close it, however due to semantic issue
            // it should stay like this
            try (OutputStreamWriter os = new OutputStreamWriter(writer, StandardCharsets.UTF_8)) {
                writeTo(os);
            }
        }

        @Override
        public String toString() {
            try (StringWriter s = new StringWriter()) {
                writeTo(s);
                return s.toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void writeTo(String file) throws Exception {
            try (Writer w = Files.newBufferedWriter(Paths.get(file), Charset.forName("UTF-8"))) {
                writeTo(w);
            }
        }

        public void writeTo(File file) throws Exception {
            try (Writer w = Files.newBufferedWriter(file.toPath(), Charset.forName("UTF-8"))) {
                writeTo(w);
            }
        }
    }

    public abstract static class TreeSerializer extends AbstractSerializer {
        private final TreeContext context;
        private final ITree root;
        final MetadataSerializers serializers = new MetadataSerializers();

        public TreeSerializer(TreeContext ctx, ITree root) {
            context = ctx;
            this.root = root;
            if (ctx != null)
                serializers.addAll(ctx.getSerializers());
        }

        protected abstract TreeFormatter newFormatter(TreeContext ctx, MetadataSerializers serializers, Writer writer)
                throws Exception;

        public void writeTo(Writer writer) throws Exception {
            TreeFormatter formatter = newFormatter(context, serializers, writer);
            try {
                writeTree(formatter, root);
            } finally {
                formatter.close();
            }
        }

        private void forwardException(Exception e) {
            throw new FormatException(e);
        }

        protected void writeTree(TreeFormatter formatter, ITree root) throws Exception {
            formatter.startSerialization();
            if (context != null)
                writeAttributes(formatter, context.getMetadata());
            formatter.endProlog();
            try {
                TreeVisitor.visitTree(root, new TreeVisitor() {

                    @Override
                    public void startTree(ITree tree) {
                        try {
                            assert tree != null;
                            formatter.startTree(tree);
                            writeAttributes(formatter, tree.getMetadata());
                            formatter.endTreeProlog(tree);
                        } catch (Exception e) {
                            forwardException(e);
                        }
                    }

                    @Override
                    public void endTree(ITree tree) {
                        try {
                            formatter.endTree(tree);
                        } catch (Exception e) {
                            forwardException(e);
                        }
                    }
                });
            } catch (FormatException e) {
                throw e.getCause();
            }
            formatter.stopSerialization();
        }

        protected void writeAttributes(TreeFormatter formatter, Iterator<Entry<String, Object>> it) throws Exception {
            while (it.hasNext()) {
                Entry<String, Object> entry = it.next();
                serializers.serialize(formatter, entry.getKey(), entry.getValue());
            }
        }

        public TreeSerializer export(String name, MetadataSerializer serializer) {
            serializers.add(name, serializer);
            return this;
        }

        public TreeSerializer export(String... name) {
            for (String n : name)
                serializers.add(n, Object::toString);
            return this;
        }
    }

    public interface TreeFormatter {
        void startSerialization() throws Exception;

        void endProlog() throws Exception;

        void stopSerialization() throws Exception;

        void startTree(ITree tree) throws Exception;

        void endTreeProlog(ITree tree) throws Exception;

        void endTree(ITree tree) throws Exception;

        void close() throws Exception;

        void serializeAttribute(String name, String value) throws Exception;
    }

    @FunctionalInterface
    public interface MetadataSerializer {
        String toString(Object object);
    }

    @FunctionalInterface
    public interface MetadataUnserializer {
        Object fromString(String value);
    }

    static class FormatException extends RuntimeException {
        private static final long serialVersionUID = 593766540545763066L;
        Exception cause;

        public FormatException(Exception cause) {
            super(cause);
            this.cause = cause;
        }

        @Override
        public synchronized Exception getCause() {
            return cause;
        }
    }

    static class TreeFormatterAdapter implements TreeFormatter {
        protected final TreeContext context;

        protected TreeFormatterAdapter(TreeContext ctx) {
            context = ctx;
        }

        @Override
        public void startSerialization() throws Exception {
        }

        @Override
        public void endProlog() throws Exception {
        }

        @Override
        public void startTree(ITree tree) throws Exception {
        }

        @Override
        public void endTreeProlog(ITree tree) throws Exception {
        }

        @Override
        public void endTree(ITree tree) throws Exception {
        }

        @Override
        public void stopSerialization() throws Exception {
        }

        @Override
        public void close() throws Exception {
        }

        @Override
        public void serializeAttribute(String name, String value) throws Exception {
        }
    }

    abstract static class AbsXmlFormatter extends TreeFormatterAdapter {
        protected final XMLStreamWriter writer;

        protected AbsXmlFormatter(Writer w, TreeContext ctx) throws XMLStreamException {
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

    static class XmlFormatter extends AbsXmlFormatter {
        public XmlFormatter(Writer w, TreeContext ctx) throws XMLStreamException {
            super(w, ctx);
        }

        @Override
        public void startSerialization() throws XMLStreamException {
            super.startSerialization();
            writer.writeStartElement("root");
            writer.writeStartElement("context");
        }

        @Override
        public void endProlog() throws XMLStreamException {
            writer.writeEndElement();
        }

        @Override
        public void stopSerialization() throws XMLStreamException {
            writer.writeEndElement();
            super.stopSerialization();
        }

        @Override
        public void serializeAttribute(String name, String value) throws XMLStreamException {
            writer.writeStartElement(name);
            writer.writeCharacters(value);
            writer.writeEndElement();
        }

        @Override
        public void startTree(ITree tree) throws XMLStreamException {
            writer.writeStartElement("tree");
            writer.writeAttribute("type", tree.getType().toString());
            if (tree.hasLabel()) writer.writeAttribute("label", tree.getLabel());
            if (ITree.NO_POS != tree.getPos()) {
                writer.writeAttribute("pos", Integer.toString(tree.getPos()));
                writer.writeAttribute("length", Integer.toString(tree.getLength()));
            }
        }

        @Override
        public void endTree(ITree tree) throws XMLStreamException {
            writer.writeEndElement();
        }
    }

    static class XmlAnnotatedFormatter extends XmlFormatter {
        final SearchOther searchOther;

        public XmlAnnotatedFormatter(Writer w, TreeContext ctx, boolean isSrc,
                                     MappingStore m) throws XMLStreamException {
            super(w, ctx);

            if (isSrc)
                searchOther = (tree) -> m.isSrcMapped(tree) ? m.getDstForSrc(tree) : null;
            else
                searchOther = (tree) -> m.isDstMapped(tree) ? m.getSrcForDst(tree) : null;
        }

        interface SearchOther {
            ITree lookup(ITree tree);
        }

        @Override
        public void startTree(ITree tree) throws XMLStreamException {
            super.startTree(tree);
            ITree o = searchOther.lookup(tree);

            if (o != null) {
                if (ITree.NO_POS != o.getPos()) {
                    writer.writeAttribute("other_pos", Integer.toString(o.getPos()));
                    writer.writeAttribute("other_length", Integer.toString(o.getLength()));
                }
            }
        }
    }

    static class XmlCompactFormatter extends AbsXmlFormatter {
        public XmlCompactFormatter(Writer w, TreeContext ctx) throws XMLStreamException {
            super(w, ctx);
        }

        @Override
        public void startSerialization() throws XMLStreamException {
            super.startSerialization();
            writer.writeStartElement("root");
        }

        @Override
        public void stopSerialization() throws XMLStreamException {
            writer.writeEndElement();
            super.stopSerialization();
        }

        @Override
        public void serializeAttribute(String name, String value) throws XMLStreamException {
            writer.writeAttribute(name, value);
        }

        @Override
        public void startTree(ITree tree) throws XMLStreamException {
            if (tree.getChildren().size() == 0)
                writer.writeEmptyElement(tree.getType().toString());
            else
                writer.writeStartElement(tree.getType().toString());
            if (tree.hasLabel())
                writer.writeAttribute("label", tree.getLabel());
        }

        @Override
        public void endTree(ITree tree) throws XMLStreamException {
            if (tree.getChildren().size() > 0)
                writer.writeEndElement();
        }
    }

    static class LispFormatter extends TreeFormatterAdapter {
        protected final Writer writer;
        protected final Pattern protectChars = Pattern.compile("[ ,\"]");
        protected final Pattern escapeChars = Pattern.compile("[\\\\\"]");
        int level = 0;

        protected LispFormatter(Writer w, TreeContext ctx) {
            super(ctx);
            writer = w;
        }

        @Override
        public void startSerialization() throws IOException {
            writer.write("((");
        }

        @Override
        public void startTree(ITree tree) throws IOException {
            if (!tree.isRoot())
                writer.write("\n");
            for (int i = 0; i < level; i++)
                writer.write("    ");
            level++;

            String pos = (ITree.NO_POS == tree.getPos() ? "" : String.format("(%d %d)",
                    tree.getPos(), tree.getLength()));

            writer.write(String.format("(%s %s (%s",
                    protect(tree.getType().toString()), protect(tree.getLabel()), pos));
        }

        @Override
        public void endProlog() throws Exception {
            writer.append(") ");
        }

        @Override
        public void endTreeProlog(ITree tree) throws Exception {
            writer.append(") (");
        }

        @Override
        public void serializeAttribute(String name, String value) throws Exception {
            writer.append(String.format("(:%s %s) ", name, protect(value)));
        }

        protected String protect(String val) {
            String text = escapeChars.matcher(val).replaceAll("\\\\$0");
            if (protectChars.matcher(text).find() || val.isEmpty())
                return String.format("\"%s\"", text);
            else
                return text;
        }

        @Override
        public void endTree(ITree tree) throws IOException {
            writer.write(")");
            level--;
        }

        @Override
        public void stopSerialization() throws IOException {
            writer.write(")");
        }
    }

    static class DotFormatter extends TreeFormatterAdapter {
        protected final Writer writer;

        private static AtomicLong idCounter = new AtomicLong();

        private Map<ITree, String> idForTrees = new HashMap<>();

        protected DotFormatter(Writer w, TreeContext ctx) {
            super(ctx);
            writer = w;
        }

        @Override
        public void startSerialization() throws Exception {
            writer.write("digraph G {\n");
        }

        @Override
        public void startTree(ITree tree) throws Exception {
            String label = getCleanLabel(tree);
            writer.write(String.format("\t%s [label=\"%s\"];\n", id(tree), label));
            if (tree.getParent() != null)
                writer.write(String.format("\t%s -> %s;\n", id(tree.getParent()), id(tree)));
        }

        @Override
        public void stopSerialization() throws Exception {
            writer.write("}");
        }

        private String getCleanLabel(ITree tree) {
            String label = tree.toString();
            if (label.contains("\"") || label.contains("\\s"))
                label = label
                        .replaceAll("\"", "")
                        .replaceAll("\\s", "")
                        .replaceAll("\\\\", "");
            if (label.length() > 30)
                label = label.substring(0, 30);
            return label;
        }

        private String id(ITree t) {
            if (idForTrees.containsKey(t))
                return idForTrees.get(t);
            else {
                String id = generateId();
                idForTrees.put(t, id);
                return id;
            }
        }

        private static String generateId() {
            return "id_" + idCounter.getAndIncrement();
        }
    }

    static class JsonFormatter extends TreeFormatterAdapter {
        private final JsonWriter writer;

        public JsonFormatter(Writer w, TreeContext ctx) {
            super(ctx);
            writer = new JsonWriter(w);
            writer.setIndent("  ");
        }

        @Override
        public void startTree(ITree t) throws IOException {
            writer.beginObject();
            writer.name("type").value(t.getType().toString());
            if (t.hasLabel()) writer.name("label").value(t.getLabel());
            if (ITree.NO_POS != t.getPos()) {
                writer.name("pos").value(Integer.toString(t.getPos()));
                writer.name("length").value(Integer.toString(t.getLength()));
            }
        }

        @Override
        public void endTreeProlog(ITree tree) throws IOException {
            writer.name("children");
            writer.beginArray();
        }

        @Override
        public void endTree(ITree tree) throws IOException {
            writer.endArray();
            writer.endObject();
        }

        @Override
        public void startSerialization() throws IOException {
            writer.beginObject();
            writer.setIndent("\t");
        }

        @Override
        public void endProlog() throws IOException {
            writer.name("root");
        }

        @Override
        public void serializeAttribute(String key, String value) throws IOException {
            writer.name(key).value(value);
        }

        @Override
        public void stopSerialization() throws IOException {
            writer.endObject();
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }
    }

    public abstract static class AbstractTextFormatter extends TreeFormatterAdapter {
        protected final Writer writer;
        int level = 0;

        public AbstractTextFormatter(Writer w, TreeContext ctx) {
            super(ctx);
            writer = w;
        }

        protected void indent(int level, String prefix) throws IOException {
            for (int i = 0; i < level; i++)
                writer.write(prefix);
        }

        @Override
        public void startTree(ITree tree) throws IOException {
            if (level != 0) writer.write("\n");
            indent(level, "    ");
            level++;

            writeTree(tree);
        }

        protected abstract void writeTree(ITree tree) throws IOException;

        @Override
        public void endTree(ITree tree) throws IOException {
            level--;
        }
    }

    public static class TextFormatter extends AbstractTextFormatter {

        public TextFormatter(Writer w, TreeContext ctx) {
            super(w, ctx);
        }

        @Override
        public void writeTree(ITree tree) throws IOException {
            writer.write(tree.toString());
        }
    }

    public static class ShortTextFormatter extends AbstractTextFormatter {

        public ShortTextFormatter(Writer w, TreeContext ctx) {
            super(w, ctx);
        }

        @Override
        public void writeTree(ITree tree) throws IOException {
            writer.write(tree.toString());
        }
    }

    @Register(id = "xml", accept = "\\.gxml$")
    // TODO Since it is not in the right package, I'm not even sure it is visible in the registry
    // TODO should we move this class elsewhere (another package)
    public static class XmlInternalGenerator extends TreeGenerator {

        static MetadataUnserializers defaultUnserializers = new MetadataUnserializers();
        final MetadataUnserializers unserializers = new MetadataUnserializers(); // FIXME should it be pushed up or not?

        private static final QName TYPE = new QName("type");

        private static final QName LABEL = new QName("label");
        private static final String POS = "pos";
        private static final String LENGTH = "length";

        static {
            defaultUnserializers.add(POS, Integer::parseInt);
            defaultUnserializers.add(LENGTH, Integer::parseInt);
        }

        public XmlInternalGenerator() {
            unserializers.addAll(defaultUnserializers);
        }

        @Override
        protected TreeContext generate(Reader source) throws IOException {
            XMLInputFactory fact = XMLInputFactory.newInstance();
            TreeContext context = new TreeContext();
            try {
                ArrayDeque<ITree> trees = new ArrayDeque<>();
                XMLEventReader r = fact.createXMLEventReader(source);
                while (r.hasNext()) {
                    XMLEvent e = r.nextEvent();
                    if (e instanceof StartElement) {
                        StartElement s = (StartElement) e;
                        if (!s.getName().getLocalPart().equals("tree")) // FIXME need to deal with options
                            continue;
                        Type type = type(s.getAttributeByName(TYPE).getValue());

                        ITree t = context.createTree(type, labelForAttribute(s, LABEL));
                        // FIXME this iterator has no type, due to the API. We have to cast it later
                        Iterator<?> it = s.getAttributes();
                        while (it.hasNext()) {
                            Attribute a = (Attribute) it.next();
                            unserializers.load(t, a.getName().getLocalPart(), a.getValue());
                        }

                        if (trees.isEmpty())
                            context.setRoot(t);
                        else
                            t.setParentAndUpdateChildren(trees.peekFirst());
                        trees.addFirst(t);
                    } else if (e instanceof EndElement) {
                        if (!((EndElement) e).getName().getLocalPart().equals("tree")) // FIXME need to deal with option
                            continue;
                        trees.removeFirst();
                    }
                }
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

        public MetadataUnserializers getUnserializers() {
            return unserializers;
        }
    }
}
