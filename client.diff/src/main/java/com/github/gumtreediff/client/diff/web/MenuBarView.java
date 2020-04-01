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
 * Copyright 2020 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.client.diff.web;

import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.IOException;

import static org.rendersnake.HtmlAttributesFactory.*;

public class MenuBarView implements Renderable {
    @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            html
            .div(class_("col"))
                .div(class_("btn-toolbar justify-content-end"))
                    .div(class_("btn-group"))
                        .a(class_("btn btn-primary btn-sm").id("legend").href("#").add("data-toggle", "popover")
                                .add("data-html", "true").add("data-placement", "bottom")
                                .add("data-content", "<span class=&quot;del&quot;>&nbsp;&nbsp;</span> deleted<br>"
                                        + "<span class=&quot;add&quot;>&nbsp;&nbsp;</span> added<br>"
                                        + "<span class=&quot;mv&quot;>&nbsp;&nbsp;</span> moved<br>"
                                        + "<span class=&quot;upd&quot;>&nbsp;&nbsp;</span> updated<br>", false)
                                .add("data-original-title", "Legend").title("Legend").role("button")).content("Legend")
                        .a(class_("btn btn-primary btn-sm").id("shortcuts").href("#").add("data-toggle", "popover")
                                .add("data-html", "true").add("data-placement", "bottom")
                                .add("data-content", "<b>q</b> quit<br><b>l</b> list<br><b>n</b> next<br>"
                                        + "<b>t</b> top<br><b>b</b> bottom", false)
                                .add("data-original-title", "Shortcuts").title("Shortcuts").role("button"))
                            .content("Shortcuts")
                    ._div()
                    .div(class_("btn-group"))
                        .a(class_("btn btn-default btn-sm btn-danger").href("/quit")).content("Quit")
                    ._div()
                ._div()
            ._div();
        }
}
