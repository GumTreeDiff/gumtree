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
import fr.labri.gumtree.tree.TreeContext;

public class TreePanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	private JTree jtree;
	private TreeContext tree;
	private Map<ITree, DefaultMutableTreeNode> trees;

	public TreePanel(final TreeContext tree, TreeCellRenderer renderer) {
		super(new GridLayout(1, 0));
		trees = new HashMap<>();
		this.tree = tree;

		ITree root = tree.getRoot();
		DefaultMutableTreeNode top = new DefaultMutableTreeNode(root);
		trees.put(root, top);
		for (ITree child: root.getChildren()) createNodes(top, child);
		
		jtree = new JTree(top) {
			private static final long serialVersionUID = 1L;
		    public String convertValueToText(Object value, boolean selected,
                    boolean expanded, boolean leaf, int row,
                    boolean hasFocus) {
		    	if(value != null) {
		    		ITree node = ((ITree)((DefaultMutableTreeNode)value).getUserObject());
		    		return node.toPrettyString(tree);
		        }
		    	return "";
			}
		};
		jtree.setCellRenderer(renderer);
		jtree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		JScrollPane treeView = new JScrollPane(jtree);
		Dimension minimumSize = new Dimension(100, 50);
		treeView.setMinimumSize(minimumSize);

		add(treeView);
	}
	
	public TreePanel(TreeContext tree) {
		this(tree, new DefaultTreeCellRenderer());
	}
	
	public JTree getJTree() {
		return jtree;
	}
	
	public Map<ITree, DefaultMutableTreeNode> getTrees() {
		return trees;
	}
	
	public TreeContext getTree() {
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
