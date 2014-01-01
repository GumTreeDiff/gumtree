package fr.labri.gumtree.gen.c;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import fr.labri.gumtree.io.TreeGenerator;
import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.tree.Tree;

public class CTreeGenerator extends TreeGenerator {
	
	private static final String COCCI_CMD = "spatch";

	@Override
	public Tree generate(String file) {
		File f = new File(file);
		ProcessBuilder b = new ProcessBuilder(COCCI_CMD, f.getAbsolutePath());
		b.directory(f.getParentFile());
		try {
			Process p = b.start();
			StringBuffer buf = new StringBuffer();
			BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
	        while ((line = r.readLine()) != null)
	            buf.append(line + "\n");
	        p.waitFor();
			if (p.exitValue() != 0)  throw new RuntimeException();
	        r.close();
	        String xml = buf.toString();
	        Tree t = TreeIoUtils.fromXmlString(xml);
            return t;
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean handleFile(String file) {
		return file.toLowerCase().endsWith(".c") || file.toLowerCase().endsWith(".h");
	}

	@Override
	public String getName() {
		return "c-cocci";
	}
}
