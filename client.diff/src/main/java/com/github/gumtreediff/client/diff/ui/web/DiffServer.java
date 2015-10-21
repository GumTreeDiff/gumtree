/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.client.diff.ui.web;

import com.github.gumtreediff.tree.Pair;
import fi.iki.elonen.NanoHTTPD;
import com.github.gumtreediff.client.diff.ui.web.views.DiffView;
import com.github.gumtreediff.client.diff.ui.web.views.DirectoryComparatorView;
import com.github.gumtreediff.client.diff.ui.web.views.ScriptView;
import com.github.gumtreediff.io.DirectoryComparator;
import com.github.gumtreediff.tree.Pair;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class DiffServer extends NanoHTTPD {

    public DirectoryComparator comparator;

    public DiffServer(String src, String dst, int port) {
        super(port);
        comparator = new DirectoryComparator(src, dst);
        comparator.compare();
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Map<String, String> parms = session.getParms();
        try {
            if ("/list".equals(uri) || ("/".equals(uri) && comparator.isDirMode())) {
                DirectoryComparatorView view = new DirectoryComparatorView(comparator);
                return respond(view);
            } else if ("/diff".equals(uri) || ("/".equals(uri) && !comparator.isDirMode())) {
                int id = 0;
                if (parms.containsKey("id"))
                    id = Integer.parseInt(parms.get("id"));
                Pair<File, File> pair = comparator.getModifiedFiles().get(id);
                return respond(new DiffView(pair.getFirst(), pair.getSecond()));
            } else if ("/script".equals(uri)) {
                int id = Integer.parseInt(parms.get("id"));
                Pair<File, File> pair = comparator.getModifiedFiles().get(id);
                return respond(new ScriptView(pair.getFirst(), pair.getSecond()));
            } else if ("/quit".equals(uri))
                System.exit(0);
            else if (uri.startsWith("/res/")) {
                String res = uri.substring(1);
                InputStream data = ClassLoader.getSystemClassLoader().getResourceAsStream(res);
                if (uri.endsWith(".css")) return new Response(Response.Status.OK, "text/css", data);
                else if (uri.endsWith(".js")) return new Response(Response.Status.OK, "text/javascript", data);
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
        return new Response(Response.Status.OK, mimeType, s);
    }

    private Response respond(String mimeType, InputStream s) {
        return new Response(Response.Status.OK, mimeType, s);
    }

}
