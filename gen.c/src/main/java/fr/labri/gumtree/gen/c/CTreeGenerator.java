package fr.labri.gumtree.gen.c;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import fr.labri.gumtree.io.TreeGenerator;
import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.tree.TreeContext;

public class CTreeGenerator extends TreeGenerator {

	private static final String COCCI_CMD = "cgum";

	@Override
	public TreeContext generate(Reader r) throws IOException {
		//FIXME this is not efficient but I am not sure how to speed up things here.
		File f = File.createTempFile("gumtree", ".c");
		System.out.println(f.getAbsolutePath());
		FileWriter w = new FileWriter(f);
		BufferedReader br = new BufferedReader(r);
		String line = br.readLine();
        while (line != null) {
            w.append(line);
            w.append(System.lineSeparator());
            line = br.readLine();
        }
		w.close();
		br.close();
		ProcessBuilder b = new ProcessBuilder(COCCI_CMD, f.getAbsolutePath());
		b.directory(f.getParentFile());
		try {
			Process p = b.start();
			StringBuffer buf = new StringBuffer();
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			line = null;
			while ((line = br.readLine()) != null)
				buf.append(line + "\n");
			p.waitFor();
			if (p.exitValue() != 0)  throw new RuntimeException();
			r.close();
			String xml = buf.toString();
			return TreeIoUtils.fromXmlString(xml);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			f.delete();
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
