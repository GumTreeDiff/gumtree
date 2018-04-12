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

package com.github.gumtreediff.client.diff.web;

import com.github.gumtreediff.actions.RootAndLeavesClassifier;
import com.github.gumtreediff.actions.TreeClassifier;
import com.github.gumtreediff.utils.StringAlgorithms;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

public final class HtmlDiffs {

    private static final String SRC_MV_SPAN = "<span class=\"%s\" id=\"move-src-%d\" data-title=\"%s\">";
    private static final String DST_MV_SPAN = "<span class=\"%s\" id=\"move-dst-%d\" data-title=\"%s\">";
    private static final String ADD_DEL_SPAN = "<span class=\"%s\" data-title=\"%s\">";
    private static final String UPD_SPAN = "<span class=\"cupd\">";
    private static final String ID_SPAN = "<span class=\"marker\" id=\"mapping-%d\"></span>";
    private static final String END_SPAN = "</span>";

    private String srcDiff;

    private String dstDiff;

    private TreeContext src;

    private TreeContext dst;

    private File fSrc;

    private File fDst;

    private Matcher matcher;

    private MappingStore mappings;

    public HtmlDiffs(File fSrc, File fDst, TreeContext src, TreeContext dst, Matcher matcher) {
        this.fSrc = fSrc;
        this.fDst = fDst;
        this.src = src;
        this.dst = dst;
        this.matcher = matcher;
        this.mappings = matcher.getMappings();
    }

    public void produce() throws IOException {
        TreeClassifier c = new RootAndLeavesClassifier(src, dst, matcher);
        TIntIntMap mappingIds = new TIntIntHashMap();

        int uId = 1;
        int mId = 1;

        TagIndex ltags = new TagIndex();
        for (ITree t: src.getRoot().getTrees()) {
            if (c.getSrcMvTrees().contains(t)) {
                mappingIds.put(mappings.getDst(t).getId(), mId);
                ltags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                ltags.addTags(t.getPos(), String.format(
                                SRC_MV_SPAN, "token mv", mId++, tooltip(src, t)), t.getEndPos(), END_SPAN);
            }
            if (c.getSrcUpdTrees().contains(t)) {
                mappingIds.put(mappings.getDst(t).getId(), mId);
                ltags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                ltags.addTags(t.getPos(), String.format(
                                SRC_MV_SPAN, "token upd", mId++, tooltip(src, t)), t.getEndPos(), END_SPAN);
                List<int[]> hunks = StringAlgorithms.hunks(t.getLabel(), mappings.getDst(t).getLabel());
                for (int[] hunk: hunks)
                    ltags.addTags(t.getPos() + hunk[0], UPD_SPAN, t.getPos() + hunk[1], END_SPAN);

            }
            if (c.getSrcDelTrees().contains(t)) {
                ltags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                ltags.addTags(t.getPos(), String.format(
                                ADD_DEL_SPAN, "token del", tooltip(src, t)), t.getEndPos(), END_SPAN);
            }
        }

        TagIndex rtags = new TagIndex();
        for (ITree t: dst.getRoot().getTrees()) {
            if (c.getDstMvTrees().contains(t)) {
                int dId = mappingIds.get(t.getId());
                rtags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                rtags.addTags(t.getPos(), String.format(
                                DST_MV_SPAN, "token mv", dId, tooltip(dst, t)), t.getEndPos(), END_SPAN);
            }
            if (c.getDstUpdTrees().contains(t)) {
                int dId = mappingIds.get(t.getId());
                rtags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                rtags.addTags(t.getPos(), String.format(
                                DST_MV_SPAN, "token upd", dId, tooltip(dst, t)), t.getEndPos(), END_SPAN);
                List<int[]> hunks = StringAlgorithms.hunks(mappings.getSrc(t).getLabel(), t.getLabel());
                for (int[] hunk: hunks)
                    rtags.addTags(t.getPos() + hunk[2], UPD_SPAN, t.getPos() + hunk[3], END_SPAN);
            }
            if (c.getDstAddTrees().contains(t)) {
                rtags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                rtags.addTags(t.getPos(), String.format(
                                ADD_DEL_SPAN, "token add", tooltip(dst, t)), t.getEndPos(), END_SPAN);
            }
        }

        String charset = System.getProperty("gt.charset.decoding", "UTF-8");
        StringWriter w1 = new StringWriter();
        BufferedReader r = Files.newBufferedReader(fSrc.toPath(), Charset.forName(charset));
        int cursor = 0;

        while (r.ready()) {
            char cr = (char) r.read();
            w1.append(ltags.getEndTags(cursor));
            w1.append(ltags.getStartTags(cursor));
            append(cr, w1);
            cursor++;
        }
        w1.append(ltags.getEndTags(cursor));
        r.close();
        srcDiff = w1.toString();

        StringWriter w2 = new StringWriter();
        r = Files.newBufferedReader(fDst.toPath(), Charset.forName(charset));
        cursor = 0;

        while (r.ready()) {
            char cr = (char) r.read();
            w2.append(rtags.getEndTags(cursor));
            w2.append(rtags.getStartTags(cursor));
            append(cr, w2);
            cursor++;
        }
        w2.append(rtags.getEndTags(cursor));
        r.close();

        dstDiff = w2.toString();
    }

    public String getSrcDiff() {
        return srcDiff;
    }

    public String getDstDiff() {
        return dstDiff;
    }

    private static String tooltip(TreeContext ctx, ITree t) {
        return (t.getParent() != null)
                ? ctx.getTypeLabel(t.getParent()) + "/" + ctx.getTypeLabel(t) : ctx.getTypeLabel(t);
    }

    private static void append(char cr, Writer w) throws IOException {
        if (cr == '<') w.append("&lt;");
        else if (cr == '>') w.append("&gt;");
        else if (cr == '&') w.append("&amp;");
        else w.append(cr);
    }
}
