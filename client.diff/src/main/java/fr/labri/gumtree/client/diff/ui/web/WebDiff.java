package fr.labri.gumtree.client.diff.ui.web;

import fr.labri.gumtree.client.diff.DiffClient;
import fi.iki.elonen.ServerRunner;
import fr.labri.gumtree.client.diff.DiffOptions;

public class WebDiff extends DiffClient {

	public static final int DEFAULT_PORT = 4754;

	private DiffServer server;

	public WebDiff(DiffOptions diffOptions) {
		super(diffOptions);
		server = new DiffServer(diffOptions.getSrc(), diffOptions.getDst(), DEFAULT_PORT);
	}

	@Override
	public void start() {
		System.out.println(String.format("Starting server: %s", "http://127.0.0.1:" + DEFAULT_PORT));
		ServerRunner.executeInstance(server);
	}

}