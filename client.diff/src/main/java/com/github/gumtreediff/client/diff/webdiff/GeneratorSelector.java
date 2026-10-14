/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2026 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.client.diff.webdiff;

import com.github.gumtreediff.gen.TreeGenerators;
import j2html.tags.Tag;

import static j2html.TagCreator.*;

final class GeneratorSelector {

    private GeneratorSelector() {
    }

    static Tag build(String selectedGeneratorId, String returnTo) {
        return form(
                input().withType("hidden").withName("returnTo").withValue(returnTo),
                label("Tree generator").withFor("generator").withClass("input-group-text"),
                select(
                        selectedOption("Default", "", selectedGeneratorId == null),
                        each(TreeGenerators.getInstance().getEntries(), entry ->
                                selectedOption(entry.id, entry.id, entry.id.equals(selectedGeneratorId))
                        )
                ).withId("generator").withName("generator").withClass("form-select form-select-sm"),
                button("Apply").withType("submit").withClass("btn btn-primary btn-sm")
        ).withAction("/generator").withMethod("post")
                .withClasses("input-group", "input-group-sm", "mb-0")
                .withStyle("min-height: 31px; align-self: center;");
    }

    private static Tag selectedOption(String label, String value, boolean selected) {
        var option = option(label).withValue(value);
        if (selected)
            option.attr("selected", "selected");
        return option;
    }
}
