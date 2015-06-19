package fr.labri.gumtree.client.diff;

import fr.labri.gumtree.client.Client;
import fr.labri.gumtree.client.Option;
import fr.labri.gumtree.gen.Generators;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.Matchers;
import fr.labri.gumtree.tree.TreeContext;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

public abstract class AbstractDiffClient<O extends AbstractDiffClient.Options> extends Client {

    protected final O opts;
    public static final String SYNTAX = "Syntax: diff [options] fileSrc fileDst";
    private TreeContext src, dst;

    public static class Options implements Option.Context {
        protected String matcher;
        protected ArrayList<String> generators = new ArrayList<>();
        protected String src, dst;

        @Override
        public Option[] values() {
            return new Option[]{
                    new Option("-m", "The qualified name of the class implementing the matcher.", 1){
                        @Override
                        protected void process(String name, String[] args) {
                            matcher = args[0];
                        }
                    },
                    new Option("-g", "Preferred generator to use (can be used more than once).", 1){
                        @Override
                        protected void process(String name, String[] args) {
                            generators.add(args[0]);
                        }
                    },
                    new Option.Help(this) {
                        @Override
                        public void process(String name, String[] args) {
                            System.out.println(SYNTAX);
                            super.process(name, args);
                        }
                    }
            };
        }

        void dump(PrintStream out) {
            out.printf("Current path: %s\n", System.getProperty("user.dir"));
            out.printf("Diff: %s %s\n", src, dst);
        }
    }

    abstract protected O newOptions();

    public AbstractDiffClient(String[] args){
        super(args);
        opts = newOptions();
        args = Option.processCommandLine(args, opts);

        if (args.length < 2)
            throw new Option.OptionException("arguments required." + SYNTAX, opts);

        opts.src = args[0];
        opts.dst = args[1];

        if (Option.Verbose.verbose) {
            opts.dump(System.out);
        }
    }

    ///////////////////
    // TODO after this line it should be rewrote in a better way
    private Matcher matcher;
    protected Matcher matchTrees() {
        Matchers matchers = Matchers.getInstance();
        if (matcher != null)
            return matcher;
        matcher = (opts.matcher == null)
                ? matchers.getMatcher(getSrcTreeContext().getRoot(), getDstTreeContext().getRoot())
                : matchers.getMatcher(opts.matcher, getSrcTreeContext().getRoot(), getDstTreeContext().getRoot());
        matcher.match();
        return matcher;
    }

    protected TreeContext getSrcTreeContext() {
        if (src == null)
            src = getTreeContext(opts.src);
        return src;
    }

    protected TreeContext getDstTreeContext() {
        if (dst == null)
            dst = getTreeContext(opts.dst);
        return dst;
    }

    private TreeContext getTreeContext(String file) {
        try {
            TreeContext t = Generators.getInstance().getTree(file);
            return t;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
