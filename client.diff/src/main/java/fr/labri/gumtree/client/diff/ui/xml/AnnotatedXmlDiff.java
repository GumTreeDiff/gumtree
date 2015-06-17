package fr.labri.gumtree.client.diff.ui.xml;

import fr.labri.gumtree.client.diff.DiffClient;
import fr.labri.gumtree.client.diff.DiffOptions;
import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.matchers.Matcher;

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
