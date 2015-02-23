package fr.labri.gumtree.client.ui.swing;

import javax.swing.JFrame;

import fr.labri.gumtree.client.DiffClient;
import fr.labri.gumtree.client.DiffOptions;
import fr.labri.gumtree.matchers.Matcher;

public final class SwingDiff extends DiffClient {

	public SwingDiff(DiffOptions diffOptions) {
		super(diffOptions);
	}

	@Override
	public void start() {
		final Matcher matcher = matchTrees();
		javax.swing.SwingUtilities.invokeLater(new Runnable() { public void run() { 
			JFrame frame = new JFrame("GumTree");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.add(new MappingsPanel(diffOptions.getSrc(), diffOptions.getDst(), getSrcTreeContext(), getDstTreeContext(), matcher));
			frame.pack();
			frame.setVisible(true);
		} });
	}
	
}