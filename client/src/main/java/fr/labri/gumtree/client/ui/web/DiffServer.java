package fr.labri.gumtree.client.ui.web;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import fr.labri.gumtree.client.ui.web.NanoHTTPD.Response.Status;
import fr.labri.gumtree.client.ui.web.views.DirectoryComparatorView;
import fr.labri.gumtree.client.ui.web.views.DiffView;
import fr.labri.gumtree.client.ui.web.views.ScriptView;
import fr.labri.gumtree.io.DirectoryComparator;
import fr.labri.gumtree.tree.Pair;

public class DiffServer extends NanoHTTPD {

	public static final int PORT = 4754;
	
	public DirectoryComparator comparator;
	
	public DiffServer(String src, String dst) {
		super(PORT);
		comparator = new DirectoryComparator(src, dst);
		comparator.compare();
	}
	
    public static void start(NanoHTTPD server) {
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Couldn't start server:" + e);
            System.exit(-1);
        }

        System.out.println("Server running on url: http://localhost:4754");
        try {
        	System.in.read();
        } catch (Throwable ignored) {
        	ignored.printStackTrace();
        }

        server.stop();
        System.out.println("Server stopped.\n");
    }

	@Override
	public Response serve(String uri, Method method, Map<String, String> header, Map<String, String> parms, Map<String, String> files) {
		try {
			if ("/list".equals(uri)) {
				DirectoryComparatorView view = new DirectoryComparatorView(comparator);
				return respond(view);
			} else if ("/diff".equals(uri)) {
				int id = Integer.parseInt(parms.get("id"));
				Pair<File, File> pair = comparator.getModifiedFiles().get(id);
				return respond(new DiffView(pair.getFirst(), pair.getSecond()));
			} else if ("/script".equals(uri)) {
				int id = Integer.parseInt(parms.get("id"));
				Pair<File, File> pair = comparator.getModifiedFiles().get(id);
				return respond(new ScriptView(pair.getFirst(), pair.getSecond()));
			} else if ("/quit".equals(uri)) System.exit(0);
			else if (uri.startsWith("/res/")) {
				String res = uri.substring(1);
				InputStream data = ClassLoader.getSystemClassLoader().getResourceAsStream(res);
				if (uri.endsWith(".css")) return new Response(Status.OK, "text/css", data);
				else if (uri.endsWith(".js")) return new Response(Status.OK, "text/javascript", data);
			} else if ("/".equals(uri)) {
				if (comparator.isDirMode()) return serve("/list", method, header, parms, files);
				else {
					parms = new HashMap<String, String>();
					parms.put("id", "0");
					return serve("/diff", method, header, parms, files);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private Response respond(Renderable r) {
		HtmlCanvas c = new HtmlCanvas();
		try {
			r.renderOn(c);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new Response(c.toHtml());
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
