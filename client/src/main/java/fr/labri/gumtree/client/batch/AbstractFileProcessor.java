package fr.labri.gumtree.client.batch;

import java.io.File;
import java.io.IOException;

public abstract class AbstractFileProcessor extends AbstractProcessor {
	
	protected String outFolder;
	
	public final static String DEFAULT_OUT_FOLDER = "out";
	
	public AbstractFileProcessor(String inFolder, String outFolder) {
		super(inFolder);
		this.outFolder = outFolder;
		ensureFolder(outFolder);
	}
	
	public AbstractFileProcessor(String inFolder) {
		this(inFolder, inFolder + File.separatorChar + DEFAULT_OUT_FOLDER);
	}
	
	public void process() {
		init();
		File f = new File(inFolder);
		if (f.exists() && f.isDirectory()) {
			for (File file : FileUtils.listAllFiles(f))
				try {
					LOGGER.info("Processing file: " + file);
					if (file.isFile()) process(file.getAbsolutePath());
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		finish();
	}
	
	public abstract void process(String file) throws IOException ;
	
}
