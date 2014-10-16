package fr.labri.gumtree.client.ui.xml;

import java.io.IOException;

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
		String xml;
		try {
			xml = TreeIoUtils.toAnnotatedXML((isSrc) ? getSrcTreeContext() : getDstTreeContext(), true, m.getMappings());
			System.out.println(xml);
		} catch (IOException e) {}
	}

}
