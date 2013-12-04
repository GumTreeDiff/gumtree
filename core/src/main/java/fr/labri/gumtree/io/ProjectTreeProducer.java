package fr.labri.gumtree.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.labri.gumtree.tree.Tree;
import fr.labri.utils.file.FileUtils;

public class ProjectTreeProducer {

	private TreeGenerator p;

	public ProjectTreeProducer(TreeGenerator p) {
		this.p = p;
	}

	public Tree generate(String folder) throws IOException {
		List<String> files = new ArrayList<>();
		FileUtils.listAllFiles(new File(folder), files, "");
		Collections.sort(files);
		Tree project = new Tree(-1, folder, "SourceFolder");
		for (String file: files) {
			if (p.handleFile(file)) {
				Tree t = p.doGenerate(file);
				Tree f = new Tree(-2, file, "SourceFile");
				project.addChild(f);
				f.addChild(t);
			}
		}
		return project;
	}

}
