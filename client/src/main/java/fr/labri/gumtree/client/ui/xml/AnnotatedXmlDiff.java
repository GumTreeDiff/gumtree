package fr.labri.gumtree.client.ui.xml;

import fr.labri.gumtree.client.DiffClient;
import fr.labri.gumtree.client.DiffOptions;
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
		Matcher m = getMatcher();
		String xml = TreeIoUtils.toAnnotatedXml((isSrc) ? m.getSrc() : m.getDst(), m.getMappings(), true);
		System.out.println(xml);
	}

}
