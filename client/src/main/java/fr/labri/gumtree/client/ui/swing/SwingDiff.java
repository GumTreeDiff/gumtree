package fr.labri.gumtree.client.ui.swing;

import javax.swing.JFrame;

import fr.labri.gumtree.client.DiffClient;
import fr.labri.gumtree.client.DiffOptions;
import fr.labri.gumtree.matchers.Matcher;

public final class SwingDiff extends DiffClient {

	public SwingDiff(DiffOptions diffOptions) {
		super(diffOptions);
	}

	private static void createAndShowGUI(String src, String dst, Matcher m) {
		JFrame frame = new JFrame("GumTree");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(new MappingsPanel(src, dst, m.getSrc(), m.getDst(), m));
		frame.pack();
		frame.setVisible(true);
	}

	@Override
	public void start() {
		final Matcher matcher = getMatcher();
		javax.swing.SwingUtilities.invokeLater(new Runnable() { public void run() { createAndShowGUI(diffOptions.getSrc(), diffOptions.getDst(), matcher); } });
	}
	
}