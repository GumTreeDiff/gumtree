package fr.labri.gumtree.client.ui.web;

import fr.labri.gumtree.client.DiffClient;
import fr.labri.gumtree.client.DiffOptions;

public class WebDiff extends DiffClient {

	public WebDiff(DiffOptions diffOptions) {
		super(diffOptions);
	}

	@Override
	public void start() {
		DiffServer server = new DiffServer(diffOptions.getSrc(), diffOptions.getDst());
		DiffServer.start(server);
	}

}