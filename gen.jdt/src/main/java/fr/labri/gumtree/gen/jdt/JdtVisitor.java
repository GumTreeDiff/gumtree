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
import org.eclipse.jdt.internal.compiler.ast.BinaryExpression;

import fr.labri.gumtree.tree.Tree;

public class JdtVisitor  extends AbstractJdtVisitor {
	
	public JdtVisitor() {
		super();
	}

	@Override
	public void preVisit(ASTNode n) {
		pushNode(n, getLabel(n));
	}
	
	protected String getLabel(ASTNode n) {
		if (n instanceof Name) return ((Name) n).getFullyQualifiedName();
		if (n instanceof Type) return n.toString();
		if (n instanceof Modifier) return n.toString();
		if (n instanceof StringLiteral) return ((StringLiteral) n).getEscapedValue();
		if (n instanceof NumberLiteral) return ((NumberLiteral) n).getToken();
		if (n instanceof CharacterLiteral) return ((CharacterLiteral) n).getEscapedValue();
		if (n instanceof BooleanLiteral) return ((BooleanLiteral) n).toString(); 
		if (n instanceof InfixExpression) return ((InfixExpression) n).getOperator().toString();
		if (n instanceof PrefixExpression) return ((PrefixExpression) n).getOperator().toString();
		if (n instanceof PostfixExpression) return ((PostfixExpression) n).getOperator().toString();
		if (n instanceof Assignment) return ((Assignment) n).getOperator().toString();
		
		return "";
	}
	
	@Override
	public boolean visit(QualifiedName name) {
		return false;
	}

	@Override
	public void postVisit(ASTNode n) {
		popNode();
	}
}
