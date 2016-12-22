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

package com.github.gumtreediff.client.diff.web;

import com.github.gumtreediff.client.Option;
import com.github.gumtreediff.client.Register;
import com.github.gumtreediff.client.diff.AbstractDiffClient;
import com.github.gumtreediff.gen.Registry;
import com.github.gumtreediff.io.DirectoryComparator;
import com.github.gumtreediff.utils.Pair;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;
import spark.Spark;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static spark.Spark.*;

@Register(description = "a web diff client", options = WebDiff.Options.class, priority = Registry.Priority.HIGH)
public class WebDiff extends AbstractDiffClient<WebDiff.Options> {

    public WebDiff(String[] args) {
        super(args);
    }

    static class Options extends AbstractDiffClient.Options {
        protected int defaultPort = Integer.parseInt(System.getProperty("gumtree.client.web.port", "4567"));
        boolean stdin = true;

        @Override
        public Option[] values() {
            return Option.Context.addValue(super.values(),
                    new Option("--port", String.format("set server port (default to)", defaultPort), 1) {
                        @Override
                        protected void process(String name, String[] args) {
                            int p = Integer.parseInt(args[0]);
                            if (p > 0)
                                defaultPort = p;
                            else
                                System.err.printf("Invalid port number (%s), using %d\n", args[0], defaultPort);
                        }
                    },
                    new Option("--no-stdin", String.format("Do not listen to stdin"), 0) {
                        @Override
                        protected void process(String name, String[] args) {
                            stdin = false;
                        }
                    }
            );
        }
    }

    @Override
    protected Options newOptions() {
        return new Options();
    }

    @Override
    public void run() {
        DirectoryComparator comparator = new DirectoryComparator(opts.src, opts.dst);
        comparator.compare();
        configureSpark(comparator, opts.defaultPort);
        Spark.awaitInitialization();
        System.out.println(String.format("Starting server: %s:%d", "http://127.0.0.1", opts.defaultPort));
    }

    public static void configureSpark(final DirectoryComparator comparator, int port) {
        port(port);
        staticFiles.location("/web/");
        get("/", (request, response) -> {
            if (comparator.isDirMode())
                response.redirect("/list");
            else
                response.redirect("/diff/0");
            return "";
        });
        get("/list", (request, response) -> {
            Renderable view = new DirectoryComparatorView(comparator);
            return render(view);
        });
        get("/diff/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Pair<File, File> pair = comparator.getModifiedFiles().get(id);
            Renderable view = new DiffView(pair.getFirst(), pair.getSecond());
            return render(view);
        });
        get("/mergely/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Renderable view = new MergelyView(id);
            return render(view);
        });
        get("/left/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Pair<File, File> pair = comparator.getModifiedFiles().get(id);
            return readFile(pair.getFirst().getAbsolutePath(), Charset.defaultCharset());
        });
        get("/right/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Pair<File, File> pair = comparator.getModifiedFiles().get(id);
            return readFile(pair.getSecond().getAbsolutePath(), Charset.defaultCharset());
        });
        get("/script/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Pair<File, File> pair = comparator.getModifiedFiles().get(id);
            Renderable view = new ScriptView(pair.getFirst(), pair.getSecond());
            return render(view);
        });
        get("/quit", (request, response) -> {
            System.exit(0);
            return "";
        });
    }

    private static String render(Renderable r) {
        HtmlCanvas c = new HtmlCanvas();
        try {
            r.renderOn(c);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return c.toHtml();
    }

    private static String readFile(String path, Charset encoding)  throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
