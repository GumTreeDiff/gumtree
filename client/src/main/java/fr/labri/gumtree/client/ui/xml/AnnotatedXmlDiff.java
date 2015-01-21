package fr.labri.gumtree.client.ui.xml;

import fr.labri.gumtree.client.DiffClient;
import fr.labri.gumtree.client.DiffOptions;
import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.tree.Tree;

public class AnnotatedXmlDiff extends DiffClient {

	private boolean isSrc; 
	
	public AnnotatedXmlDiff(DiffOptions diffOptions, boolean isSrc) {
		super(diffOptions);
		this.isSrc = isSrc;
	}

	@Override
	public void start() {
		Matcher m = matchTrees();
		try {
			TreeIoUtils.toAnnotatedXml((isSrc) ? getSrcTreeContext() : getDstTreeContext(), isSrc, m.getMappings()).writeTo(System.out);
		} catch (Exception e) {}
	}
}
