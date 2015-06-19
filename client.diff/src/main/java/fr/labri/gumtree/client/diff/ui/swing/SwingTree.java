package fr.labri.gumtree.client.diff.ui.swing;

import fr.labri.gumtree.gen.Generators;
import fr.labri.gumtree.tree.TreeContext;

import javax.swing.*;
import java.io.IOException;

public final class SwingTree {

	public static void main(String[] args) throws IOException {
		final TreeContext t = Generators.getInstance().getTree(args[0]);
		javax.swing.SwingUtilities.invokeLater(new Runnable() { public void run() { createAndShowGUI(t); } });
	}

	private SwingTree() {
	}

	private static void createAndShowGUI(TreeContext tree) {
		JFrame frame = new JFrame("Tree Viewer");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(new TreePanel(tree));
		frame.pack();
		frame.setVisible(true);
	}
}