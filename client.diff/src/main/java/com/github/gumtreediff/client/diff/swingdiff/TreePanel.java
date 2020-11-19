/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.client.diff.swingdiff;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class TreePanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private JTree jtree;
    private TreeContext tree;
    private Map<Tree, DefaultMutableTreeNode> trees;

    public TreePanel(final TreeContext tree, TreeCellRenderer renderer) {
        super(new GridLayout(1, 0));
        trees = new HashMap<>();
        this.tree = tree;

        Tree root = tree.getRoot();
        DefaultMutableTreeNode top = new DefaultMutableTreeNode(root);
        trees.put(root, top);
        for (Tree child: root.getChildren())
            createNodes(top, child);

        jtree = new JTree(top) {
            private static final long serialVersionUID = 1L;
            
            @Override
            public String convertValueToText(Object value, boolean selected,
                                             boolean expanded, boolean leaf, int row,
                                             boolean hasFocus) {
                if (value != null) {
                    Tree node = ((Tree) ((DefaultMutableTreeNode) value).getUserObject());
                    return node.toString();
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

    public Map<Tree, DefaultMutableTreeNode> getTrees() {
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

    private void createNodes(DefaultMutableTreeNode parent, Tree tree) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(tree);
        trees.put(tree, node);
        parent.add(node);
        for (Tree child: tree.getChildren())
            createNodes(node, child);
    }
}
