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

package com.github.gumtreediff.gen.antlr4;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.Type;
import com.github.gumtreediff.tree.TreeContext;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import static com.github.gumtreediff.tree.TypeSet.type;

public abstract class AbstractAntlr4TreeGenerator extends TreeGenerator {

    private Deque<Tree> trees = new ArrayDeque<>();

    protected static Map<Integer, Integer> chars;

    protected CommonTokenStream tokens;

    public AbstractAntlr4TreeGenerator() {
    }

    protected abstract ParseTree getStartSymbol(Reader r) throws RecognitionException, IOException;

    @Override
    public TreeContext generate(Reader r) throws IOException {
        try {
            ParseTree pt = getStartSymbol(r);
            TreeContext context = new TreeContext();
            buildTree(context, pt);
            return context;
        } catch (RecognitionException e) {
            //System.out.println("at " + e.line + ":" + e.charPositionInLine);
            e.printStackTrace();
        }
        return null;
    }

    protected abstract String[] getTokenNames();

    protected abstract String[] getRuleNames();

    protected Type getTokenName(int tokenType) {
        String[] names = getTokenNames();
        if (tokenType < 0 || tokenType >= names.length)
            return Type.NO_TYPE;
        return type(names[tokenType]);
    }

    protected Type getRuleName(int ruleType) {
        String[] names = getRuleNames();
        if (ruleType < 0 || ruleType >= names.length)
            return Type.NO_TYPE;
        return type(names[ruleType]);
    }

    @SuppressWarnings("unchecked")
    protected void buildTree(TreeContext context, ParseTree pt) {
        Object payload = pt.getPayload(); //makeOrGetType();
        Type type = null;
        if (payload instanceof Token)
            type = getTokenName(((Token)payload).getType());
        else if (payload instanceof RuleContext)
            type = getRuleName(((RuleContext)payload).getRuleIndex());

        String label = pt.getText();
        if (type.name.equals(label)) // FIXME
            label = Tree.NO_LABEL;

        Tree t = context.createTree(type, label);

//        int start = startPos(pt.getSourceInterval().a);
//        int stop = stopPos(pt.getSourceInterval().b);
        int start = pt.getSourceInterval().a;
        int stop = pt.getSourceInterval().b;
        t.setPos(start);
        t.setLength(stop - start + 1); // FIXME check if this + 1 make sense ?

        if (trees.isEmpty())
            context.setRoot(t);
        else
            t.setParentAndUpdateChildren(trees.peek());

        if (pt.getChildCount() > 0) {
            trees.push(t);
            for (int i = 0; i < pt.getChildCount(); i ++)
                buildTree(context, pt.getChild(i));
            trees.pop();
        }
    }

    private int startPos(int tkPosition) {
        if (tkPosition == -1) return 0;
        Token tk = tokens.get(tkPosition);
        return tk.getStartIndex();
    }

    private int stopPos(int tkPosition) {
        if (tkPosition == -1) return 0;
        Token tk = tokens.get(tkPosition);
        return tk.getStopIndex();
    }
}
