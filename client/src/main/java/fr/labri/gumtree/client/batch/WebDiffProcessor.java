package fr.labri.gumtree.client.batch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.rendersnake.HtmlCanvas;

import fr.labri.gumtree.client.ui.web.views.DiffView;

public final class WebDiffProcessor extends AbstractFilePairsProcessor {
	
	public static void main(String[] args) {
		WebDiffProcessor g = new WebDiffProcessor(args[0]);
		g.process();
	}
	
	private static final String[] BOOTSTRAP_RESOURCES = new String[] { 
		"res/web/list.js", "res/web/diff.js", "res/web/script.js", "res/web/gumtree.css", "res/web/bootstrap.min.js", "res/web/bootstrap.min.css", "res/web/jquery.min.js"
	};
	
	public WebDiffProcessor(String folder) {
		super(folder);
		ensureBootstrap();
	}
	
	@Override
	public void processFilePair(String fsrc, String fdst) throws IOException {
		DiffView v = new DiffView(new File(fsrc), new File(fdst));
		HtmlCanvas c = new HtmlCanvas();
		v.renderOn(c);
		String f = inFolder + File.separatorChar + "diffs" + File.separatorChar + fileName(fsrc).replace("_v0.", "_diff.") + ".html";
		LOGGER.info("Generating file: " + f);
		FileWriter w = new FileWriter(f);
		w.append(c.toHtml());
		w.close();
	}
	
	private void ensureBootstrap() {
		ensureFolder("diffs");
		ensureFolder("diffs" + File.separatorChar + "res");
		ensureFolder("diffs" + File.separatorChar + "res" + File.separatorChar + "web");
		for (String res : BOOTSTRAP_RESOURCES) copyResource(res, "diffs" + File.separatorChar + res);
	}

}
