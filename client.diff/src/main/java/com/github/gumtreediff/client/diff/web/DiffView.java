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

import com.github.gumtreediff.gen.Generators;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.TreeContext;
import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.File;
import java.io.IOException;

import static org.rendersnake.HtmlAttributesFactory.class_;
import static org.rendersnake.HtmlAttributesFactory.lang;

public class DiffView implements Renderable {

    private HtmlDiffs diffs;

    private File fSrc;

    private File fDst;

    public DiffView(File fSrc, File fDst) throws IOException {
        this.fSrc = fSrc;
        this.fDst = fDst;
        TreeContext src = Generators.getInstance().getTree(fSrc.getAbsolutePath());
        TreeContext dst = Generators.getInstance().getTree(fDst.getAbsolutePath());
        Matcher matcher = Matchers.getInstance().getMatcher(src.getRoot(), dst.getRoot());
        matcher.match();
        diffs = new HtmlDiffs(fSrc, fDst, src, dst, matcher);
        diffs.produce();
    }

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        html
        .render(DocType.HTML5)
        .html(lang("en"))
            .render(new BootstrapHeaderView())
            .body()
                .div(class_("container-fluid"))
                    .div(class_("row"))
                        .render(new MenuBar())
                    ._div()
                    .div(class_("row"))
                        .div(class_("col-lg-6 max-height"))
                            .h5().content(fSrc.getName())
                            .pre(class_("pre max-height")).content(diffs.getSrcDiff(), false)
                        ._div()
                        .div(class_("col-lg-6 max-height"))
                            .h5().content(fDst.getName())
                            .pre(class_("pre max-height")).content(diffs.getDstDiff(), false)
                        ._div()
                    ._div()
                ._div()
                .render(new BootstrapFooterView())
                .macros().javascript("/dist/diff.js")
            ._body()
        ._html();
    }

    private static class MenuBar implements Renderable {

        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            html
            .div(class_("col-lg-12"))
                .div(class_("btn-toolbar pull-right"))
                    .div(class_("btn-group"))
                        .a(class_("btn btn-default btn-xs").id("legend").href("#").add("data-toggle", "popover").add("data-html", "true").add("data-placement", "bottom").add("data-content", "<span class=&quot;del&quot;>&nbsp;&nbsp;</span> deleted<br><span class=&quot;add&quot;>&nbsp;&nbsp;</span> added<br><span class=&quot;mv&quot;>&nbsp;&nbsp;</span> moved<br><span class=&quot;upd&quot;>&nbsp;&nbsp;</span> updated<br>", false).add("data-original-title", "Legend").title("Legend").role("button")).content("Legend")
                        .a(class_("btn btn-default btn-xs").id("shortcuts").href("#").add("data-toggle", "popover").add("data-html", "true").add("data-placement", "bottom").add("data-content", "<b>q</b> quit<br><b>l</b> list<br><b>n</b> next<br><b>t</b> top<br><b>b</b> bottom", false).add("data-original-title", "Shortcuts").title("Shortcuts").role("button")).content("Shortcuts")
                    ._div()
                    .div(class_("btn-group"))
                        .a(class_("btn btn-default btn-xs btn-danger").href("/quit")).content("Quit")
                    ._div()
                ._div()
            ._div();
        }
    }
}
