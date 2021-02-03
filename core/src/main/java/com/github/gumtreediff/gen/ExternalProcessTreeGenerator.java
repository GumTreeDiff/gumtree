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

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public abstract class ExternalProcessTreeGenerator extends TreeGenerator {

    public String readStandardOutput(Reader r) throws IOException {
        // TODO avoid recreating file if supplied reader is already a file
        File f = dumpReaderInTempFile(r);
        ProcessBuilder b = new ProcessBuilder(getCommandLine(f.getAbsolutePath()));
        b.directory(f.getParentFile());
        Process p = b.start();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder buf = new StringBuilder();
            String line = null;
            while ((line = br.readLine()) != null)
                buf.append(line + System.lineSeparator());
            p.waitFor();
            if (p.exitValue() != 0)
                throw new SyntaxException(this, r, null);
            r.close();
            p.destroy();
            return buf.toString();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            f.delete();
        }
    }

    private File dumpReaderInTempFile(Reader r) throws IOException {
        File f = File.createTempFile("gumtree", "");
        try (
                Writer w = Files.newBufferedWriter(f.toPath(), Charset.forName("UTF-8"))
        ) {
            char[] buf = new char[8192];
            while (true) {
                int length = r.read(buf);
                if (length < 0)
                    break;
                w.write(buf, 0, length);
            }
        }
        return f;
    }

    protected abstract String[] getCommandLine(String file);
}
