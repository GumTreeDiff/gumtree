package fr.labri.gumtree.client.ui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.FileReader;
import java.io.IOException;

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

import fr.labri.gumtree.actions.RootsClassifier;
import fr.labri.gumtree.actions.TreeClassifier;
import fr.labri.gumtree.matchers.MappingStore;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.tree.ITree;

public class MappingsPanel extends JPanel implements TreeSelectionListener {

	private static final long serialVersionUID = 1L;

	private ITree src;
	private ITree dst;
	private TreeClassifier classifyTrees;
	private MappingStore mappings;

	private TreePanel panSrc;
	private TreePanel panDst;
	private JTextArea txtSrc;
	private JTextArea txtDst;

	private static final Color DEL_COLOR = new Color(190, 0, 0);
	private static final Color ADD_COLOR = new Color(0, 158, 0);
	private static final Color UPD_COLOR = new Color(189, 162, 0);
	//private static final Color MIS_COLOR = new Color(0, 0, 128);
	private static final Color MV_COLOR = new Color(128, 0, 128);

	public MappingsPanel(String srcPath, String dstPath, ITree src, ITree dst, Matcher m)  {
		super(new GridLayout(1, 0));
		this.src = src;
		this.dst = dst;
		this.classifyTrees = new RootsClassifier(src, dst, m);
		this.mappings = new MappingStore(m.getMappingSet());
		this.panSrc = new TreePanel(src, new MappingsCellRenderer(true));
		this.panSrc.getJTree().addTreeSelectionListener(this);
		this.panDst = new TreePanel(dst, new MappingsCellRenderer(false));
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
			txtSrc.getUI().getEditorKit(txtSrc).read(new FileReader(srcPath), txtSrc.getDocument(), 0);
			txtDst.getUI().getEditorKit(txtDst).read(new FileReader(dstPath), txtDst.getDocument(), 0);
		} catch (IOException | BadLocationException e) {
			e.printStackTrace();
		}

		setPreferredSize(new Dimension(1024, 768));
		openNodes();
	}

	private void openNodes() {
		for (ITree t: classifyTrees.getSrcDelTrees()) openNode(panSrc, t);
		for (ITree t: classifyTrees.getDstAddTrees()) openNode(panDst, t); 
		for (ITree t: classifyTrees.getSrcUpdTrees()) openNode(panSrc, t);
		for (ITree t: classifyTrees.getDstUpdTrees()) openNode(panDst, t);
		for (ITree t: classifyTrees.getSrcMvTrees()) openNode(panSrc, t);
		for (ITree t: classifyTrees.getDstMvTrees()) openNode(panDst, t);
		panSrc.getJTree().scrollPathToVisible(new TreePath(panSrc.getTrees().get(src).getPath()));
		panDst.getJTree().scrollPathToVisible(new TreePath(panDst.getTrees().get(dst).getPath()));
	}

	private void openNode(TreePanel p, ITree t) {
		DefaultMutableTreeNode n = p.getTrees().get(t);
		p.getJTree().scrollPathToVisible(new TreePath(n.getPath()));
	}

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		JTree jtree = (JTree) e.getSource();
		if (jtree.getSelectionPath() == null) return;
		ITree sel = (ITree) ((DefaultMutableTreeNode) jtree.getLastSelectedPathComponent()).getUserObject();
		JTextArea selJTextArea = null;
		boolean isMapped = false;
		ITree match = null;
		TreePanel matchTreePanel = null;
		JTextArea matchJTextArea = null;

		if (jtree == panSrc.getJTree()) {
			selJTextArea = txtSrc;
			matchTreePanel = panDst;
			matchJTextArea = txtDst;
			if (mappings.hasSrc(sel)) {
				isMapped = true; match = mappings.getDst(sel);
			}
		} else {
			selJTextArea = txtDst;
			matchTreePanel = panSrc;
			matchJTextArea = txtSrc;
			if (mappings.hasDst(sel)) {
				isMapped = true; match = mappings.getSrc(sel);
			}
		}
		try {
			updateJTreeAndJTextArea(sel, selJTextArea, isMapped, match, matchTreePanel, matchJTextArea);
		} catch (BadLocationException ex) {
			ex.printStackTrace();
		}
	}

	private void updateJTreeAndJTextArea(ITree sel, JTextArea selJTextArea, boolean isMapped, 
			ITree match, TreePanel matchTreePanel, JTextArea matchJTextArea) throws BadLocationException {
		selJTextArea.getHighlighter().removeAllHighlights();
		selJTextArea.getHighlighter().addHighlight(sel.getPos(), sel.getEndPos(), DefaultHighlighter.DefaultPainter);
		selJTextArea.setCaretPosition(sel.getPos());

		if (isMapped) {
			DefaultMutableTreeNode node = matchTreePanel.getTrees().get(match);
			matchTreePanel.getJTree().scrollPathToVisible(new TreePath(node.getPath()));
			matchTreePanel.getJTree().setSelectionPath(new TreePath(node.getPath()));
			matchJTextArea.getHighlighter().removeAllHighlights();
			matchJTextArea.getHighlighter().addHighlight(match.getPos(), match.getEndPos(), DefaultHighlighter.DefaultPainter);
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
		public Component getTreeCellRendererComponent(JTree jtree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			super.getTreeCellRendererComponent(jtree, value, selected, expanded, leaf, row, hasFocus);
			ITree tree = (ITree) ((DefaultMutableTreeNode) value).getUserObject();
			if (isSrc && classifyTrees.getSrcDelTrees().contains(tree)) setForeground(DEL_COLOR);
			else if (!isSrc && classifyTrees.getDstAddTrees().contains(tree)) setForeground(ADD_COLOR);
			else if (isSrc && classifyTrees.getSrcUpdTrees().contains(tree)) setForeground(UPD_COLOR);
			else if (!isSrc && classifyTrees.getDstUpdTrees().contains(tree)) setForeground(UPD_COLOR);
			else if (isSrc && classifyTrees.getSrcMvTrees().contains(tree)) setForeground(MV_COLOR);
			else if (!isSrc && classifyTrees.getDstMvTrees().contains(tree)) setForeground(MV_COLOR);
			return this;
		}
	}

}
