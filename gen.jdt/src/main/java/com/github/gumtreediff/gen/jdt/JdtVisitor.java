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


import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.Type;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.*;

import static com.github.gumtreediff.tree.TypeSet.type;

public class JdtVisitor  extends AbstractJdtVisitor {

    private static final Type INFIX_EXPRESSION_OPERATOR = type("INFIX_EXPRESSION_OPERATOR");
    private static final Type METHOD_INVOCATION_RECEIVER = type("METHOD_INVOCATION_RECEIVER");
    private static final Type METHOD_INVOCATION_ARGUMENTS = type("METHOD_INVOCATION_ARGUMENTS");
    private static final Type TYPE_DECLARATION_KIND = type("TYPE_DECLARATION_KIND");
    private static final Type ASSIGNMENT_OPERATOR = type("ASSIGNMENT_OPERATOR");
    private static final Type PREFIX_EXPRESSION_OPERATOR = type("PREFIX_EXPRESSION_OPERATOR");
    private static final Type POSTFIX_EXPRESSION_OPERATOR = type("POSTFIX_EXPRESSION_OPERATOR");

    private static final Type ARRAY_INITIALIZER = nodeAsSymbol(ASTNode.ARRAY_INITIALIZER);
    private static final Type SIMPLE_NAME = nodeAsSymbol(ASTNode.SIMPLE_NAME);

    private IScanner scanner;

    public JdtVisitor(IScanner scanner) {
        super();
        this.scanner = scanner;
    }

    @Override
    public void preVisit(ASTNode n) {
        pushNode(n, getLabel(n));
    }

    public boolean visit(MethodInvocation i)  {
        if (i.getExpression() !=  null) {
            push(i, METHOD_INVOCATION_RECEIVER, "", i.getExpression().getStartPosition(),
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
            push(i, METHOD_INVOCATION_ARGUMENTS,"", startPos , length);
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
        else if (n instanceof PrimitiveType)
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
            return n.toString();
        else if (n instanceof TextElement)
            return n.toString();
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
    public boolean visit(SingleVariableDeclaration d) {
        if (d.isVarargs()) {
            pushNode(d, "");
            pushNode(d.getType(), type("VARARGS_TYPE"), d.getStartPosition(),
                    d.getName().getStartPosition() - 1 - d.getStartPosition());
            d.getType().accept(this);
            popNode();
            d.getName().accept(this);
            popNode();
            return false;
        }
        else
            return true;
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
        else if (n instanceof ArrayCreation)
            handlePostVisit((ArrayCreation) n);

        popNode();
    }

    private void handlePostVisit(ArrayCreation c) {
        Tree t = this.trees.peek();
        if (t.getChild(1).getType() == ARRAY_INITIALIZER)
            return;
        for (int i = 1; i < t.getChild(0).getChildren().size(); i++) {
            Tree dim = t.getChild(0).getChild(i);
            if (t.getChildren().size() < 2)
                break;
            Tree expr = t.getChildren().remove(1);
            dim.addChild(expr);
        }
    }

    private void handlePostVisit(PostfixExpression e) {
        Tree t = this.trees.peek();
        String label  = e.getOperator().toString();
        Tree s = context.createTree(POSTFIX_EXPRESSION_OPERATOR, label);
        PosAndLength pl = searchPostfixExpressionPosition(e);
        s.setPos(pl.pos);
        s.setLength(pl.length);
        t.getChildren().add(1, s);
        s.setParent(t);
    }

    private PosAndLength searchPostfixExpressionPosition(PostfixExpression e) {
        Tree t = this.trees.peek();
        scanner.resetTo(t.getChild(0).getEndPos(), t.getEndPos());
        int pos = 0;
        int length = 0;
        try {
            int token = scanner.getNextToken();
            while (token != ITerminalSymbols.TokenNameEOF) {
                pos = scanner.getCurrentTokenStartPosition();
                length = scanner.getCurrentTokenEndPosition() - pos + 1;
                break;
            }
        }
        catch (InvalidInputException ex) {
            throw new SyntaxException(null, null, ex);
        }

        return new PosAndLength(pos, length);
    }

    private void handlePostVisit(PrefixExpression e) {
        Tree t = this.trees.peek();
        String label  = e.getOperator().toString();
        Tree s = context.createTree(PREFIX_EXPRESSION_OPERATOR, label);
        PosAndLength pl = searchPrefixExpressionPosition(e);
        s.setPos(pl.pos);
        s.setLength(pl.length);
        t.getChildren().add(0, s);
        s.setParent(t);
    }

    private PosAndLength searchPrefixExpressionPosition(PrefixExpression e) {
        Tree t = this.trees.peek();
        scanner.resetTo(t.getPos(), t.getChild(0).getPos());
        int pos = 0;
        int length = 0;
        try {
            int token = scanner.getNextToken();
            while (token != ITerminalSymbols.TokenNameEOF) {
                pos = scanner.getCurrentTokenStartPosition();
                length = scanner.getCurrentTokenEndPosition() - pos + 1;
                break;
            }
        }
        catch (InvalidInputException ex) {
            throw new SyntaxException(null, null, ex);
        }

        return new PosAndLength(pos, length);
    }

    private void handlePostVisit(Assignment a) {
        Tree t = this.trees.peek();
        String label  = a.getOperator().toString();
        Tree s = context.createTree(ASSIGNMENT_OPERATOR, label);
        PosAndLength pl = searchAssignmentOperatorPosition(a);
        s.setPos(pl.pos);
        s.setLength(pl.length);
        t.getChildren().add(1, s);
        s.setParent(t);
    }

    private PosAndLength searchAssignmentOperatorPosition(Assignment a) {
        Tree t = this.trees.peek();
        scanner.resetTo(t.getChild(0).getEndPos(), t.getChild(1).getPos());
        int pos = 0;
        int length = 0;
        try {
            int token = scanner.getNextToken();
            while (token != ITerminalSymbols.TokenNameEOF) {
                pos = scanner.getCurrentTokenStartPosition();
                length = scanner.getCurrentTokenEndPosition() - pos + 1;
                break;
            }
        }
        catch (InvalidInputException e) {
            throw new SyntaxException(null, null, e);
        }

        return new PosAndLength(pos, length);
    }

    private void handlePostVisit(InfixExpression e) {
        Tree t = this.trees.peek();
        String label  = e.getOperator().toString();
        Tree s = context.createTree(INFIX_EXPRESSION_OPERATOR, label);
        PosAndLength pl = searchInfixOperatorPosition(e);
        s.setPos(pl.pos);
        s.setLength(pl.length);
        t.getChildren().add(1, s);
        s.setParent(t);
    }

    private PosAndLength searchInfixOperatorPosition(InfixExpression e) {
        Tree t = this.trees.peek();
        scanner.resetTo(t.getChild(0).getEndPos(), t.getChild(1).getPos());
        int pos = 0;
        int length = 0;
        try {
            int token = scanner.getNextToken();
            while (token != ITerminalSymbols.TokenNameEOF) {
                pos = scanner.getCurrentTokenStartPosition();
                length = scanner.getCurrentTokenEndPosition() - pos + 1;
                break;
            }
        }
        catch (InvalidInputException ex) {
            throw new SyntaxException(null, null, ex);
        }

        return new PosAndLength(pos, length);
    }

    private void handlePostVisit(TypeDeclaration d) {
        String label = "class";
        if (d.isInterface())
            label = "interface";

        Tree s = context.createTree(TYPE_DECLARATION_KIND, label);
        PosAndLength pl = searchTypeDeclarationKindPosition(d);
        s.setPos(pl.pos);
        s.setLength(pl.length);
        int index = 0;
        Tree t = this.trees.peek();
        for (Tree c : t.getChildren()) {
            if (c.getType() != SIMPLE_NAME)
                index++;
            else
                break;
        }
        t.insertChild(s, index);
    }

    private PosAndLength searchTypeDeclarationKindPosition(TypeDeclaration d) {
        int start = d.getStartPosition();
        int end = start + d.getLength();
        scanner.resetTo(start, end);
        int pos = 0;
        int length = 0;
        try {
            int prevToken = -1;
            while (true) {
                int token = scanner.getNextToken();
                if ((token == ITerminalSymbols.TokenNameclass || token == ITerminalSymbols.TokenNameinterface)
                        && prevToken != ITerminalSymbols.TokenNameDOT) {
                    pos = scanner.getCurrentTokenStartPosition();
                    length = scanner.getCurrentTokenEndPosition() - pos + 1;
                    break;
                }
                prevToken = token;
            }
        }
        catch (InvalidInputException e) {
            throw new SyntaxException(null, null, e);
        }
        return new PosAndLength(pos, length);
    }

    public static class PosAndLength {
        public int pos;

        public int length;

        public PosAndLength(int pos, int length) {
            this.pos = pos;
            this.length = length;
        }
    }

}
