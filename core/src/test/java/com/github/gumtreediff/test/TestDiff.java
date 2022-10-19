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
 * Copyright 2011-2022 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2022 Floréal Morandat <florealm@gmail.com>
 * Copyright 2011-2022 Raquel Pau <raquelpau@gmail.com>
 */
package com.github.gumtreediff.test;

import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.TreeGenerators;
import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.matchers.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestDiff {

    private static String TREE_GENERATOR_ID = "xml";
    private static String MATCHER_ID = "gumtree-simple";
    private static String PATH_DUMMY_FILE = "src/test/resources/Dummy_v0.xml";

    @BeforeAll
    public static void prepareMatcher() {
        Matcher matcher = new CompositeMatchers.SimpleGumtree();
        Matchers.getInstance().install(matcher.getClass(),
                matcher.getClass().getAnnotation(com.github.gumtreediff.matchers.Register.class));
    }

    @BeforeAll
    public static void prepareTreeGenerator() {
        Register r = TreeIoUtils.XmlInternalGenerator.class.getAnnotation(Register.class);
        TreeGenerators.getInstance().install(TreeIoUtils.XmlInternalGenerator.class, r);
    }

    @Test
    public void testComputeWithReaders() throws IOException {
        FileReader source = new FileReader(PATH_DUMMY_FILE);
        FileReader target = new FileReader(PATH_DUMMY_FILE);

        Diff resultWithReader = Diff.compute(source, target, TREE_GENERATOR_ID, MATCHER_ID,
                new GumtreeProperties());

        Diff resultWithFiles = Diff.compute(PATH_DUMMY_FILE, PATH_DUMMY_FILE, "xml",
                "gumtree-simple",
                new GumtreeProperties());

        assertEquals(resultWithFiles.mappings.size(), resultWithReader.mappings.size());

        assertNoChanges(resultWithReader.mappings);
        assertNoChanges(resultWithFiles.mappings);
    }

    @AfterAll
    public static void clear() {
        Matchers.getInstance().clear();
        TreeGenerators.getInstance().clear();
    }

    private static void assertNoChanges(MappingStore mappings) {
        for (Mapping mapping : mappings) {
            assertEquals(mapping.first.toTreeString(), mapping.second.toTreeString());
        }
    }
}
