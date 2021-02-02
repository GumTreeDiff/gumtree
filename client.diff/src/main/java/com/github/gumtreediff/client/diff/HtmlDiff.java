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
 * Copyright 2017 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.client.diff;

import com.github.gumtreediff.io.DirectoryComparator;
import com.github.gumtreediff.client.Register;
import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.utils.Pair;
import com.github.gumtreediff.client.Option;
import com.github.gumtreediff.client.diff.webdiff.VanillaDiffView;

import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

@Register(name = "htmldiff", description = "Dump diff as HTML in stdout",
        options = HtmlDiff.HtmlDiffOptions.class)
public class HtmlDiff extends AbstractDiffClient<HtmlDiff.HtmlDiffOptions> {

    public HtmlDiff(String[] args) {
        super(args);
    }

    public static class HtmlDiffOptions extends AbstractDiffClient.DiffOptions {
        protected String output;

        @Override
        public Option[] values() {
            return Option.Context.addValue(super.values(),
                    new Option("-o", "output file", 1) {
                        @Override
                        protected void process(String name, String[] args) {
                            output = args[0];
                        }
                    }
            );
        }
    }

    @Override
    protected HtmlDiffOptions newOptions() {
        return new HtmlDiffOptions();
    }

    @Override
    public void run() throws IOException {
        DirectoryComparator comparator = new DirectoryComparator(opts.srcPath, opts.dstPath);
        Pair<File, File> pair = comparator.getModifiedFiles().get(0);
        Diff diff = getDiff(pair.first.getAbsolutePath(), pair.second.getAbsolutePath());
        Renderable view = new VanillaDiffView(pair.first, pair.second, diff, true);
        HtmlCanvas c = new HtmlCanvas();
        view.renderOn(c);
        if (opts.output == null) {
            System.out.println(c.toHtml());
        }
        else {
            File htmlOutput = new File(opts.output);
            FileWriter writer = new FileWriter(htmlOutput);
            writer.write(c.toHtml());
            writer.close();
        }
    }
}
