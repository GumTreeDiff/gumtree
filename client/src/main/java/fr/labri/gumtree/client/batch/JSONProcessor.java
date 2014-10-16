package fr.labri.gumtree.client.batch;

import java.io.IOException;

import fr.labri.gumtree.client.TreeGeneratorRegistry;
import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.tree.TreeContext;

public class JSONProcessor extends AbstractFileProcessor {
	
	public static void main(String[] args) {
		JSONProcessor g = new JSONProcessor(args[0]);
		g.process();
	}

	public JSONProcessor(String inFolder) {
		super(inFolder, "json");
	}
	
	public JSONProcessor(String inFolder, String outFolder) {
		super(inFolder, outFolder);
	}

	@Override
	public void process(String file) throws IOException {
		TreeContext t = TreeGeneratorRegistry.getInstance().getTree(file);
		if (t != null) TreeIoUtils.toJSON(t, file + ".json");
	}

}
