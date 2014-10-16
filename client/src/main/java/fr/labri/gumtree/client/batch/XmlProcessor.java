package fr.labri.gumtree.client.batch;

import java.io.IOException;

import fr.labri.gumtree.client.TreeGeneratorRegistry;
import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.tree.TreeContext;

public class XmlProcessor extends AbstractFileProcessor {
	
	public static void main(String[] args) {
		XmlProcessor g = new XmlProcessor(args[0]);
		g.process();
	}

	public XmlProcessor(String inFolder) {
		super(inFolder, "xml");
	}
	
	public XmlProcessor(String inFolder, String outFolder) {
		super(inFolder, outFolder);
	}

	@Override
	public void process(String file) throws IOException {
		TreeContext t = TreeGeneratorRegistry.getInstance().getTree(file);
		if (t != null) TreeIoUtils.toXml(t, file + ".xml");
	}

}
