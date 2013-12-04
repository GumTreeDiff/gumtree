package fr.labri.gumtree.client.ui.web;

import fr.labri.gumtree.client.DiffClient;
import fr.labri.gumtree.client.DiffOptions;
import fr.labri.gumtree.matchers.composite.Matcher;

public class WebDiff extends DiffClient {

	public WebDiff(DiffOptions diffOptions) {
		super(diffOptions);
	}

	@Override
	public void start() {
		Matcher matcher = getMatcher();
		DiffServer server = new DiffServer(diffOptions.getSrc(), diffOptions.getDst(), matcher.getSrc(), matcher.getDst(), matcher);
		DiffServer.start(server);
	}

}