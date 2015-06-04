package gen.sax;

import static fr.labri.gumtree.tree.ITree.NO_LABEL;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import fr.labri.gumtree.io.TreeGenerator;
import fr.labri.gumtree.tree.ITree;
import fr.labri.gumtree.tree.TreeContext;

public class SAXTreeGenerator extends TreeGenerator {
	public static final String DOCUMENT = "Document";
	public static final int DOCUMENT_ID = -2;
	public static final int CDATA_ID = -3;
	public static final String ATTR = "Attr";
	public static final String CDATA = "CData";
	public static final String ELT = "Elt";

	public TreeContext generate(Reader reader) throws IOException {
		try {
//			TreeContext tc = new TreeContext();
			XMLReader xr = XMLReaderFactory.createXMLReader();
			XMLHandlers hdl = new XMLHandlers();
			xr.setContentHandler(hdl);
			xr.setErrorHandler(hdl);
			xr.parse(new InputSource(reader));
			return hdl.tc;
		} catch (SAXException e) {
		} finally {
			// close resources
		}
		return null;
	}

	class XMLHandlers extends DefaultHandler {
		Locator locator;
		Deque<ITree> stack = new ArrayDeque<ITree>();
		TreeContext tc = new TreeContext();
		Map<String, Integer> names = new HashMap<>();
		
		@Override
		public void setDocumentLocator(Locator locator) {
			this.locator = locator;
		}
		
		@Override
		public void startDocument() throws SAXException {
			ITree t = tc.createTree(DOCUMENT_ID, NO_LABEL, DOCUMENT);
			tc.setRoot(t);
			stack.push(t);
		}
		
		@Override
		public void endDocument() throws SAXException {
			stack.pop();
			assert stack.isEmpty();
		}
			
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			String name = qName + ELT; 
			ITree t = tc.createTree(typeFor(name), NO_LABEL, name);
			addAttributes(t, attributes);
			t.setLcPosStart(new int[] {locator.getLineNumber(), locator.getColumnNumber()});
			// TODO pos
			ITree p = stack.peek();
			p.addChild(t);
			stack.push(t);
		}
		
		private void addAttributes(ITree tree, Attributes attrs) {
			int len = attrs.getLength();
			for (int i = 0; i < len; i++) {
				String attrName = attrs.getQName(i) + ATTR;
				ITree at = tc.createTree(typeFor(attrName), attrs.getValue(i), attrName);
				tree.addChild(at);
				// TODO add positions
			}
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			ITree t = stack.pop();
			t.setLcPosEnd(new int[] {locator.getLineNumber(), locator.getColumnNumber()}); // FIXME !
			// TODO length
		}
		
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			tc.createTree(CDATA_ID, new String(ch, start, length), CDATA);
		}
		
		int typeFor(String name) {
			Integer id = names.get(name);
			if (id == null) {
				id = names.size();
				names.put(name, id);
			}
			return id;
		}
	}
	
	@Override
	public String getName() {
		return "sax-xml";
	}

	@Override
	public boolean handleFile(String filename) {
		String f = filename.toLowerCase();
		return f.endsWith(".xml") || f.endsWith(".xsd");
	}
}
