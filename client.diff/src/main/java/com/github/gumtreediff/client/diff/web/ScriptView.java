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

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.gen.Generators;
import com.github.gumtreediff.io.ActionsIoUtils;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.TreeContext;
import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.rendersnake.HtmlAttributesFactory.class_;
import static org.rendersnake.HtmlAttributesFactory.lang;

public class ScriptView implements Renderable {

    private final MappingStore mappings;

    private final TreeContext src;

    private final TreeContext dst;

    private File fSrc;

    private File fDst;

    private List<Action> script;

    public ScriptView(File fSrc, File fDst) throws IOException {
        this.fSrc = fSrc;
        this.fDst = fDst;
        src = Generators.getInstance().getTree(fSrc.getAbsolutePath());
        dst = Generators.getInstance().getTree(fDst.getAbsolutePath());
        Matcher matcher = Matchers.getInstance().getMatcher(src.getRoot(), dst.getRoot());
        matcher.match();
        mappings = matcher.getMappings();
        ActionGenerator g = new ActionGenerator(src.getRoot(), dst.getRoot(), mappings);
        g.generate();
        this.script = g.getActions();
    }

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        html
            .render(DocType.HTML5)
            .html(lang("en"))
                .render(new BootstrapHeaderView())
                .body()
                    .div(class_("container"))
                        .div(class_("row"))
                            .div(class_("col-lg-12"))
                                .h3()
                                    .write("Script ")
                                    .small().content(String.format("%s -> %s", fSrc.getName(), fDst.getName()))
                                ._h3()
                                .pre().content(ActionsIoUtils.toText(src, this.script, mappings).toString())
                            ._div()
                        ._div()
                    ._div()
                    .render(new BootstrapFooterView())
                    .macros().javascript("/dist/script.js")
                ._body()
            ._html();
    }

}
