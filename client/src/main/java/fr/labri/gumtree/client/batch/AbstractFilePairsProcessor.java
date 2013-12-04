package fr.labri.gumtree.client.batch;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import fr.labri.gumtree.tree.Pair;

public abstract class AbstractFilePairsProcessor extends AbstractProcessor {
	
	public AbstractFilePairsProcessor(String folder) {
		super(folder);
	}
	
	public void process() {
		init();
		List<Pair<String, String>> filePairs = new ArrayList<>();
		for (File src : new TreeSet<File>(Arrays.asList(new File(inFolder).listFiles(new FilenameFilter() { @Override public boolean accept(File dir, String name) { return name.contains("_v0."); } })))) {
			String dst = src.getAbsolutePath().replace("_v0.", "_v1.");
			if (src.exists() && new File(dst).exists())
				filePairs.add(new Pair<String, String>(src.getAbsolutePath(), dst));
		}
		
		for (Pair<String, String> filePair : filePairs)
			try {
				LOGGER.info("Processing files:\n\t" + filePair.getFirst() + "\n\t" + filePair.getSecond());
				processFilePair(filePair.getFirst(), filePair.getSecond());
			} catch (IOException e) {
				e.printStackTrace();
			}
		
		finish();
	}
	
	public abstract void processFilePair(String source, String dest) throws IOException;

}
