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
 * along with GumTree. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2022 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.gen.treesitter;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.utils.Registry;

@Register(id = "cs-treesitter", accept = "\\.[cs]$", priority = Registry.Priority.HIGH)
public class CSharpTreeSitterTreeGenerator extends AbstractTreeSitterGenerator {
    private static final String CSHARP_PARSER_NAME = "csharp";

    @Override
    public String getParserName() {
        return CSHARP_PARSER_NAME;
    }
}