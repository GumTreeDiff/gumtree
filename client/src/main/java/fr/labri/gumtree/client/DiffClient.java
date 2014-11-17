package fr.labri.gumtree.client;

import java.io.IOException;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import fr.labri.gumtree.client.ui.swing.SwingDiff;
import fr.labri.gumtree.client.ui.web.WebDiff;
import fr.labri.gumtree.client.ui.xml.AnnotatedXmlDiff;
import fr.labri.gumtree.client.ui.xml.XmlDiff;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.MatcherFactories;
import fr.labri.gumtree.tree.ITree;
import fr.labri.gumtree.tree.TreeContext;

public abstract class DiffClient {
	
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
			else if ("xml".equals(diffOptions.getOutput())) client = new XmlDiff(diffOptions);
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
	
	protected Matcher getMatcher() {
		Matcher m = (diffOptions.getMatcher() == null) ? MatcherFactories.newMatcher(getSrcTree(), getDstTree()) : MatcherFactories.newMatcher(getSrcTree(), getDstTree(), diffOptions.getMatcher());
		m.match();
		return m;
	}
	
	protected ITree getSrcTree() {
		return getTree(diffOptions.getSrc());
	}
	
	protected ITree getDstTree() {
		return getTree(diffOptions.getDst());
	}
	
	protected TreeContext getSrcTreeContext() {
		return getTreeContext(diffOptions.getSrc());
	}
	
	protected TreeContext getDstTreeContext() {
		return getTreeContext(diffOptions.getDst());
	}
	
	
	private ITree getTree(String file) {
		try {
			TreeContext t = TreeGeneratorRegistry.getInstance().getTree(file, diffOptions.getGenerators());
			return t.getRoot();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
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
