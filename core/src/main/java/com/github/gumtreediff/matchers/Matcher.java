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

package com.github.gumtreediff.matchers;

import com.github.gumtreediff.tree.ITree;
import org.atteo.classindex.IndexSubclasses;

@IndexSubclasses
public abstract class Matcher {
    protected final ITree src;

    protected final ITree dst;

    protected final MappingStore mappings;

    public Matcher(ITree src, ITree dst, MappingStore mappings) {
        this.src = src;
        this.dst = dst;
        this.mappings = mappings;
    }

    public abstract void match();

    public MappingStore getMappings() {
        return mappings;
    }

    public boolean isMappingAllowed(ITree src, ITree dst) {
        return src.hasSameType(dst) && mappings.areBothUnmapped(src, dst);
    }
}
