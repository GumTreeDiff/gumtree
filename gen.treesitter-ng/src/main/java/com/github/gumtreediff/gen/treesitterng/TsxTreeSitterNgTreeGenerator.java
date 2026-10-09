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
 * Copyright 2026 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.gen.treesitterng;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.utils.Registry;
import org.treesitter.TSLanguage;
import org.treesitter.TreeSitterTsx;

@Register(id = "tsx-treesitter-ng", accept = {"\\.tsx$"}, priority = Registry.Priority.MAXIMUM)
public final class TsxTreeSitterNgTreeGenerator extends AbstractTreeSitterNgGenerator {
    public static final TSLanguage TSX_TREE_SITTER_LANGUAGE = new TreeSitterTsx();
    private static final String TSX_LANGUAGE_NAME = "tsx";

    @Override
    protected TSLanguage getTreeSitterLanguage() {
        return TSX_TREE_SITTER_LANGUAGE;
    }

    @Override
    protected String getLanguageName() {
        return TSX_LANGUAGE_NAME;
    }
}
