package fr.labri.gumtree.client;

import java.io.IOException;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import fr.labri.gumtree.client.ui.swing.SwingDiff;
import fr.labri.gumtree.client.ui.web.WebDiff;
import fr.labri.gumtree.client.ui.xml.AnnotatedXmlDiff;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.MatcherFactories;
import fr.labri.gumtree.tree.TreeContext;

public abstract class DiffClient {
	TreeContext src, dst;
	Matcher matcher;
	
	public static void main(String[] args) {
		DiffOptions diffOptions = new DiffOptions();
		CmdLineParser parser = new CmdLineParser(diffOptions);
		try {
			parser.parseArgument(args);

			if (diffOptions.isVerbose()) {
				System.out.printf("Current path: %s\n", System.getProperty("user.dir"));
				System.out.printf("Diff: %s %s\n", diffOptions.getSrc(), diffOptions.getDst());
			}

			DiffClient client;
			if ("swing".equals(diffOptions.getOutput())) client = new SwingDiff(diffOptions);
			else if ("asrc".equals(diffOptions.getOutput())) client = new AnnotatedXmlDiff(diffOptions, true);
			else if ("adst".equals(diffOptions.getOutput())) client = new AnnotatedXmlDiff(diffOptions, false);
			else client = new WebDiff(diffOptions);
			client.start();
		} catch (CmdLineException e) {
			e.printStackTrace();
		}
	}
	
	protected DiffOptions diffOptions;	
	
	public DiffClient(DiffOptions diffOptions) {
		this.diffOptions = diffOptions;
	}
	
	public abstract void start();
	
	protected Matcher matchTrees() {
		if (matcher != null)
			return matcher;
		matcher = (diffOptions.getMatcher() == null)
				? MatcherFactories.newMatcher(getSrcTreeContext().getRoot(), getDstTreeContext().getRoot())
				: MatcherFactories.newMatcher(getSrcTreeContext().getRoot(), getDstTreeContext().getRoot(), diffOptions.getMatcher());
		matcher.match();
		return matcher;
	}

	protected TreeContext getSrcTreeContext() {
		if (src == null)
			src = getTreeContext(diffOptions.getSrc());
		return src;
	}
	protected TreeContext getDstTreeContext() {
		if (dst == null)
			dst = getTreeContext(diffOptions.getDst());
		return dst;
	}
	
	private TreeContext getTreeContext(String file) {
		try {
			TreeContext t = TreeGeneratorRegistry.getInstance().getTree(file, diffOptions.getGenerators());
			return t;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
