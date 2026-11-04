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
package com.github.gumtreediff.gen.cdt;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.utils.Registry;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;

@Register(id = "cpp-cdt", accept = "\\.(C|CC|cpp|cc|cxx|H|hh|hpp|hxx)$", priority = Registry.Priority.LOW)
public final class CppCdtTreeGenerator extends AbstractCdtTreeGenerator {
    public CppCdtTreeGenerator() {
        super(GPPLanguage.getDefault(), "input.cpp");
    }
}
