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

import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.IOException;

import static org.rendersnake.HtmlAttributesFactory.charset;
import static org.rendersnake.HtmlAttributesFactory.name;

public class BootstrapFooterView implements Renderable {

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        html
        .macros().javascript("/dist/jquery.min.js")
        .macros().javascript("/dist/bootstrap.min.js");
    }

}
