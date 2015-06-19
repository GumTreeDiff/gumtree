package fr.labri.gumtree.client.diff;

import fi.iki.elonen.ServerRunner;
import fr.labri.gumtree.client.Option;
import fr.labri.gumtree.client.Register;
import fr.labri.gumtree.client.diff.ui.web.DiffServer;

@Register(description = "a web diff client", options = WebDiff.Options.class)
public class WebDiff extends AbstractDiffClient<WebDiff.Options> {

    public WebDiff(String[] args) {
        super(args);
    }

    static class Options extends AbstractDiffClient.Options{
        protected int defaultPort = Integer.parseInt(System.getProperty("gumtree.client.web.port", "4754"));

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
        System.out.println(String.format("Starting server: %s", "http://127.0.0.1:" + opts.defaultPort));
        ServerRunner.executeInstance(server);
    }
}