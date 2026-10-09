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
 * Copyright 2026 GumTree contributors
 */

package com.github.gumtreediff.client.diff.dotdiff;

import com.github.gumtreediff.client.Option;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DotDiffTest {
    @Test
    void acceptsConfiguredMaximumLabelLength() {
        DotDiff.DotDiffOptions options = new DotDiff.DotDiffOptions();
        Option.processCommandLine(new String[]{"--max-label-length", "0"}, options);
        assertEquals(0, options.maxLabelLength);
    }

    @Test
    void rejectsNegativeMaximumLabelLength() {
        DotDiff.DotDiffOptions options = new DotDiff.DotDiffOptions();
        assertThrows(Option.OptionException.class,
                () -> Option.processCommandLine(new String[]{"--max-label-length", "-1"}, options));
    }

    @Test
    void preservesAndEscapesFullLabelWhenTruncationIsDisabled() {
        assertEquals("Type: \\\"value\\\" \\\\path\\nnext",
                DotDiff.formatLabel("Type: \"value\" \\path\nnext", 0));
    }

    @Test
    void truncatesLabelsToConfiguredLengthWithoutSplittingCodePoints() {
        assertEquals("abc...", DotDiff.formatLabel("abcdefgh", 6));
        assertEquals("ab😀", DotDiff.formatLabel("ab😀cd", 3));
    }
}
