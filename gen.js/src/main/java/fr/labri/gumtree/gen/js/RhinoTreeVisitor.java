package fr.labri.gumtree.gen.js;

import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.NumberLiteral;
import org.mozilla.javascript.ast.StringLiteral;

import fr.labri.gumtree.tree.ITree;
import fr.labri.gumtree.tree.TreeContext;

public class RhinoTreeVisitor implements NodeVisitor {
	
	private Map<AstNode, ITree> trees;
	private TreeContext context;
	
	public RhinoTreeVisitor(AstRoot root) {
		trees = new HashMap<>();
		context = new TreeContext();
	}
	
	public TreeContext getTree(AstNode root) {
		ITree tree = buildTree(root);
		context.setRoot(tree);
		visit(root);
		return context;
	}
	
	public boolean visit(AstNode node) {
		if (node instanceof AstRoot)
			return true;
		else {
			ITree t = buildTree(node);
			ITree p = trees.get(node.getParent());
			p.addChild(t);
			
			if (node instanceof Name) {
				Name name = (Name) node;
				t.setLabel(name.getIdentifier());
			} else if ( node instanceof StringLiteral) {
				StringLiteral literal = (StringLiteral) node;
				t.setLabel(literal.getValue());
			} else if ( node instanceof NumberLiteral) {
				NumberLiteral l = (NumberLiteral) node;
				t.setLabel(l.getValue());
			}
			
			return true;
		}
	}
	
	private ITree buildTree(AstNode node)  {
		ITree t = context.createTree(node.getType(), ITree.NO_LABEL, Token.typeToName(node.getType()));
		t.setPos(node.getAbsolutePosition());
		t.setLength(node.getLength());
		trees.put(node, t);
		return t;
	}

}
