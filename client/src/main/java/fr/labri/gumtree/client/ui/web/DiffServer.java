package fr.labri.gumtree.client.ui.web;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import fr.labri.gumtree.actions.ActionGenerator;
import fr.labri.gumtree.client.ui.web.NanoHTTPD.Response.Status;
import fr.labri.gumtree.io.ActionsSerializer;
import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.tree.Tree;

public class DiffServer extends NanoHTTPD {

	public static final int PORT = 4754;
	
	private String fSrc;

	private String fDst;

	private Tree tSrc;

	private Tree tDst;

	private Matcher matcher;
	
	public DiffServer(String fSrc, String fDst, Tree tSrc, Tree tDst, Matcher matcher) {
		super(PORT);
		this.fSrc = fSrc;
		this.fDst = fDst;
		this.tSrc = tSrc;
		this.tDst = tDst;
		this.matcher = matcher;
	}
	
    public static void start(NanoHTTPD server) {
        try {
            server.start();
        } catch (IOException ioe) {
            System.err.println("Couldn't start server:" + ioe);
            System.exit(-1);
        }

        System.out.println("Server running on url: http://localhost:4754");
        try {
        	System.in.read();
        } catch (Throwable ignored) {
        }

        server.stop();
        System.out.println("Server stopped.\n");
    }

	@Override
	public Response serve(String uri, Method method, Map<String, String> header, Map<String, String> parms, Map<String, String> files) {
		System.out.println("Requested: " + uri);
		try {
			if ("/src".equals(uri))
				return respond("text/plain", new FileInputStream(fSrc));
			else if ("/src/xml".equals(uri)) 
				return respond("text/xml", TreeIoUtils.toXml(tSrc));
			else if ("/src/cxml".equals(uri)) 
				return respond("text/xml", TreeIoUtils.toCompactXml(tSrc));
			else if ("/src/dot".equals(uri)) 
				return respond("text/plain", TreeIoUtils.toDot(tSrc));
			else if ("/dst".equals(uri)) 
				return respond("text/plain", new FileInputStream(fDst));
			else if ("/dst/xml".equals(uri)) 
				return respond("text/xml", TreeIoUtils.toXml(tDst));
			else if ("/dst/cxml".equals(uri)) 
				return respond("text/xml", TreeIoUtils.toCompactXml(tDst));
			else if ("/dst/dot".equals(uri)) 
				return respond("text/plain", TreeIoUtils.toDot(tDst));
			else if ("/diff".equals(uri) || "/".equals(uri))
				return respond(BootstrapGenerator.produceHTML(fSrc, fDst, tSrc, tDst, matcher));
			else if ("/script".equals(uri)) {
				ActionGenerator g = new ActionGenerator(tSrc, tDst, matcher.getMappings());
				g.generate();
				return respond("text/plain", ActionsSerializer.toText(g.getActions()));
			} else if ("/quit".equals(uri)) System.exit(0);
			else if (uri.startsWith("/assets")) {
				String res = uri.substring(1);
				InputStream data = ClassLoader.getSystemClassLoader().getResourceAsStream(res);
				if (uri.endsWith(".css")) return new Response(Status.OK, "text/css", data);
				else if (uri.endsWith(".js")) return new Response(Status.OK, "text/javascript", data);
			} 
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

	private Response respond(String s) {
		return new Response(s);
	}
	
	private Response respond(String mimeType, String s) {
		return new Response(Status.OK, mimeType, s);
	}

	private Response respond(String mimeType, InputStream s) {
		return new Response(Status.OK, mimeType, s);
	}

}
