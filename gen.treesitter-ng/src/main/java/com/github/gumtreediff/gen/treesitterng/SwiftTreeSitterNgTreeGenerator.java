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
 * Copyright 2021 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.gen.treesitterng;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.utils.Registry;
import org.treesitter.TSLanguage;
import org.treesitter.TreeSitterSwift;

@Register(id = "swift-treesitter-ng", accept = "\\.swift$", priority = Registry.Priority.HIGH)
public final class SwiftTreeSitterNgTreeGenerator extends AbstractTreeSitterNgGenerator {
    public static final TSLanguage SWIFT_TREE_SITTER_LANGUAGE = new TreeSitterSwift();
    private static final String SWIFT_LANGUAGE_NAME = "swift";

    @Override
    protected TSLanguage getTreeSitterLanguage() {
        return SWIFT_TREE_SITTER_LANGUAGE;
    }

    @Override
    protected String getLanguageName() {
        return SWIFT_LANGUAGE_NAME;
    }
}
