package fr.labri.gumtree.client.ui.xml;

import fr.labri.gumtree.actions.ActionGenerator;
import fr.labri.gumtree.client.DiffClient;
import fr.labri.gumtree.client.DiffOptions;
import fr.labri.gumtree.io.ActionsIoUtils;
import fr.labri.gumtree.matchers.Matcher;

public class XmlDiff extends DiffClient {

	public XmlDiff(DiffOptions diffOptions) {
		super(diffOptions);
	}

	@Override
	public void start() {
		Matcher m = matchTrees();
		ActionGenerator g = new ActionGenerator(m.getSrc(), m.getDst(), m.getMappings());
		g.generate();
		String xml = ActionsIoUtils.toXml(getSrcTreeContext(), g.getActions(), m.getMappings());
		System.out.println(xml);
	}
}
