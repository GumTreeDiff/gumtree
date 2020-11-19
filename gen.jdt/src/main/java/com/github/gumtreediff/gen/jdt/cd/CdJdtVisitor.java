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

package com.github.gumtreediff.gen.jdt.cd;

import java.util.List;

import com.github.gumtreediff.gen.jdt.AbstractJdtVisitor;
import org.eclipse.jdt.core.dom.*;

/**
 * Combination of two ChangeDistiller's AST visitors:
 *  JavaASTBodyTransformer and JavaASTChangeDistillerVisitor.
 * Modifications are labeled as "@Inria"
 *
 * INRIA removed fNodeStack since it's inherited with the new
 */
@SuppressWarnings("unused")
public class CdJdtVisitor extends AbstractJdtVisitor {
    private static final String COLON_SPACE = ": ";
    private boolean fEmptyJavaDoc;
    private boolean fInMethodDeclaration;

    @Override
    public boolean visit(Block node) {
        // skip block as it is not interesting
        return true;
    }

    @Override
    public void endVisit(Block node) {
        // do nothing pop is not needed (see visit(Block))
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean visit(FieldDeclaration node) {
        if (node.getJavadoc() != null) {
            node.getJavadoc().accept(this);
        }

        // @Inria
        pushNode(node, node.toString());
        //
        visitListAsNode(EntityType.MODIFIERS, node.modifiers());
        node.getType().accept(this);
        visitListAsNode(EntityType.FRAGMENTS, node.fragments());

        return false;
    }

    @Override
    public void endVisit(FieldDeclaration node) {
        // @Inria
        popNode();
    }

    @Override
    public boolean visit(Javadoc node) {
        String string = "";
        // @Inria: exclude doc
        if (checkEmptyJavaDoc(string)) {
            pushNode(node, string);
        } else {
            // @TODO not really robust, hopefully there is no nested javadoc comments.
            fEmptyJavaDoc = true;
        }
        return false;
    }

    @Override
    public void endVisit(Javadoc node) {
        if (!fEmptyJavaDoc)
            popNode();
        else
            fEmptyJavaDoc = false;
    }

    private boolean checkEmptyJavaDoc(String doc) {
        String[] splittedDoc = doc.split("/\\*+\\s*");
        String result = "";
        for (String s : splittedDoc) {
            result += s;
        }
        try {
            result = result.split("\\s*\\*/")[0];
        } catch (ArrayIndexOutOfBoundsException e) {
            result = result.replace('/', ' ');
        }
        result = result.replace('*', ' ').trim();

        return !result.equals("");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean visit(MethodDeclaration node) {
        if (node.getJavadoc() != null) {
            node.getJavadoc().accept(this);
        }
        fInMethodDeclaration = true;

        // @Inria
        pushNode(node, node.getName().toString());
        //

        visitListAsNode(EntityType.MODIFIERS, node.modifiers());
        if (node.getReturnType2() != null) {
            node.getReturnType2().accept(this);
        }
        visitListAsNode(EntityType.TYPE_ARGUMENTS, node.typeParameters());
        visitListAsNode(EntityType.PARAMETERS, node.parameters());
        visitListAsNode(EntityType.THROW, node.thrownExceptionTypes());

        // @Inria
        // The body can be null when the method declaration is from a interface
        if (node.getBody() != null) {
            node.getBody().accept(this);
        }
        return false;

    }

    @Override
    public void endVisit(MethodDeclaration node) {
        fInMethodDeclaration = false;
        popNode();
    }

    @Override
    public boolean visit(Modifier node) {
        pushNode(node, node.getKeyword().toString());
        return false;
    }

    @Override
    public void endVisit(Modifier node) {
        popNode();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean visit(ParameterizedType node) {
        pushNode(node, "");
        node.getType().accept(this);
        visitListAsNode(EntityType.TYPE_ARGUMENTS, node.typeArguments());
        return false;
    }

    @Override
    public void endVisit(ParameterizedType node) {
        popNode();
    }

    @Override
    public boolean visit(PrimitiveType node) {
        String vName = "";
        if (fInMethodDeclaration) {
            vName += getCurrentParent().getLabel() + COLON_SPACE;
        }
        pushNode(node, vName + node.getPrimitiveTypeCode().toString());
        return false;
    }

    @Override
    public void endVisit(PrimitiveType node) {
        popNode();
    }

    @Override
    public boolean visit(QualifiedType node) {
        pushNode(node, "");
        return true;
    }

    @Override
    public void endVisit(QualifiedType node) {
        popNode();
    }

    @Override
    public boolean visit(SimpleType node) {
        String vName = "";
        if (fInMethodDeclaration) {
            vName += getCurrentParent().getLabel() + COLON_SPACE;
        }
        pushNode(node, vName + node.getName().getFullyQualifiedName());
        return false;
    }

    @Override
    public void endVisit(SimpleType node) {
        popNode();
    }

    @Override
    public boolean visit(SingleVariableDeclaration node) {
        boolean isNotParam = !getCurrentParent().getLabel().equals(EntityType.PARAMETERS.toString());// @inria
        pushNode(node, node.getName().getIdentifier());
        node.getType().accept(this);
        return false;
    }

    @Override
    public void endVisit(SingleVariableDeclaration node) {
        popNode();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean visit(TypeDeclaration node) {
        if (node.getJavadoc() != null) {
            node.getJavadoc().accept(this);
        }
        // @Inria
        pushNode(node, node.getName().toString());
        visitListAsNode(EntityType.MODIFIERS, node.modifiers());
        visitListAsNode(EntityType.TYPE_ARGUMENTS, node.typeParameters());
        if (node.getSuperclassType() != null) {
            node.getSuperclassType().accept(this);
        }

        visitListAsNode(EntityType.SUPER_INTERFACE_TYPES, node.superInterfaceTypes());

        // @Inria
        // Change Distiller does not check the changes at Class Field declaration
        for (FieldDeclaration fd : node.getFields()) {
            fd.accept(this);
        }
        // @Inria
        // Visit Declaration and Body (inside MD visiting)
        for (MethodDeclaration md : node.getMethods()) {
            md.accept(this);
        }
        return false;
    }

    @Override
    public void endVisit(TypeDeclaration node) {
        popNode();
    }

    @Override
    public boolean visit(TypeDeclarationStatement node) {
        // skip, only type declaration is interesting
        return true;
    }

    @Override
    public void endVisit(TypeDeclarationStatement node) {
        // do nothing
    }

    @Override
    public boolean visit(TypeLiteral node) {
        pushNode(node, "");
        return true;
    }

    @Override
    public void endVisit(TypeLiteral node) {
        popNode();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean visit(TypeParameter node) {
        pushNode(node, node.getName().getFullyQualifiedName());
        visitList(node.typeBounds());
        return false;
    }

    @Override
    public void endVisit(TypeParameter node) {
        popNode();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean visit(VariableDeclarationExpression node) {
        pushNode(node, "");
        visitListAsNode(EntityType.MODIFIERS, node.modifiers());
        node.getType().accept(this);
        visitListAsNode(EntityType.FRAGMENTS, node.fragments());
        return false;
    }

    @Override
    public void endVisit(VariableDeclarationExpression node) {
        popNode();
    }

    @Override
    public boolean visit(VariableDeclarationFragment node) {
        pushNode(node, node.getName().getFullyQualifiedName());
        return false;
    }

    @Override
    public void endVisit(VariableDeclarationFragment node) {
        popNode();
    }

    @Override
    public boolean visit(WildcardType node) {
        String bound = node.isUpperBound() ? "extends" : "super";
        pushNode(node, bound);
        return true;
    }

    @Override
    public void endVisit(WildcardType node) {
        popNode();
    }

    private void visitList(List<ASTNode> list) {
        for (ASTNode node : list) {
            (node).accept(this);
        }
    }

    private void visitListAsNode(EntityType fakeType, List<ASTNode> list) {
        int start = startPosition(list);
        pushFakeNode(fakeType, start, endPosition(list) - start);
        if (!list.isEmpty()) {
            // @Inria
            // As ChangeDistiller has empty nodes e.g.
            //  Type Argument, Parameter, Thown,
            //  the push and pop are before the empty condition check
            visitList(list);
        }
        popNode();
    }

    private int startPosition(List<ASTNode> list) {
        if (list.isEmpty())
            return -1;
        return list.get(0).getStartPosition();
    }

    private int endPosition(List<ASTNode> list) {
        if (list.isEmpty())
            return 0;
        ASTNode n = list.get(list.size() - 1);
        return n.getStartPosition() + n.getLength();
    }
    // /***************BODY VISITOR*************************
    private static final String COLON = ":";

    @Override
    public boolean visit(AssertStatement node) {
        String value = node.getExpression().toString();
        if (node.getMessage() != null) {
            value += COLON + node.getMessage().toString();
        }
        pushNode(node, value);
        return false;
    }

    @Override
    public void endVisit(AssertStatement node) {
        popNode();
    }

    @Override
    public boolean visit(BreakStatement node) {
        pushNode(node, node.getLabel() != null ? node.getLabel().toString() : "");
        return false;
    }

    @Override
    public void endVisit(BreakStatement node) {
        popNode();
    }

    @Override
    public boolean visit(CatchClause node) {
	if (node.getException().getType() instanceof SimpleType) {
            pushNode(node, ((SimpleType) node.getException().getType()).getName().getFullyQualifiedName());
        } else {
            pushNode(node, node.getException().getType().toString());
        }
        // since exception type is used as value, visit children by hand
        node.getBody().accept(this);
        return false;
    }

    @Override
    public void endVisit(CatchClause node) {
        popNode();
    }

    @Override
    public boolean visit(ConstructorInvocation node) {
        pushNode(node, node.toString());
        return false;
    }

    @Override
    public void endVisit(ConstructorInvocation node) {
        popNode();
    }

    @Override
    public boolean visit(ContinueStatement node) {
        pushNode(node, node.getLabel() != null ? node.getLabel().toString() : "");
        return false;
    }

    @Override
    public void endVisit(ContinueStatement node) {
        popNode();
    }

    @Override
    public boolean visit(DoStatement node) {
        pushNode(node, node.getExpression().toString());
        return true;
    }

    @Override
    public void endVisit(DoStatement node) {
        popNode();
    }

    @Override
    public boolean visit(EmptyStatement node) {
        pushNode(node, "");
        return false;
    }

    @Override
    public void endVisit(EmptyStatement node) {
        popNode();
    }

    @Override
    public boolean visit(EnhancedForStatement node) {
        pushNode(node, node.getParameter().toString() + COLON + node.getExpression().toString());
        return true;
    }

    @Override
    public void endVisit(EnhancedForStatement node) {
        popNode();
    }

    @Override
    public boolean visit(ExpressionStatement node) {
        pushNode(node.getExpression(), node.toString());
        return false;
    }

    @Override
    public void endVisit(ExpressionStatement node) {
        popNode();
    }

    @Override
    public boolean visit(ForStatement node) {
        String value = "";
        if (node.getExpression() != null) {
            value = node.getExpression().toString();
        }
        pushNode(node, value);
        return true;
    }

    @Override
    public void endVisit(ForStatement node) {
        popNode();
    }

    @Override
    public boolean visit(IfStatement node) {
        String expression = node.getExpression().toString();
        pushNode(node, expression/* , node.getStartPosition(), node.getLength() */);

        Statement stmt = node.getThenStatement();
        if (stmt != null) {
            pushNode(stmt, expression);
            stmt.accept(this);
            popNode();
        }

        stmt = node.getElseStatement();
        if (stmt != null) {
            pushNode(stmt, expression);
            node.getElseStatement().accept(this);
            popNode();
        }
        return false;
    }

    @Override
    public void endVisit(IfStatement node) {
        popNode();
    }

    @Override
    public boolean visit(LabeledStatement node) {
        pushNode(node, node.getLabel().getFullyQualifiedName());
        node.getBody().accept(this);
        return false;
    }

    @Override
    public void endVisit(LabeledStatement node) {
        popNode();
    }

    @Override
    public boolean visit(ReturnStatement node) {
        pushNode(node, node.getExpression() != null ? node.getExpression().toString() : "");
        return false;
    }

    @Override
    public void endVisit(ReturnStatement node) {
        popNode();
    }

    @Override
    public boolean visit(SuperConstructorInvocation node) {
        pushNode(node, node.toString());
        return false;
    }

    @Override
    public void endVisit(SuperConstructorInvocation node) {
        popNode();
    }

    @Override
    public boolean visit(SwitchCase node) {
        pushNode(node, node.getExpression() != null ? node.getExpression().toString() : "default");
        return false;
    }

    @Override
    public void endVisit(SwitchCase node) {
        popNode();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean visit(SwitchStatement node) {
        pushNode(node, node.getExpression().toString());
        visitList(node.statements());
        return false;
    }

    @Override
    public void endVisit(SwitchStatement node) {
        popNode();
    }

    @Override
    public boolean visit(SynchronizedStatement node) {
        pushNode(node, node.getExpression().toString());
        return true;
    }

    @Override
    public void endVisit(SynchronizedStatement node) {
        popNode();
    }

    @Override
    public boolean visit(ThrowStatement node) {
        pushNode(node, node.getExpression().toString());
        return false;
    }

    @Override
    public void endVisit(ThrowStatement node) {
        popNode();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean visit(TryStatement node) {
        pushNode(node, "");

        Statement stmt = node.getBody();
        pushNode(stmt, "");
        stmt.accept(this);
        popNode();

        visitListAsNode(EntityType.CATCH_CLAUSES, node.catchClauses());

        stmt = node.getFinally();
        if (stmt != null) {
            // @Inria
            pushNode(stmt, "");
            stmt.accept(this);
            popNode();
        }
        return false;
    }

    @Override
    public void endVisit(TryStatement node) {
        popNode();
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        pushNode(node, node.toString());
        return false;
    }

    @Override
    public void endVisit(VariableDeclarationStatement node) {
        popNode();
    }

    @Override
    public boolean visit(WhileStatement node) {
        pushNode(node, node.getExpression().toString());
        return true;
    }

    @Override
    public void endVisit(WhileStatement node) {
        popNode();
    }

}
