package fr.labri.gumtree.client.batch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public abstract class AbstractProcessor {
	
	public final static Logger LOGGER = Logger.getLogger("fr.labri.gumtree.client.batch");
	
	protected String inFolder;
	
	public AbstractProcessor(String folder) {
		this.inFolder = folder;
	}
	
	public abstract void process();
	
	protected void init() {};
	
	protected void finish() {};
	
	protected void ensureFolder(String name) {
		File f = new File(inFolder + File.separatorChar + name);
		if (f.exists() && !f.isDirectory()) f.delete();
		if (!f.exists()) f.mkdir();
	}
	
	protected String nextFile(String folder, String prefix, String ext) {
		int nb = 0;
		Set<String> files = new HashSet<>();
		for (String f: new File(this.inFolder + File.separatorChar + folder).list()) files.add(f);
		String next = prefix + "_" + nb + "." + ext;
		while (files.contains(next)) next = prefix + "_" + ++nb + "." + ext;
		return this.inFolder + File.separatorChar + folder + File.separatorChar + next;
	}
	
	protected void copyResource(String url, String dest) {
		try {
			InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(url);
			FileOutputStream out = new FileOutputStream(inFolder + File.separatorChar + dest);
			int read = 0;
			byte[] bytes = new byte[1024];
			while ((read = in.read(bytes)) != -1)
				out.write(bytes, 0, read);
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected String fileName(String file) {
		return new File(file).getName();
	}
	
	protected String fileExtension(String file) {
		return file.substring(file.lastIndexOf("."));
	}

	protected long tic() {
		return System.currentTimeMillis();
	}
	
}
