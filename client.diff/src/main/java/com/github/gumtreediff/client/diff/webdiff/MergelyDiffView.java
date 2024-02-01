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
                    div().withId("compare").withStyle("width: 100%; height: 100%;")
                ),
                script("lhs_url = \"/left/" + id + "\";" + "rhs_url = \"/right/" + id + "\";").withType("text/javascript"),
                script().withSrc("/dist/launch-mergely.js").withType("text/javascript")
            )
        ).withLang("en");
    }

    private static class Header {
        public final static String MERGELY_JS_URL = "https://cdnjs.cloudflare.com/ajax/libs/mergely/5.0.0/mergely.min.js";

        public final static String MERGELY_CSS_URL = "https://cdnjs.cloudflare.com/ajax/libs/mergely/5.0.0/mergely.css";


        public static Tag build() {
            return head(
                script().withSrc(MERGELY_JS_URL).withType("text/javascript"),
                link().withHref(MERGELY_CSS_URL).withType("text/css").withRel("stylesheet")
            );
        }
    }
}
