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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.actions.TreeClassifier;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;

public class MappingsPanel extends JPanel implements TreeSelectionListener {

    private static final long serialVersionUID = 1L;

    private TreeContext src;
    private TreeContext dst;
    private TreeClassifier classifyTrees;
    private MappingStore mappings;

    private TreePanel panSrc;
    private TreePanel panDst;
    private JTextArea txtSrc;
    private JTextArea txtDst;

    private static final Color DEL_COLOR = new Color(190, 0, 0);
    private static final Color ADD_COLOR = new Color(0, 158, 0);
    private static final Color UPD_COLOR = new Color(189, 162, 0);
    private static final Color MV_COLOR = new Color(128, 0, 128);

    public MappingsPanel(String srcPath, String dstPath, Diff diff)  {
        super(new GridLayout(1, 0));
        this.src = diff.src;
        this.dst = diff.dst;
        this.classifyTrees = diff.createAllNodeClassifier();
        this.mappings = diff.mappings;
        this.panSrc = new TreePanel(this.src, new MappingsCellRenderer(true));
        this.panSrc.getJTree().addTreeSelectionListener(this);
        this.panDst = new TreePanel(this.dst, new MappingsCellRenderer(false));
        this.panDst.getJTree().addTreeSelectionListener(this);
        this.txtSrc = new JTextArea();
        this.txtDst = new JTextArea();

        JPanel top = new JPanel();
        top.setLayout(new GridLayout(1, 2));
        top.add(panSrc);
        top.add(panDst);
        JPanel bottom = new JPanel();
        bottom.setLayout(new GridLayout(1, 2));
        bottom.add(new JScrollPane(txtSrc));
        bottom.add(new JScrollPane(txtDst));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        split.setDividerLocation(650);
        add(split);

        try {
            //FIXME a checkstyle issue induced the strange indentation
            txtSrc
                    .getUI()
                    .getEditorKit(txtSrc)
                    .read(new StringReader(readFileAsString(srcPath)), txtSrc.getDocument(), 0);
            txtDst
                    .getUI()
                    .getEditorKit(txtDst)
                    .read(new StringReader(readFileAsString(dstPath)), txtDst.getDocument(), 0);
        } catch (IOException | BadLocationException e) {
            e.printStackTrace();
        }
        setPreferredSize(new Dimension(1024, 768));
        openNodes();
    }

    private static String readFileAsString(String filePath) {
        String content = "";
        try {
            content = new String(Files.readAllBytes(Paths.get(filePath)));
            content = content.replaceAll("\r\n", "\r\n ");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    private void openNodes() {
        for (Tree t: classifyTrees.getDeletedSrcs()) openNode(panSrc, t);
        for (Tree t: classifyTrees.getInsertedDsts()) openNode(panDst, t);
        for (Tree t: classifyTrees.getUpdatedSrcs()) openNode(panSrc, t);
        for (Tree t: classifyTrees.getUpdatedDsts()) openNode(panDst, t);
        for (Tree t: classifyTrees.getMovedSrcs()) openNode(panSrc, t);
        for (Tree t: classifyTrees.getMovedDsts()) openNode(panDst, t);
        panSrc.getJTree().scrollPathToVisible(new TreePath(panSrc.getTrees().get(src.getRoot()).getPath()));
        panDst.getJTree().scrollPathToVisible(new TreePath(panDst.getTrees().get(dst.getRoot()).getPath()));
    }

    private void openNode(TreePanel p, Tree t) {
        DefaultMutableTreeNode n = p.getTrees().get(t);
        p.getJTree().scrollPathToVisible(new TreePath(n.getPath()));
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        JTree jtree = (JTree) e.getSource();
        if (jtree.getSelectionPath() == null) return;
        Tree sel = (Tree) ((DefaultMutableTreeNode) jtree.getLastSelectedPathComponent()).getUserObject();
        JTextArea selJTextArea;
        boolean isMapped = false;
        Tree match = null;
        TreePanel matchTreePanel;
        JTextArea matchJTextArea;

        if (jtree == panSrc.getJTree()) {
            selJTextArea = txtSrc;
            matchTreePanel = panDst;
            matchJTextArea = txtDst;
            if (mappings.isSrcMapped(sel)) {
                isMapped = true;
                match = mappings.getDstForSrc(sel);
            }
        } else {
            selJTextArea = txtDst;
            matchTreePanel = panSrc;
            matchJTextArea = txtSrc;
            if (mappings.isDstMapped(sel)) {
                isMapped = true;
                match = mappings.getSrcForDst(sel);
            }
        }
        try {
            updateJTreeAndJTextArea(sel, selJTextArea, isMapped, match, matchTreePanel, matchJTextArea);
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    private void updateJTreeAndJTextArea(Tree sel, JTextArea selJTextArea, boolean isMapped,
                                         Tree match, TreePanel matchTreePanel,
                                         JTextArea matchJTextArea) throws BadLocationException {
        selJTextArea.getHighlighter().removeAllHighlights();
        selJTextArea.getHighlighter().addHighlight(sel.getPos(), sel.getEndPos(), DefaultHighlighter.DefaultPainter);
        selJTextArea.setCaretPosition(sel.getPos());

        if (isMapped) {
            DefaultMutableTreeNode node = matchTreePanel.getTrees().get(match);
            matchTreePanel.getJTree().scrollPathToVisible(new TreePath(node.getPath()));
            matchTreePanel.getJTree().setSelectionPath(new TreePath(node.getPath()));
            matchJTextArea.getHighlighter().removeAllHighlights();
            matchJTextArea.getHighlighter().addHighlight(
                    match.getPos(), match.getEndPos(), DefaultHighlighter.DefaultPainter);
            matchJTextArea.setCaretPosition(match.getPos());
        } else {
            matchTreePanel.getJTree().clearSelection();
            matchJTextArea.getHighlighter().removeAllHighlights();
        }
    }

    private class MappingsCellRenderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = 1L;

        private boolean isSrc;

        public MappingsCellRenderer(boolean left) {
            this.isSrc = left;
        }

        @Override
        public Component getTreeCellRendererComponent(JTree jtree, Object value,
                                                      boolean selected, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(jtree, value, selected, expanded, leaf, row, hasFocus);
            Tree tree = (Tree) ((DefaultMutableTreeNode) value).getUserObject();
            if (isSrc && classifyTrees.getDeletedSrcs().contains(tree)) setForeground(DEL_COLOR);
            else if (!isSrc && classifyTrees.getInsertedDsts().contains(tree)) setForeground(ADD_COLOR);
            else if (isSrc && classifyTrees.getUpdatedSrcs().contains(tree)) setForeground(UPD_COLOR);
            else if (!isSrc && classifyTrees.getUpdatedDsts().contains(tree)) setForeground(UPD_COLOR);
            else if (isSrc && classifyTrees.getMovedSrcs().contains(tree)) setForeground(MV_COLOR);
            else if (!isSrc && classifyTrees.getMovedDsts().contains(tree)) setForeground(MV_COLOR);
            return this;
        }
    }

}
