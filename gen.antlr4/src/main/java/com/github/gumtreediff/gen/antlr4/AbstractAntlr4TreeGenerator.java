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
 * Copyright 2017 Svante Schubert <svante.schubert gmail com>
 */
package com.github.gumtreediff.gen.antlr4;

import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

public abstract class AbstractAntlr4TreeGenerator<L extends Lexer, P extends Parser> extends TreeGenerator {

    private static final Logger LOG = Logger.getLogger(AbstractAntlr4TreeGenerator.class.getName());

    private final Deque<ITree> trees = new ArrayDeque<>();
    protected CommonTokenStream tokens;
    protected HashMap<Integer, String> rules = new HashMap<Integer, String>();
    private Vocabulary vocabulary;
    private static final Boolean DEBUG_ENABLED = Boolean.FALSE;

    protected ParserRuleContext getParseTreeRoot(Reader r)
            throws org.antlr.v4.runtime.RecognitionException, IOException {
        CharStream stream = CharStreams.fromReader(r);
        L lexer = getLexer(stream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        P parser = getParser(tokens);
        // optimization taken from
        // https://github.com/antlr/antlr4/blob/master/doc/faq/general.md
        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);

        setVocabulary(getVocabulary(lexer));
        tokens = getTokenStream(lexer);
        // otherwise rule contexts only point upwards
        parser.setBuildParseTree(true);

        ParserRuleContext pctx = null;
        try {
            pctx = getStartRule(parser);  // STAGE 1
        } catch (Exception ex) {
            if (DEBUG_ENABLED) {
                System.err.println("SSL failed, trying LL..");
            }
            LOG.info("Parsing with prediction mode SSL didn't work out.. \n");
            tokens.seek(0); // rewind input stream
            LOG.info("Rewind input stream & resetting parser.. \n");
            parser.reset();
            parser.getInterpreter().setPredictionMode(PredictionMode.LL);
            LOG.info("Changing parser's prediction mode to LL.. \n");
            LOG.info("2nd try.. \n");
            pctx = getStartRule(parser);  // STAGE 2
            // if we parse ok, it's LL not SLL
        }

        // reverse name->index rule map
        for (Map.Entry<String, Integer> entry : parser.getRuleIndexMap().entrySet()) {
            LOG.info("Key: " + entry.getKey() + "\tValue: " + entry.getValue());
            rules.put(entry.getValue(), entry.getKey());
        }
        return pctx;
    }

    protected abstract L getLexer(CharStream stream);

    protected abstract P getParser(CommonTokenStream tokens);

    protected abstract ParserRuleContext getStartRule(P parser) throws org.antlr.v4.runtime.RecognitionException;

    protected CommonTokenStream getTokenStream(L lexer) {
        return new CommonTokenStream(lexer);
    }

    @Override
    public TreeContext generate(Reader r) throws IOException {
        try {
            ParserRuleContext startRule = getParseTreeRoot(r);
            TreeContext context = new TreeContext();
            buildGumTree(context, startRule);
            return context;
        } catch (org.antlr.v4.runtime.RecognitionException e) {
            LOG.log(Level.SEVERE, e.toString(), e);
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    /**
     * By default all rules and tokens from ANTLR4 are being added to the
     * GumTree
     */
    protected void buildGumTree(TreeContext context, ParseTree node) {
        // Labels of nodes correspond to the
        // name of their production rule in the grammar
        String label = "";
        int type = Integer.MIN_VALUE;
        Token token;
        ITree t = null;
        if (node instanceof TerminalNodeImpl) {
            token = ((TerminalNode) node).getSymbol();
            type = token.getType();
            String tokenSymbolicName = getVocabulary().getSymbolicName(type);

            String tokenDisplayName = getVocabulary().getDisplayName(type);
            String tokenLiteralName = getVocabulary().getLiteralName(type);
            label = token.getText();
            if (DEBUG_ENABLED) {
                System.out.println("\n****************************************");
                System.out.println("tokenSymbolicName " + tokenSymbolicName);
                System.out.println("tokenDisplayName " + tokenDisplayName);
                System.out.println("tokenLiteralName " + tokenLiteralName);
                System.out.println("label / token.getText() '" + label + "'");
            }

            if (tokenSymbolicName.equals(label)) {
                label = ITree.NO_LABEL;
            }
            t = context.createTree(type, label, tokenSymbolicName);
            int start = token.getStartIndex();
            t.setPos(start);
            t.setLength(token.getStopIndex() - start + 1);
        } else if (node instanceof ParserRuleContext) {
            ParserRuleContext ctx = ((ParserRuleContext) node);
            type = ctx.getRuleIndex();
            // Some simple rules extend rules without changing rule index,
            // if that is the case, set node name to that parent.
            label = rules.get(type);
            t = context.createTree(type * -1, label, null);
        } else {
            throw new RuntimeException("Check out: Found a node being no token nor rule!");
        }

        if (trees.isEmpty()) {
            context.setRoot(t);
        } else {
            t.setParentAndUpdateChildren(trees.peek());
        }

        if (node.getChildCount() > 0) {
            trees.push(t);
            for (int i = 0; node.getChildCount() > i; i++) {
                buildGumTree(context, node.getChild(i));
            }
            trees.pop();
        }
    }

    protected Vocabulary getVocabulary(L lexer) {
        return lexer.getVocabulary();
    }

    /**
     * @return the vocabulary
     */
    public Vocabulary getVocabulary() {
        return vocabulary;
    }

    /**
     * @param vocabulary the vocabulary to set
     */
    public void setVocabulary(Vocabulary vocabulary) {
        this.vocabulary = vocabulary;
    }
}
