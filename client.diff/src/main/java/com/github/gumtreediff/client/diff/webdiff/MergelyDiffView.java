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

package com.github.gumtreediff.client.diff.webdiff;

import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.IOException;

import static org.rendersnake.HtmlAttributesFactory.*;

public class MergelyDiffView implements Renderable {

    private int id;

    public MergelyDiffView(int id) {
        this.id = id;
    }

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        html
        .render(DocType.HTML5)
        .html(lang("en"))
                .render(new Header())
                .body()
                    .div(class_("mergely-full-screen-8"))
                        .div(class_("mergely-resizer"))
                            .div(id("mergely"))._div()
                        ._div()
                    ._div()
                    .macros().script("lhs_url = \"/left/" + id + "\";")
                    .macros().script("rhs_url = \"/right/" + id + "\";")
                    .macros().javascript("/dist/launch-mergely.js")
                ._body()
        ._html();
    }

    private static class Header implements Renderable {
        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            html
                    .head()
                        .macros().javascript(WebDiff.JQUERY_JS_URL)
                        .macros().javascript("https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.32.0/codemirror.min.js")
                        .macros().stylesheet("https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.32.0/codemirror.css")
                        .macros().javascript("https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.32.0/addon/search/searchcursor.min.js")
                        .macros().javascript("/dist/mergely.js")
                        .macros().stylesheet("/dist/mergely.css")
                    ._head();
        }
    }
}
