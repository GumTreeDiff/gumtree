package fr.labri.gumtree.client.ui.web;

import fi.iki.elonen.ServerRunner;
import fr.labri.gumtree.client.DiffClient;
import fr.labri.gumtree.client.DiffOptions;

import java.io.IOException;

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