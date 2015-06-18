package fr.labri.gumtree.client.diff;

import fr.labri.gumtree.client.Client;
import fr.labri.gumtree.client.Run;
import fr.labri.gumtree.client.diff.ui.swing.SwingDiff;
import fr.labri.gumtree.client.diff.ui.web.WebDiff;
import fr.labri.gumtree.client.diff.ui.xml.AnnotatedXmlDiff;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class Diff extends Client {

    DiffClient client;

    public Diff(String[] args) {
        super(args);
        DiffOptions diffOptions = new DiffOptions();
        CmdLineParser parser = new CmdLineParser(diffOptions);
        try {
            parser.parseArgument(args);

            if (diffOptions.isVerbose()) {
                System.out.printf("Current path: %s\n", System.getProperty("user.dir"));
                System.out.printf("Diff: %s %s\n", diffOptions.getSrc(), diffOptions.getDst());
            }

            if ("swing".equals(diffOptions.getOutput())) client = new SwingDiff(diffOptions);
            else if ("asrc".equals(diffOptions.getOutput())) client = new AnnotatedXmlDiff(diffOptions, true);
            else if ("adst".equals(diffOptions.getOutput())) client = new AnnotatedXmlDiff(diffOptions, false);
            else client = new WebDiff(diffOptions);
        } catch (CmdLineException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        client.start();
    }

    public static void main(String[] args) {
        Run.startClient(Diff.class, args);
    }
}
