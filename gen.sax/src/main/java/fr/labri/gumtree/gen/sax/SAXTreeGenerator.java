package fr.labri.gumtree.gen.sax;

import fr.labri.gumtree.gen.Register;
import fr.labri.gumtree.io.LineReader;
import fr.labri.gumtree.gen.TreeGenerator;
import fr.labri.gumtree.tree.ITree;
import fr.labri.gumtree.tree.TreeContext;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

import static fr.labri.gumtree.tree.ITree.NO_LABEL;

@Register(id = "xml-sax", accept = {"\\.xml$", "\\.xsd$", "\\.wadl$"})
public class SAXTreeGenerator extends TreeGenerator {
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
			XMLHandlers hdl = new XMLHandlers(lr);

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

	class XMLHandlers extends DefaultHandler {
		public int lastPosition[] = {1, 1};

		Locator locator;
		Deque<ITree> stack = new ArrayDeque<ITree>();
		TreeContext tc = new TreeContext();
		Map<String, Integer> names = new HashMap<>();
		LineReader lineReader;
		public XMLHandlers(LineReader lr) {
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
			t.setLcPosStart(lastPosition);
			tc.setRoot(t);
			stack.push(t);
		}

		public void processingInstruction (String target, String data) {
			System.out.println(target + " " + data);
		}

		@Override
		public void endDocument() throws SAXException {
			debug("enddoc");
			ITree t = stack.pop();
			int line = locator.getLineNumber();
			int col = locator.getColumnNumber();
			t.setLcPosEnd(new int[]{line, col});
			t.setLength(lineReader.positionFor(line, col));
			assert stack.isEmpty();
		}
			
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			debug("startElt", qName);
			ITree t = tc.createTree(ELT_ID, qName, ELT);
			addAttributes(t, attributes);
			setStartPosition(t);
			ITree p = stack.peek();
			p.addChild(t);
			stack.push(t);
		}

		void debug(Object... o) {
			System.out.println("lc: " + locator.getLineNumber()+":" + locator.getColumnNumber());
			System.out.println("last: " + Arrays.toString(lastPosition));
			System.out.println("stack: " + stack);
			if (o.length > 0)
				System.out.println("=> " + Arrays.toString(o));
		}

		private void setStartPosition(ITree t) {
			int pos[] = currentPosition();
			t.setLcPosStart(pos);
			t.setPos(lineReader.positionFor(pos[0], pos[1]));
		}

		private void setEndPosition(ITree t) {
			int pos[] = currentPosition();
			t.setLcPosEnd(new int[]{locator.getLineNumber(), locator.getColumnNumber()}); //FIXME
			t.setPos(lineReader.positionFor(locator.getLineNumber(), locator.getColumnNumber()) - t.getPos());
		}

		private int[] currentPosition() {
			int lp[] = lastPosition;
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
