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
 * Copyright 2020 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.test;

import com.github.gumtreediff.io.DirectoryComparator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestDirectoryComparator {
    @Test
    public void testDirectoryComparatorOnTwoFiles() {
        DirectoryComparator cmp = new DirectoryComparator("src/test/resources/action_v0.xml",
                "src/test/resources/action_v1.xml");
        cmp.compare();
        assertTrue(cmp.getModifiedFiles().size() == 1);
        assertTrue(cmp.getModifiedFiles().get(0).first.getName().equals("action_v0.xml"));
        assertTrue(cmp.getModifiedFiles().get(0).second.getName().equals("action_v1.xml"));
    }

    @Test
    public void testDirectoryComparatorOnTwoFolders() {
        DirectoryComparator cmp = new DirectoryComparator("src/test/resources/diff/left",
                "src/test/resources/diff/right");
        cmp.compare();
        assertTrue(cmp.getModifiedFiles().size() == 1);
        assertTrue(cmp.getModifiedFiles().get(0).first.getName().equals("modified"));
        assertTrue(cmp.getModifiedFiles().get(0).second.getName().equals("modified"));
        assertTrue(cmp.getDeletedFiles().size() == 2);
        assertTrue(cmp.getAddedFiles().size() == 2);
    }

    @Test
    public void testDirectoryComparatorOnNonExistentFiles() {
        assertThrows(IllegalArgumentException.class, () -> {
            DirectoryComparator cmp = new DirectoryComparator("foo",
                    "src/test/resources/action_v0.xml");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            DirectoryComparator cmp = new DirectoryComparator(
                    "src/test/resources/action_v0.xml", "foo");
        });
    }

    @Test
    public void testDirectoryComparatorOnFileAndFolder() {
        assertThrows(IllegalArgumentException.class, () -> {
            DirectoryComparator cmp = new DirectoryComparator("src/test/resources",
                    "src/test/resources/action_v0.xml");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            DirectoryComparator cmp = new DirectoryComparator("src/test/resources/action_v0.xml",
                    "src/test/resources");
        });
    }
}
