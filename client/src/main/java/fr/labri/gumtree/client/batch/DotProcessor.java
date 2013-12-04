package fr.labri.gumtree.client.batch;

import java.io.IOException;

import fr.labri.gumtree.client.TreeGeneratorRegistry;
import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.tree.Tree;

public class DotProcessor extends AbstractFileProcessor {
	
	public static void main(String[] args) {
		DotProcessor g = new DotProcessor(args[0]);
		g.process();
	}
	
	public DotProcessor(String inFolder) {
		super(inFolder, "dot");
	}

	public DotProcessor(String inFolder, String outFolder) {
		super(inFolder, outFolder);
	}

	@Override
	public void process(String file) throws IOException {
		Tree t = TreeGeneratorRegistry.getInstance().getTree(file);
		if (t != null) TreeIoUtils.toDot(t, file + ".dot");
	}

}
