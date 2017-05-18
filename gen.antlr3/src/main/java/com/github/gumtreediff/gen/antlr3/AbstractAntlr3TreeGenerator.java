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

package com.github.gumtreediff.gen.antlr3;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.*;
import org.antlr.runtime.tree.CommonTree;

import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

public abstract class AbstractAntlr3TreeGenerator<L extends Lexer, P extends Parser> extends TreeGenerator {

    private Deque<ITree> trees = new ArrayDeque<>();

    protected static Map<Integer, Integer> chars;

    protected TokenStream tokens;

    public AbstractAntlr3TreeGenerator() {
    }

    protected CommonTree getStartSymbol(Reader r) throws RecognitionException, IOException {
        ANTLRStringStream stream = getAntlrStream(r);
        L lexer = getLexer(stream);
        tokens = getTokenStream(lexer);
        P parser = getParser(tokens);
        return getTreeFromRule(getStartRule(parser));
    }

    protected abstract L getLexer(ANTLRStringStream stream);

    protected abstract P getParser(TokenStream tokens);

    protected abstract RuleReturnScope getStartRule(P parser) throws RecognitionException;

    protected CommonTree getTreeFromRule(RuleReturnScope startRule) {
        return (CommonTree) startRule.getTree();
    }

    protected ANTLRStringStream getAntlrStream(Reader r) throws IOException {
        return new ANTLRReaderStream(r);
    }

    protected TokenStream getTokenStream(L lexer) {
        return new TokenRewriteStream(lexer);
    }

    @Override
    public TreeContext generate(Reader r) throws IOException {
        try {
            CommonTree ct = getStartSymbol(r);
            TreeContext context = new TreeContext();
            buildTree(context, ct);
            return context;
        } catch (RecognitionException e) {
            System.out.println("at " + e.line + ":" + e.charPositionInLine);
            e.printStackTrace();
        }
        return null;
    }

    protected abstract String[] getTokenNames();

    protected String getTokenName(int tokenType) {
        String[] names = getTokenNames();
        if (tokenType < 0 || tokenType >= names.length)
            return ITree.NO_LABEL;
        return names[tokenType];
    }

    @SuppressWarnings("unchecked")
    protected void buildTree(TreeContext context, CommonTree ct) {
        int type = ct.getType();
        String tokenName = getTokenName(type);
        String label = ct.getText();
        if (tokenName.equals(label))
            label = ITree.NO_LABEL;

        ITree t = context.createTree(type, label, tokenName);

        int start = startPos(ct.getTokenStartIndex());
        int stop = stopPos(ct.getTokenStopIndex());
        t.setPos(start);
        t.setLength(stop - start + 1); // FIXME check if this + 1 make sense ?

        if (trees.isEmpty())
            context.setRoot(t);
        else
            t.setParentAndUpdateChildren(trees.peek());

        if (ct.getChildCount() > 0) {
            trees.push(t);
            for (CommonTree cct : (List<CommonTree>) ct.getChildren())
                buildTree(context, cct);
            trees.pop();
        }
    }

    private int startPos(int tkPosition) {
        if (tkPosition == -1) return 0;
        Token tk = tokens.get(tkPosition);
        if (tk instanceof CommonToken)
            return ((CommonToken)tk).getStartIndex();
        return 0;
    }

    private int stopPos(int tkPosition) {
        if (tkPosition == -1) return 0;
        Token tk = tokens.get(tkPosition);
        if (tk instanceof CommonToken)
            return ((CommonToken)tk).getStopIndex();
        return 0;
    }
}
