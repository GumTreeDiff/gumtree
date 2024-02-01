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

package com.github.gumtreediff.client.diff.webdiff;

import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.client.Option;
import com.github.gumtreediff.client.Register;
import com.github.gumtreediff.client.diff.AbstractDiffClient;
import com.github.gumtreediff.utils.Registry;
import com.github.gumtreediff.io.DirectoryComparator;
import com.github.gumtreediff.utils.Pair;

import spark.Spark;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static spark.Spark.*;

@Register(description = "Web diff client", options = WebDiff.WebDiffOptions.class, priority = Registry.Priority.HIGH)
public class WebDiff extends AbstractDiffClient<WebDiff.WebDiffOptions> {
    public static final String JQUERY_JS_URL = "https://code.jquery.com/jquery-3.4.1.min.js";
    public static final String BOOTSTRAP_CSS_URL = "https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css";
    public static final String BOOTSTRAP_JS_URL = "https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js";

    public WebDiff(String[] args) {
        super(args);
    }

    public static class WebDiffOptions extends AbstractDiffClient.DiffOptions {
        public static final int DEFAULT_PORT = 4567;
        public int port = DEFAULT_PORT;

        @Override
        public Option[] values() {
            return Option.Context.addValue(super.values(),
                    new Option("--port", String.format("Set server port (default to %d).", DEFAULT_PORT), 1) {
                        @Override
                        protected void process(String name, String[] args) {
                            int p = Integer.parseInt(args[0]);
                            if (p > 0)
                                port = p;
                            else
                                System.err.printf("Invalid port number (%s), using %d.\n", args[0], port);
                        }
                    }
            );
        }
    }

    @Override
    protected WebDiffOptions newOptions() {
        return new WebDiffOptions();
    }

    @Override
    public void run() {
        DirectoryComparator comparator = new DirectoryComparator(opts.srcPath, opts.dstPath);
        comparator.compare();
        configureSpark(comparator, opts.port);
        Spark.awaitInitialization();
        System.out.println(String.format("Starting server: %s:%d.", "http://127.0.0.1", opts.port));
    }

    public void configureSpark(final DirectoryComparator comparator, int port) {
        port(port);
        staticFiles.location("/web/");
        get("/", (request, response) -> {
            if (comparator.isDirMode())
                response.redirect("/list");
            else
                response.redirect("/monaco-diff/0");
            return "";
        });
        get("/list", (request, response) -> {
            return DirectoryDiffView.build(comparator).renderFormatted();
        });
        get("/vanilla-diff/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Pair<File, File> pair = comparator.getModifiedFiles().get(id);
            Diff diff = getDiff(pair.first.getAbsolutePath(), pair.second.getAbsolutePath());
            return VanillaDiffView.build(pair.first, pair.second, diff, false).renderFormatted();
        });
        get("/monaco-diff/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Pair<File, File> pair = comparator.getModifiedFiles().get(id);
            Diff diff = getDiff(pair.first.getAbsolutePath(), pair.second.getAbsolutePath());
            return MonacoDiffView.build(pair.first, pair.second, diff, id).renderFormatted();
        });
        get("/monaco-native-diff/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Pair<File, File> pair = comparator.getModifiedFiles().get(id);
            return MonacoNativeDiffView.build(pair.first, pair.second, id).renderFormatted();
        });
        get("/mergely-diff/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            return MergelyDiffView.build(id).renderFormatted();
        });
        get("/raw-diff/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Pair<File, File> pair = comparator.getModifiedFiles().get(id);
            Diff diff = getDiff(pair.first.getAbsolutePath(), pair.second.getAbsolutePath());
            return TextDiffView.build(pair.first, pair.second, diff).renderFormatted();
        });
        get("/left/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Pair<File, File> pair = comparator.getModifiedFiles().get(id);
            return readFile(pair.first.getAbsolutePath(), Charset.defaultCharset());
        });
        get("/right/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Pair<File, File> pair = comparator.getModifiedFiles().get(id);
            return readFile(pair.second.getAbsolutePath(), Charset.defaultCharset());
        });
        get("/quit", (request, response) -> {
            System.exit(0);
            return "";
        });
    }

    private static String readFile(String path, Charset encoding)  throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
