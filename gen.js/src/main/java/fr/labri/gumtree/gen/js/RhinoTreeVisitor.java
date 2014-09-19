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

import fr.labri.gumtree.tree.Tree;

public class RhinoTreeVisitor implements NodeVisitor {
	
	private Tree tree;
	
	private Map<AstNode, Tree> trees;
	
	public RhinoTreeVisitor(AstRoot root) {
		trees = new HashMap<>();
		tree = buildTree(root);
	}
	
	public boolean visit(AstNode node) {
		if (node instanceof AstRoot)
			return true;
		else {
			Tree t = buildTree(node);
			Tree p = trees.get(node.getParent());
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
	
	public Tree getTree() {
		return tree;
	}
	
	private Tree buildTree(AstNode node)  {
		Tree t = new Tree(node.getType(), Token.typeToName(node.getType()));
		t.setPos(node.getAbsolutePosition());
		t.setLength(node.getLength());
		trees.put(node, t);
		return t;
	}

}
