package fr.labri.gumtree.client.batch;

import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.logging.Logger;

public abstract class BatchProcessor extends SimpleFileVisitor<Path> {
	
	public final static Logger LOGGER = Logger.getLogger("fr.labri.gumtree.client.batch");
	
	protected String outputFolder;
	
	public BatchProcessor(String outputFolder){
		this.outputFolder = outputFolder;
		BatchUtils.ensureFolder(outputFolder);
	}
	
}
