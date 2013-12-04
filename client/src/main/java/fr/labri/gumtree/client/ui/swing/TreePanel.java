package fr.labri.gumtree.client.ui.swing;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;

import fr.labri.gumtree.tree.Tree;

public class TreePanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	private JTree jtree;
	private Tree tree;
	private Map<Tree, DefaultMutableTreeNode> trees;

	public TreePanel(Tree tree, TreeCellRenderer renderer) {
		super(new GridLayout(1, 0));
		trees = new HashMap<Tree, DefaultMutableTreeNode>();
		this.tree = tree;

		DefaultMutableTreeNode top = new DefaultMutableTreeNode(tree);
		trees.put(tree, top);
		for (Tree child: tree.getChildren()) createNodes(top, child);
		
		jtree = new JTree(top);
		jtree.setCellRenderer(renderer);
		jtree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		JScrollPane treeView = new JScrollPane(jtree);
		Dimension minimumSize = new Dimension(100, 50);
		treeView.setMinimumSize(minimumSize);

		add(treeView);
	}
	
	public TreePanel(Tree tree) {
		this(tree, new DefaultTreeCellRenderer());
	}
	
	public JTree getJTree() {
		return jtree;
	}
	
	public Map<Tree, DefaultMutableTreeNode> getTrees() {
		return trees;
	}
	
	public Tree getTree() {
		return this.tree;
	}

	public void valueChanged(TreeSelectionEvent e) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) jtree.getLastSelectedPathComponent();
		if (node == null) return;
		Object nodeInfo = node.getUserObject();
		System.out.println(nodeInfo);
	}
	
	private void createNodes(DefaultMutableTreeNode parent, Tree tree) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(tree);
		trees.put(tree, node);
		parent.add(node);
		for (Tree child: tree.getChildren()) createNodes(node, child);
	}

}
