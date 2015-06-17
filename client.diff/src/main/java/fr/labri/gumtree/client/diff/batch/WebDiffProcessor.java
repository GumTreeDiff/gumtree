package fr.labri.gumtree.client.diff.batch;

import fr.labri.gumtree.client.batch.BatchProcessor;
import fr.labri.gumtree.client.batch.BatchUtils;
import fr.labri.gumtree.client.diff.ui.web.views.DiffView;
import org.rendersnake.HtmlCanvas;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public final class WebDiffProcessor extends BatchProcessor {

	private static final String[] BOOTSTRAP_RESOURCES = new String[] {
		"res/web/list.js", "res/web/diff.js", "res/web/script.js", "res/web/gumtree.css", "res/web/bootstrap.min.js", "res/web/bootstrap.min.css", "res/web/jquery.min.js"
	};

	public static void main(String[] args) throws IOException {
		File in = new File(args[0]);
		Files.walkFileTree(in.toPath(), new WebDiffProcessor(args[1]));
	}

	public WebDiffProcessor(String outputFolder) {
		super(outputFolder);
		BatchUtils.ensureFolder(outputFolder + File.separatorChar + "res" + File.separatorChar + "res/web");
		for (String res : BOOTSTRAP_RESOURCES)
			BatchUtils.copyResource(res, outputFolder + File.separatorChar + res);
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		File[] files = dir.toFile().listFiles();
		if (files.length == 2 && files[1].getName().startsWith("src_") &&
				files[0].getName().startsWith("dst_")) {
			File src = files[1];
			File dst = files[0];

			LOGGER.info(String.format("Processing %s and %s", src.getAbsolutePath(), dst.getAbsolutePath()));

			DiffView v = new DiffView(src, dst);
			HtmlCanvas c = new HtmlCanvas();
			v.renderOn(c);
			String out = outputFolder + File.separatorChar + src.getName() + ".html";
			File f = new File(out);
			LOGGER.info(String.format("Saving result to %s", f.getAbsolutePath()));
			FileWriter w = new FileWriter(out);
			w.append(c.toHtml());
			w.close();
		}
		return FileVisitResult.CONTINUE;
	}

}
