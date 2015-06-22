package fr.labri.gumtree.client.diff;

import fr.labri.gumtree.client.Register;
import fr.labri.gumtree.client.diff.ui.swing.MappingsPanel;
import fr.labri.gumtree.matchers.Matcher;

import javax.swing.*;

@Register(description = "A swing diff client", options = AbstractDiffClient.Options.class)
public final class SwingDiff extends AbstractDiffClient<AbstractDiffClient.Options> {

    public SwingDiff(String[] args) {
        super(args);
    }

    @Override
    public void run() {
        final Matcher matcher = matchTrees();
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("GumTree");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(new MappingsPanel(opts.src, opts.dst, getSrcTreeContext(), getDstTreeContext(), matcher));
                frame.pack();
                frame.setVisible(true);
            }
        });
    }

    @Override
    protected AbstractDiffClient.Options newOptions() {
        return new AbstractDiffClient.Options();
    }
}