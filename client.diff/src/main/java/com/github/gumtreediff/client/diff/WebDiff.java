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

package com.github.gumtreediff.client.diff;

import com.github.gumtreediff.client.Option;
import com.github.gumtreediff.client.Register;
import com.github.gumtreediff.client.diff.ui.web.DiffServer;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.ServerRunner;
import com.github.gumtreediff.client.Option;
import com.github.gumtreediff.client.Register;
import com.github.gumtreediff.client.diff.ui.web.DiffServer;

import java.io.IOException;

@Register(description = "a web diff client", options = WebDiff.Options.class)
public class WebDiff extends AbstractDiffClient<WebDiff.Options> {

    public WebDiff(String[] args) {
        super(args);
    }

    static class Options extends AbstractDiffClient.Options {
        protected int defaultPort = Integer.parseInt(System.getProperty("gumtree.client.web.port", "4754"));
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
        DiffServer server = new DiffServer(opts.src, opts.dst, opts.defaultPort);
        System.out.println(String.format("Starting server: %s:%d", "http://127.0.0.1", opts.defaultPort));

        if (opts.stdin) {
            ServerRunner.executeInstance(server);
        } else {
            runServer(server);
        }
    }

    public void runServer(NanoHTTPD server) {
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Couldn't start server:\n" + e);
            return;
        }

        System.out.println("Server started, Ctrl-C to stop.\n");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // FIXME to be called you should send SIGTERM ... I have no clue if this signal is mapped to some keys
            System.out.println("Server stopped.\n");
            server.stop();
        }));
        try {
            while(!Thread.currentThread().isInterrupted())
                Thread.sleep(5000);
        } catch (Throwable ignored) {
        }
    }

}
