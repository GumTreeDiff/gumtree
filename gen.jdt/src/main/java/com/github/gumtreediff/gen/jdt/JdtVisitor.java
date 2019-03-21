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
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com> *
 */


package com.github.gumtreediff.gen.jdt;


import com.github.gumtreediff.tree.ITree;
import org.eclipse.jdt.core.dom.*;

public class JdtVisitor  extends AbstractJdtVisitor {

    private static final int INFIX_EXPRESSION_OPERATOR = -1;
    private static final int METHOD_INVOCATION_RECEIVER = -2;
    private static final int METHOD_INVOCATION_ARGUMENTS = -3;
    private static final int TYPE_DECLARATION_KIND = -4;
    private static final int ASSIGNMENT_OPERATOR = -5;
    private static final int PREFIX_EXPRESSION_OPERATOR = -6;
    private static final int POSTFIX_EXPRESSION_OPERATOR = -7;

    public JdtVisitor() {
        super();
    }

    @Override
    public void preVisit(ASTNode n) {
        pushNode(n, getLabel(n));
    }

    public boolean visit(MethodInvocation i)  {
        if (i.getExpression() !=  null) {
            push(METHOD_INVOCATION_RECEIVER, "MethodInvocationReceiver", "", i.getExpression().getStartPosition(),
                    i.getExpression().getLength());
            i.getExpression().accept(this);
            popNode();
        }
        pushNode(i.getName(), getLabel(i.getName()));
        popNode();
        if (i.arguments().size() >  0) {
            int startPos = ((ASTNode) i.arguments().get(0)).getStartPosition();
            int length = ((ASTNode) i.arguments().get(i.arguments().size() - 1)).getStartPosition()
                    + ((ASTNode) i.arguments().get(i.arguments().size() - 1)).getLength() -  startPos;
            push(METHOD_INVOCATION_ARGUMENTS, "MethodInvocationArguments","", startPos , length);
            for (Object o : i.arguments()) {
                ((ASTNode) o).accept(this);

            }
            popNode();
        }
        return false;
    }

    protected String getLabel(ASTNode n) {
        if (n instanceof Name)
            return ((Name) n).getFullyQualifiedName();
        else if (n instanceof Type)
            return n.toString();
        else if (n instanceof Modifier)
            return n.toString();
        else if (n instanceof StringLiteral)
            return ((StringLiteral) n).getEscapedValue();
        else if (n instanceof NumberLiteral)
            return ((NumberLiteral) n).getToken();
        else if (n instanceof CharacterLiteral)
            return ((CharacterLiteral) n).getEscapedValue();
        else if (n instanceof BooleanLiteral)
            return ((BooleanLiteral) n).toString();
        else if (n instanceof TextElement)
            return n.toString();
        else if (n instanceof TagElement)
            return ((TagElement) n).getTagName();
        else
            return "";
    }

    @Override
    public boolean visit(TypeDeclaration d) {
        return true;
    }

    @Override
    public boolean visit(TagElement e) {
        return true;
    }

    @Override
    public boolean visit(QualifiedName name) {
        return false;
    }

    @Override
    public void postVisit(ASTNode n) {
        if (n instanceof TypeDeclaration)
            handlePostVisit((TypeDeclaration) n);
        else if (n instanceof InfixExpression)
            handlePostVisit((InfixExpression) n);
        else if (n instanceof Assignment)
            handlePostVisit((Assignment)  n);
        else if (n instanceof PrefixExpression)
            handlePostVisit((PrefixExpression) n);
        else if (n instanceof PostfixExpression)
            handlePostVisit((PostfixExpression) n);

        popNode();
    }

    private void handlePostVisit(PostfixExpression e) {
        ITree t = this.trees.peek();
        String label  = e.getOperator().toString();
        int pos = t.getPos() + e.toString().indexOf(label);
        ITree s = context.createTree(POSTFIX_EXPRESSION_OPERATOR, label, "PostfixExpressionOperator");
        s.setPos(pos);
        s.setLength(label.length());
        t.getChildren().add(1, s);
        s.setParent(t);
    }

    private void handlePostVisit(PrefixExpression e) {
        ITree t = this.trees.peek();
        String label  = e.getOperator().toString();
        int pos = t.getPos() + e.toString().indexOf(label);
        ITree s = context.createTree(PREFIX_EXPRESSION_OPERATOR, label, "PrefixExpressionOperator");
        s.setPos(pos);
        s.setLength(label.length());
        t.getChildren().add(0, s);
        s.setParent(t);
    }

    private void handlePostVisit(Assignment a) {
        ITree t = this.trees.peek();
        String label  = a.getOperator().toString();
        int pos = t.getPos() + a.toString().indexOf(label);
        ITree s = context.createTree(ASSIGNMENT_OPERATOR, label, "AssignmentOperator");
        s.setPos(pos);
        s.setLength(label.length());
        t.getChildren().add(1, s);
        s.setParent(t);
    }

    private void handlePostVisit(InfixExpression e) {
        ITree t = this.trees.peek();
        String label  = e.getOperator().toString();
        int pos = t.getPos() + e.toString().indexOf(label);
        ITree s = context.createTree(INFIX_EXPRESSION_OPERATOR, label, "InfixExpressionOperator");
        s.setPos(pos);
        s.setLength(label.length());
        t.getChildren().add(1, s);
        s.setParent(t);
    }

    private void handlePostVisit(TypeDeclaration d) {
        ITree t = this.trees.peek();
        String label = "class";
        if (d.isInterface())
            label = "interface";
        int pos = t.getPos() + d.toString().indexOf(label);
        ITree s = context.createTree(TYPE_DECLARATION_KIND, label, "TypeDeclarationKind");
        s.setPos(pos);
        s.setLength(label.length());
        int index = 0;
        for (ITree c : t.getChildren()) {
            if (!context.getTypeLabel(c).equals("SimpleName"))
                index++;
        }
        t.getChildren().add(index - 1, s);
        s.setParent(t);
    }
}
