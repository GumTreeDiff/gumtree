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

package com.github.gumtreediff.io;

import com.github.gumtreediff.utils.Pair;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DirectoryComparator {

    private Path src;

    private Path dst;

    private List<Pair<File, File>> modifiedFiles;

    private Set<File> deletedFiles;

    private  Set<File> addedFiles;

    private boolean dirMode = true;

    public DirectoryComparator(String src, String dst) {
        this.src = Paths.get(src);
        this.dst = Paths.get(dst);

        if (!Files.exists(this.src))
            throw new IllegalArgumentException("File " + this.src + " does not exist.");
        if (!Files.exists(this.dst))
            throw new IllegalArgumentException("File " + this.dst + " does not exist.");
        if ((Files.isDirectory(this.src) && Files.isRegularFile(this.dst))
                || (Files.isRegularFile(this.src) && Files.isDirectory(this.dst)))
            throw new IllegalArgumentException("File " + this.src + " and " + this.dst
                + "are not of the same type (file and folder).");

        modifiedFiles = new ArrayList<>();
        addedFiles = new HashSet<>();
        deletedFiles =  new HashSet<>();

        if (!(Files.isDirectory(this.src) || Files.isDirectory(this.dst))) {
            this.modifiedFiles.add(new Pair<>(this.src.toFile(), this.dst.toFile()));

            this.src = this.src.toAbsolutePath().getParent(); // avoid null parents
            this.dst = this.dst.toAbsolutePath().getParent(); // avoid null parents
            this.dirMode = false;
        }
    }

    public void compare() {
        if (!dirMode)
            return;

        AllFilesVisitor vSrc = new AllFilesVisitor(src);
        AllFilesVisitor vDst = new AllFilesVisitor(dst);
        try {
            Files.walkFileTree(src, vSrc);
            Files.walkFileTree(dst, vDst);

            Set<String> addedFiles = new HashSet<>();
            addedFiles.addAll(vDst.files);
            addedFiles.removeAll(vSrc.files);
            for (String file : addedFiles)
                this.addedFiles.add(toDstFile(file));

            Set<String> deletedFiles = new HashSet<>();
            deletedFiles.addAll(vSrc.files);
            deletedFiles.removeAll(vDst.files);
            for (String file : deletedFiles)
                this.deletedFiles.add(toSrcFile(file));

            Set<String> commonFiles = new HashSet<>();
            commonFiles.addAll(vSrc.files);
            commonFiles.retainAll(vDst.files);

            for (String file : commonFiles)
                if (hasChanged(file, file))
                    modifiedFiles.add(new Pair<>(toSrcFile(file), toDstFile(file)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Path getSrc() {
        return src;
    }

    public Path getDst() {
        return dst;
    }

    public boolean isDirMode() {
        return dirMode;
    }

    public List<Pair<File, File>> getModifiedFiles() {
        return modifiedFiles;
    }

    public Set<File> getDeletedFiles() {
        return deletedFiles;
    }

    public Set<File> getAddedFiles() {
        return addedFiles;
    }

    private File toSrcFile(String s) {
        return new File(src.toFile(), s);
    }

    private File toDstFile(String s) {
        return new File(dst.toFile(), s);
    }

    public boolean hasChanged(String s1, String s2) throws IOException {
        File f1 = toSrcFile(s1);
        File f2 = toDstFile(s2);
        long l1 = Files.size(f1.toPath());
        long l2 = Files.size(f2.toPath());
        if (l1 != l2)
            return true;
        else {
            try (InputStream dis1 = new BufferedInputStream(new FileInputStream(f1));
                    InputStream dis2 = new BufferedInputStream(new FileInputStream(f2))) {
                int c1, c2;
                while ((c1 = dis1.read()) != -1) {
                    c2 = dis2.read();
                    if (c1 != c2)
                        return true;
                }
                return false;
            }
        }
    }

    public static class AllFilesVisitor extends SimpleFileVisitor<Path> {

        private Set<String> files = new HashSet<>();

        private Path root;

        public AllFilesVisitor(Path root) {
            this.root = root;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (!file.getFileName().startsWith("."))
                files.add(root.relativize(file).toString());
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            return (dir.getFileName().toString().startsWith("."))
                    ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
        }

    }

}
