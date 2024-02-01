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

import j2html.tags.Tag;
import j2html.tags.specialized.HtmlTag;

import static j2html.TagCreator.*;

public class MergelyDiffView {
    public static HtmlTag build(int id) {
        return html(
            Header.build(),
            body(
                div(
                    div(div().withId("mergely")).withClass("mergely-resizer")
                ).withClass("mergely-full-screen-8"),
                script("lhs_url = \"/left/" + id + "\";" + "rhs_url = \"/right/" + id + "\";")
                        .withType("text/javascript"),
                script().withSrc("/dist/launch-mergely.js").withType("text/javascript")
            )
        ).withLang("en");
    }

    private static class Header {
        public static final String CODE_MIRROR_JS_URL
                = "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.32.0/codemirror.min.js";
        public final static String CODE_MIRROR_CSS_URL
                = "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.32.0/codemirror.css";
        public final static String SEARCH_CURSOR_JS_URL
                = "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.32.0/addon/search/searchcursor.min.js";
        public static Tag build() {
            return head(
                script().withSrc(WebDiff.JQUERY_JS_URL).withType("text/javascript"),
                script().withSrc(CODE_MIRROR_JS_URL).withType("text/javascript"),
                link().withHref(CODE_MIRROR_CSS_URL).withType("text/css").withRel("stylesheet"),
                script().withSrc(SEARCH_CURSOR_JS_URL).withType("text/javascript"),
                script().withSrc("/dist/mergely.js").withType("text/javascript"),
                link().withHref("/dist/mergely.css").withType("text/css").withRel("stylesheet")
            );
        }
    }
}
