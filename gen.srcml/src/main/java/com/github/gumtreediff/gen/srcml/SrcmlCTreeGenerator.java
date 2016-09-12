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
 * Copyright 2016 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.gen.srcml;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TreeContext.MetadataSerializers;
import com.github.gumtreediff.tree.TreeContext.MetadataUnserializers;

import java.io.*;
import java.util.Arrays;
import java.util.regex.Pattern;

@Register(id = "c-srcml", accept = "\\.[ch]$")
public class SrcmlCTreeGenerator extends AbstractSrcmlTreeGenerator {

    @Override
    public String getLanguage() {
        return "C";
    }
}
