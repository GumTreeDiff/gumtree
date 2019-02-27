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
 * Copyright 2016 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.gen.css;

import com.github.gumtreediff.io.LineReader;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.helger.css.CSSSourceLocation;
import com.helger.css.ICSSSourceLocationAware;
import com.helger.css.ICSSWriterSettings;
import com.helger.css.decl.*;
import com.helger.css.decl.visit.ICSSVisitor;
import com.helger.css.writer.CSSWriterSettings;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Stack;

public class GtCssVisitor implements ICSSVisitor {

    private TreeContext ctx;

    private ArrayDeque<ITree> trees;

    private LineReader lr;

    private ICSSWriterSettings settings;

    private CascadingStyleSheet sheet;

    public GtCssVisitor(CascadingStyleSheet sheet, LineReader lr) throws IOException {
        this.lr = lr;
        this.settings = new CSSWriterSettings();
        this.sheet = sheet;
        this.ctx = new TreeContext();
        this.trees = new ArrayDeque<>();
        ITree root = this.ctx.createTree(hashCode(sheet), "", "CascadingStyleSheet");
        setLocation(root, sheet);
        this.ctx.setRoot(root);
        this.trees.push(root);
    }

    public TreeContext getTreeContext() {
        return ctx;
    }

    private void setLocation(ITree t, ICSSSourceLocationAware a) {
        CSSSourceLocation l = a.getSourceLocation();
        int pos = lr.positionFor(l.getFirstTokenBeginLineNumber(), l.getFirstTokenBeginColumnNumber());
        int end = lr.positionFor(l.getLastTokenEndLineNumber(), l.getLastTokenEndColumnNumber());
        int length = end - pos + 1;
        t.setPos(pos + 1);
        t.setLength(length);
    }

    private int hashCode(ICSSSourceLocationAware a) {
        return a.getClass().getName().hashCode();
    }

    @Override
    public void begin() {
    }

    @Override
    public void onImport(@Nonnull CSSImportRule i) {
        //TODO add media nodes
        ITree t = ctx.createTree(hashCode(i), i.getAsCSSString(settings, 0), "CSSImportRule");
        t.setParentAndUpdateChildren(trees.peekFirst());
        setLocation(t, i);
    }

    @Override
    public void onNamespace(@Nonnull CSSNamespaceRule n) {

    }

    @Override
    public void onDeclaration(@Nonnull CSSDeclaration d) {
        ITree t = ctx.createTree(hashCode(d), d.getProperty(), "CSSDeclaration");
        t.setParentAndUpdateChildren(trees.peekFirst());
        setLocation(t, d);
        CSSExpression e = d.getExpression();
        ITree c = ctx.createTree(hashCode(e), e.getAsCSSString(settings, 0), "CSSExpression");
        c.setParentAndUpdateChildren(t);
        setLocation(c, e);
        //TODO handle expression members.
        /*
        CSSExpression expr = aDeclaration.getExpression();
        for (ICSSExpressionMember member : expr.getAllMembers()) {
            ITree m = ctx.createTree(6, member.getAsCSSString(settings, 0), "CSSExpressionMember");
            m.setParentAndUpdateChildren(c);
        }
        */
    }

    @Override
    public void onBeginStyleRule(@Nonnull CSSStyleRule s) {
        ITree t = ctx.createTree(hashCode(s), "", "CSSStyleRule");
        setLocation(t, s);
        t.setParentAndUpdateChildren(trees.peekFirst());
        trees.addFirst(t);
    }

    @Override
    public void onStyleRuleSelector(@Nonnull CSSSelector s) {
        ITree t = ctx.createTree(hashCode(s), s.getAsCSSString(settings, 0), "CSSSelector");
        t.setParentAndUpdateChildren(trees.peekFirst());
        setLocation(t, s);
    }

    @Override
    public void onEndStyleRule(@Nonnull CSSStyleRule aStyleRule) {
        trees.removeFirst();
    }

    @Override
    public void onBeginPageRule(@Nonnull CSSPageRule aPageRule) {

    }

    @Override
    public void onBeginPageMarginBlock(@Nonnull CSSPageMarginBlock aPageMarginBlock) {

    }

    @Override
    public void onEndPageMarginBlock(@Nonnull CSSPageMarginBlock aPageMarginBlock) {

    }

    @Override
    public void onEndPageRule(@Nonnull CSSPageRule aPageRule) {

    }

    @Override
    public void onBeginFontFaceRule(@Nonnull CSSFontFaceRule aFontFaceRule) {

    }

    @Override
    public void onEndFontFaceRule(@Nonnull CSSFontFaceRule aFontFaceRule) {

    }

    @Override
    public void onBeginMediaRule(@Nonnull CSSMediaRule aMediaRule) {

    }

    @Override
    public void onEndMediaRule(@Nonnull CSSMediaRule aMediaRule) {

    }

    @Override
    public void onBeginKeyframesRule(@Nonnull CSSKeyframesRule aKeyframesRule) {

    }

    @Override
    public void onBeginKeyframesBlock(@Nonnull CSSKeyframesBlock aKeyframesBlock) {

    }

    @Override
    public void onEndKeyframesBlock(@Nonnull CSSKeyframesBlock aKeyframesBlock) {

    }

    @Override
    public void onEndKeyframesRule(@Nonnull CSSKeyframesRule aKeyframesRule) {

    }

    @Override
    public void onBeginViewportRule(@Nonnull CSSViewportRule aViewportRule) {

    }

    @Override
    public void onEndViewportRule(@Nonnull CSSViewportRule aViewportRule) {

    }

    @Override
    public void onBeginSupportsRule(@Nonnull CSSSupportsRule aSupportsRule) {

    }

    @Override
    public void onEndSupportsRule(@Nonnull CSSSupportsRule aSupportsRule) {

    }

    @Override
    public void onUnknownRule(@Nonnull CSSUnknownRule aUnknownRule) {

    }

    @Override
    public void end() {

    }
}
