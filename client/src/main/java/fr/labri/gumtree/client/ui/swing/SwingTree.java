package fr.labri.gumtree.client.ui.swing;

import java.io.IOException;

import javax.swing.JFrame;

import fr.labri.gumtree.client.TreeGeneratorRegistry;
import fr.labri.gumtree.tree.Tree;

public final class SwingTree {
	
	public static void main(String[] args) throws IOException {
		final Tree t = TreeGeneratorRegistry.getInstance().getTree(args[0]);
		javax.swing.SwingUtilities.invokeLater(new Runnable() { public void run() { createAndShowGUI(t); } });
	}
	
	private SwingTree() {
	}

	private static void createAndShowGUI(Tree tree) {
		JFrame frame = new JFrame("Tree Viewer");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(new TreePanel(tree));
		frame.pack();
		frame.setVisible(true);
	}
}