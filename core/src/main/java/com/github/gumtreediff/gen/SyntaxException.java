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
 * Copyright 2019 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.gen;

import java.io.Reader;

/**
 * A class to represent syntax error encountered by tree generators.
 */
public class SyntaxException extends RuntimeException {
    private final TreeGenerator g;
    private final Reader r;

    /**
     * Instantiate a syntax expression encountered by the provided
     * tree generator on the provided reader via the provided cause.
     */
    public SyntaxException(TreeGenerator g, Reader r, Throwable cause) {
        super(cause);
        this.g = g;
        this.r = r;
    }
}
