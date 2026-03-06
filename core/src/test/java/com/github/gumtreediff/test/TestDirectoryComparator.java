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

import java.io.File;

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

    @Test
    public void testPairAndUnpairFiles() {
        DirectoryComparator cmp = new DirectoryComparator("src/test/resources/diff/left",
                "src/test/resources/diff/right");
        cmp.compare();
        assertEquals(1, cmp.getModifiedFiles().size());
        assertEquals(2, cmp.getDeletedFiles().size());
        assertEquals(2, cmp.getAddedFiles().size());

        File srcFile = cmp.getDeletedFiles().stream()
                .filter(f -> f.getName().equals("renamedLeft")).findFirst().get();
        File dstFile = cmp.getAddedFiles().stream()
                .filter(f -> f.getName().equals("renamedRight")).findFirst().get();

        cmp.pairFiles(srcFile, dstFile);
        assertEquals(2, cmp.getModifiedFiles().size());
        assertEquals(1, cmp.getDeletedFiles().size());
        assertEquals(1, cmp.getAddedFiles().size());
        assertEquals("renamedLeft", cmp.getModifiedFiles().get(1).first.getName());
        assertEquals("renamedRight", cmp.getModifiedFiles().get(1).second.getName());

        cmp.unpairFiles(1);
        assertEquals(1, cmp.getModifiedFiles().size());
        assertEquals(2, cmp.getDeletedFiles().size());
        assertEquals(2, cmp.getAddedFiles().size());
    }

    @Test
    public void testPairFilesInvalidArguments() {
        DirectoryComparator cmp = new DirectoryComparator("src/test/resources/diff/left",
                "src/test/resources/diff/right");
        cmp.compare();

        File validDeleted = cmp.getDeletedFiles().iterator().next();
        File validAdded = cmp.getAddedFiles().iterator().next();

        assertThrows(IllegalArgumentException.class, () ->
                cmp.pairFiles(new File("nonexistent"), validAdded));
        assertThrows(IllegalArgumentException.class, () ->
                cmp.pairFiles(validDeleted, new File("nonexistent")));
        assertThrows(IllegalArgumentException.class, () ->
                cmp.unpairFiles(-1));
        assertThrows(IllegalArgumentException.class, () ->
                cmp.unpairFiles(999));
    }
}
