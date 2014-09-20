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

import fr.labri.gumtree.tree.ITree;

public class TreePanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	private JTree jtree;
	private ITree tree;
	private Map<ITree, DefaultMutableTreeNode> trees;

	public TreePanel(ITree tree, TreeCellRenderer renderer) {
		super(new GridLayout(1, 0));
		trees = new HashMap<>();
		this.tree = tree;

		DefaultMutableTreeNode top = new DefaultMutableTreeNode(tree);
		trees.put(tree, top);
		for (ITree child: tree.getChildren()) createNodes(top, child);
		
		jtree = new JTree(top);
		jtree.setCellRenderer(renderer);
		jtree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		JScrollPane treeView = new JScrollPane(jtree);
		Dimension minimumSize = new Dimension(100, 50);
		treeView.setMinimumSize(minimumSize);

		add(treeView);
	}
	
	public TreePanel(ITree tree) {
		this(tree, new DefaultTreeCellRenderer());
	}
	
	public JTree getJTree() {
		return jtree;
	}
	
	public Map<ITree, DefaultMutableTreeNode> getTrees() {
		return trees;
	}
	
	public ITree getTree() {
		return this.tree;
	}

	public void valueChanged(TreeSelectionEvent e) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) jtree.getLastSelectedPathComponent();
		if (node == null) return;
		Object nodeInfo = node.getUserObject();
		System.out.println(nodeInfo);
	}
	
	private void createNodes(DefaultMutableTreeNode parent, ITree tree) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(tree);
		trees.put(tree, node);
		parent.add(node);
		for (ITree child: tree.getChildren()) createNodes(node, child);
	}

}
