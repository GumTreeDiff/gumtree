/*
 * Copyright 2011 Jean-Rémy Falleri
 * 
 * This file is part of Praxis.
 * Praxis is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Praxis is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Praxis.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.labri.gumtree.gen.jdt;

import java.util.Stack;

import org.eclipse.jdt.core.dom.*;

import fr.labri.gumtree.tree.ITree;
import fr.labri.gumtree.tree.Tree;

public class JdtVisitor  extends AbstractJdtVisitor {
	
	private Stack<Tree> trees;

	public JdtVisitor() {
		super();
		this.root = null;
		this.trees = new Stack<Tree>();
	}

	public ITree getRoot() {
		return this.root;
	}

	@Override
	public void preVisit(ASTNode n) {
		int type = n.getNodeType();

		String label = "";
		if (n instanceof Name) label = ((Name) n).getFullyQualifiedName();
		else if (n instanceof Type) label = n.toString();
		else if (n instanceof Modifier) label = n.toString();
		else if (n instanceof StringLiteral) label = ((StringLiteral) n).getEscapedValue();
		else if (n instanceof NumberLiteral) label = ((NumberLiteral) n).getToken();
		else if (n instanceof CharacterLiteral) label = ((CharacterLiteral) n).getEscapedValue();
		else if (n instanceof BooleanLiteral) label = ((BooleanLiteral) n).toString(); 
		else if (n instanceof InfixExpression) label = ((InfixExpression) n).getOperator().toString();
		else if (n instanceof PrefixExpression) label = ((PrefixExpression) n).getOperator().toString();
		else if (n instanceof PostfixExpression) label = ((PostfixExpression) n).getOperator().toString();
		else if (n instanceof Assignment) label = ((Assignment) n).getOperator().toString();

		Tree t = new Tree(JdtTreeGenerator.class, type, label, n.getClass().getSimpleName());
		t.setPos(n.getStartPosition());
		t.setLength(n.getLength());

		if (root == null) root = t;
		else {
			Tree parent = trees.peek();
			t.setParentAndUpdateChildren(parent);
		}

		trees.push(t);
	}
	
	@Override
	public boolean visit(QualifiedName name) {
		return false;
	}

	@Override
	public void postVisit(ASTNode n) {
		trees.pop();
	}
}
