package fr.labri.gumtree.client.diff;

import fr.labri.gumtree.client.Option;
import fr.labri.gumtree.client.Register;
import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.matchers.Matcher;

@Register(name = "axmldiff", description = "Dump annotated xml tree",
        experimental = true, options = AbstractDiffClient.Options.class)
public abstract class AnnotatedXmlDiff extends AbstractDiffClient<AnnotatedXmlDiff.Options> {

    public AnnotatedXmlDiff(String[] args) {
        super(args);
    }

    static class Options extends AbstractDiffClient.Options{
        protected boolean isSrc = true;

        @Override
        public Option[] values() {
            return Option.Context.addValue(super.values(),
                    new Option("--src", String.format("Dump source tree (default: %s)", isSrc ? "yes" : "no"), 0) {
                        @Override
                        protected void process(String name, String[] args) {
                            isSrc = true;
                        }
                    },
                    new Option("--dst", String.format("Dump destination tree (default: %s)", !isSrc
                                    ? "yes" : "no"), 0) {
                        @Override
                        protected void process(String name, String[] args) {
                            isSrc = false;
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
        Matcher m = matchTrees();
        try {
            TreeIoUtils.toAnnotatedXml((opts.isSrc)
                            ? getSrcTreeContext()
                            : getDstTreeContext(), opts.isSrc, m.getMappings()).writeTo(System.out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
