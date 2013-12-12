package fr.labri.gumtree.client.batch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import fr.labri.gumtree.client.MatcherFactory;
import fr.labri.gumtree.client.TreeGeneratorRegistry;
import fr.labri.gumtree.client.ui.web.BootstrapGenerator;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.tree.Tree;

public final class WebDiffProcessor extends AbstractFilePairsProcessor {
	
	public static void main(String[] args) {
		WebDiffProcessor g = new WebDiffProcessor(args[0]);
		g.process();
	}
	
	private static final String[] BOOTSTRAP_RESOURCES = new String[] { 
		"assets/gumtree.js", "assets/gumtree.css", "assets/bootstrap.min.js", "assets/bootstrap.min.css", "assets/jquery.min.js"
	};
	
	public WebDiffProcessor(String folder) {
		super(folder);
		ensureBootstrap();
	}
	
	@Override
	public void processFilePair(String fsrc, String fdst) throws IOException {
		Tree src = TreeGeneratorRegistry.getInstance().getTree(fsrc);
		Tree dst = TreeGeneratorRegistry.getInstance().getTree(fdst);
		Matcher matcher = MatcherFactory.createMatcher(src, dst);
		String diff = BootstrapGenerator.produceHTML(fsrc, fdst, src, dst, matcher);
		String f = inFolder + File.separatorChar + "diffs" + File.separatorChar + fileName(fsrc).replace("_v0.", "_diff.") + ".html";
		LOGGER.info("Generating file: " + f);
		FileWriter w = new FileWriter(f);
		w.append(diff);
		w.close();
	}
	
	private void ensureBootstrap() {
		ensureFolder("diffs");
		ensureFolder("diffs" + File.separatorChar + "assets");
		for (String res : BOOTSTRAP_RESOURCES) copyResource(res, "diffs" + File.separatorChar + res);
	}

}
