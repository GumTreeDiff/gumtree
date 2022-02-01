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
import javax.swing.JSplitPane;
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
import com.github.gumtreediff.tree.Tree;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

public class MappingsPanel extends JPanel implements TreeSelectionListener {
    private static final long serialVersionUID = 1L;

    private final Diff diff;
    private final TreeClassifier allNodeClassifier;
    private final TreeClassifier rootNodesClassifier;

    private final TreePanel panSrc;
    private final TreePanel panDst;
    private final RSyntaxTextArea txtSrc;
    private final RSyntaxTextArea txtDst;

    private static final Color NODE_DEL_COLOR = new Color(190, 0, 0);
    private static final Color NODE_ADD_COLOR = new Color(0, 158, 0);
    private static final Color NODE_UPD_COLOR = new Color(189, 162, 0);
    private static final Color NODE_MV_COLOR = new Color(140, 0, 140);

    private static final Color TEXT_DEL_COLOR = new Color(190, 0, 0, 64);
    private static final Color TEXT_ADD_COLOR = new Color(0, 158, 0, 64);
    private static final Color TEXT_UPD_COLOR = new Color(189, 162, 0, 64);
    private static final Color TEXT_MV_COLOR = new Color(128, 0, 128, 64);

    private boolean inProcess = false;
    private Object srcTag = null;
    private Object dstTag = null;

    public MappingsPanel(String srcPath, String dstPath, Diff diff)  {
        super(new GridLayout(1, 0));
        this.diff = diff;
        this.allNodeClassifier = diff.createAllNodeClassifier();
        this.rootNodesClassifier = diff.createRootNodesClassifier();

        JPanel top = new JPanel();
        top.setLayout(new GridLayout(1, 2));
        this.panSrc = new TreePanel(this.diff.src, new MappingsCellRenderer(true));
        this.panSrc.getJTree().addTreeSelectionListener(this);
        top.add(panSrc);
        this.panDst = new TreePanel(this.diff.dst, new MappingsCellRenderer(false));
        this.panDst.getJTree().addTreeSelectionListener(this);
        top.add(panDst);

        JPanel bottom = new JPanel();
        bottom.setLayout(new GridLayout(1, 2));
        this.txtSrc = new RSyntaxTextArea();
        configureEditor(srcPath, this.txtSrc);
        bottom.add(new RTextScrollPane(txtSrc));
        this.txtDst = new RSyntaxTextArea();
        configureEditor(dstPath, this.txtDst);
        bottom.add(new RTextScrollPane(txtDst));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        split.setDividerLocation(384);
        add(split);

        try {
            txtSrc.getUI()
                    .getEditorKit(txtSrc)
                    .read(new StringReader(readFileAsString(srcPath)), txtSrc.getDocument(), 0);
            txtDst.getUI()
                    .getEditorKit(txtDst)
                    .read(new StringReader(readFileAsString(dstPath)), txtDst.getDocument(), 0);
            highlight();
        } catch (IOException | BadLocationException e) {
            e.printStackTrace();
        }
        setPreferredSize(new Dimension(1024, 768));
        openNodes();
    }

    private static String readFileAsString(String filePath) throws IOException {
        String content = "";
        content = new String(Files.readAllBytes(Paths.get(filePath)));
        content = content.replaceAll("\r\n", "\r\n ");
        return content;
    }

    private void openNodes() {
        for (Tree t: allNodeClassifier.getDeletedSrcs())
            openNode(panSrc, t);
        for (Tree t: allNodeClassifier.getInsertedDsts())
            openNode(panDst, t);
        for (Tree t: allNodeClassifier.getUpdatedSrcs())
            openNode(panSrc, t);
        for (Tree t: allNodeClassifier.getUpdatedDsts())
            openNode(panDst, t);
        for (Tree t: allNodeClassifier.getMovedSrcs())
            openNode(panSrc, t);
        for (Tree t: allNodeClassifier.getMovedDsts())
            openNode(panDst, t);
        panSrc.getJTree().scrollPathToVisible(new TreePath(panSrc.getTrees().get(diff.src.getRoot()).getPath()));
        panDst.getJTree().scrollPathToVisible(new TreePath(panDst.getTrees().get(diff.dst.getRoot()).getPath()));
    }

    private void openNode(TreePanel p, Tree t) {
        DefaultMutableTreeNode n = p.getTrees().get(t);
        p.getJTree().scrollPathToVisible(new TreePath(n.getPath()));
    }

    private void configureEditor(String path, RSyntaxTextArea editor) {
        editor.setEditable(false);
        editor.setCodeFoldingEnabled(true);
        editor.setHighlightCurrentLine(false);

        if (path.endsWith("java"))
            editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        else if (path.endsWith("js"))
            editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
        else if (path.endsWith("ts"))
            editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT);
        else if (path.endsWith("py"))
            editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
        else if (path.endsWith("rb"))
            editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_RUBY);
        else if (path.endsWith("c") || path.endsWith("h"))
            editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C);
        else if (path.endsWith("css"))
            editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CSS);
        else if (path.endsWith("cpp"))
            editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS);
        else if (path.endsWith("cs"))
            editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CSHARP);
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        if (inProcess) // Avoid firing two events in case of move and update
            return;

        inProcess = true;

        JTree jTree = (JTree) e.getSource();
        if (jTree.getSelectionPath() == null)
            return;

        Tree sel = (Tree) ((DefaultMutableTreeNode) jTree.getLastSelectedPathComponent()).getUserObject();
        RSyntaxTextArea selJTextArea;
        boolean isMapped = false;
        Tree match = null;
        TreePanel matchTreePanel;
        RSyntaxTextArea matchJTextArea;

        if (jTree == panSrc.getJTree()) {
            selJTextArea = txtSrc;
            matchTreePanel = panDst;
            matchJTextArea = txtDst;
            if (diff.mappings.isSrcMapped(sel)) {
                isMapped = true;
                match = diff.mappings.getDstForSrc(sel);
            }
        } else {
            selJTextArea = txtDst;
            matchTreePanel = panSrc;
            matchJTextArea = txtSrc;
            if (diff.mappings.isDstMapped(sel)) {
                isMapped = true;
                match = diff.mappings.getSrcForDst(sel);
            }
        }
        try {
            selectAndhighlightNodes(sel, selJTextArea, isMapped, match, matchTreePanel, matchJTextArea);
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        } finally {
            inProcess = false;
        }
    }

    private void highlight() throws BadLocationException {
        for (Tree t : rootNodesClassifier.getDeletedSrcs()) {
            txtSrc.getHighlighter().addHighlight(
                    t.getPos(), t.getEndPos(), new DefaultHighlighter.DefaultHighlightPainter(TEXT_DEL_COLOR));
        }
        for (Tree t : rootNodesClassifier.getUpdatedSrcs()) {
            txtSrc.getHighlighter().addHighlight(
                    t.getPos(), t.getEndPos(), new DefaultHighlighter.DefaultHighlightPainter(TEXT_UPD_COLOR));
        }
        for (Tree t : rootNodesClassifier.getMovedSrcs()) {
            txtSrc.getHighlighter().addHighlight(
                    t.getPos(), t.getEndPos(), new DefaultHighlighter.DefaultHighlightPainter(TEXT_MV_COLOR));
        }
        for (Tree t : rootNodesClassifier.getInsertedDsts()) {
            txtDst.getHighlighter().addHighlight(
                    t.getPos(), t.getEndPos(), new DefaultHighlighter.DefaultHighlightPainter(TEXT_ADD_COLOR));
        }
        for (Tree t : rootNodesClassifier.getUpdatedDsts()) {
            txtDst.getHighlighter().addHighlight(
                    t.getPos(), t.getEndPos(), new DefaultHighlighter.DefaultHighlightPainter(TEXT_UPD_COLOR));
        }
        for (Tree t : rootNodesClassifier.getMovedDsts()) {
            txtDst.getHighlighter().addHighlight(
                    t.getPos(), t.getEndPos(), new DefaultHighlighter.DefaultHighlightPainter(TEXT_MV_COLOR));
        }
    }

    private void selectAndhighlightNodes(Tree sel, RSyntaxTextArea selJTextArea, boolean isMapped,
                                         Tree match, TreePanel matchTreePanel,
                                         RSyntaxTextArea matchJTextArea) throws BadLocationException {
        Color color = Color.lightGray;
        if (this.srcTag != null)
            txtSrc.getHighlighter().removeHighlight(this.srcTag);
        if (this.dstTag != null)
            txtDst.getHighlighter().removeHighlight(this.dstTag);

        Object selTag = selJTextArea.getHighlighter().addHighlight(
                sel.getPos(), sel.getEndPos(), new DefaultHighlighter.DefaultHighlightPainter(color));
        selJTextArea.setCaretPosition(sel.getPos());

        if (txtSrc == selJTextArea)
            this.srcTag = selTag;
        else
            this.dstTag = selTag;

        if (isMapped) {
            DefaultMutableTreeNode node = matchTreePanel.getTrees().get(match);
            matchTreePanel.getJTree().scrollPathToVisible(new TreePath(node.getPath()));
            matchTreePanel.getJTree().setSelectionPath(new TreePath(node.getPath()));
            Object matchTag = matchJTextArea.getHighlighter().addHighlight(
                    match.getPos(), match.getEndPos(), new DefaultHighlighter.DefaultHighlightPainter(color));
            if (txtSrc == matchJTextArea)
                this.srcTag = matchTag;
            else
                this.dstTag = matchTag;
            matchJTextArea.setCaretPosition(match.getPos());
        }
    }

    private class MappingsCellRenderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = 1L;

        private final boolean isSrc;

        public MappingsCellRenderer(boolean left) {
            this.isSrc = left;
        }

        @Override
        public Component getTreeCellRendererComponent(JTree jTree, Object value,
                                                      boolean selected, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(jTree, value, selected, expanded, leaf, row, hasFocus);
            Tree tree = (Tree) ((DefaultMutableTreeNode) value).getUserObject();
            if (isSrc && allNodeClassifier.getDeletedSrcs().contains(tree))
                setForeground(NODE_DEL_COLOR);
            else if (!isSrc && allNodeClassifier.getInsertedDsts().contains(tree))
                setForeground(NODE_ADD_COLOR);
            else if (isSrc && allNodeClassifier.getUpdatedSrcs().contains(tree))
                setForeground(NODE_UPD_COLOR);
            else if (!isSrc && allNodeClassifier.getUpdatedDsts().contains(tree))
                setForeground(NODE_UPD_COLOR);
            else if (isSrc && allNodeClassifier.getMovedSrcs().contains(tree))
                setForeground(NODE_MV_COLOR);
            else if (!isSrc && allNodeClassifier.getMovedDsts().contains(tree))
                setForeground(NODE_MV_COLOR);

            return this;
        }
    }

}
