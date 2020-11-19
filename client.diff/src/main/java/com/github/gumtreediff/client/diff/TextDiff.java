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

package com.github.gumtreediff.client.diff;

import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.client.Option;
import com.github.gumtreediff.client.Register;
import com.github.gumtreediff.io.ActionsIoUtils;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.TreeContext;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

@Register(name = "textdiff", description = "Dump actions in a textual format.",
        options = TextDiff.TextDiffOptions.class)
public class TextDiff extends AbstractDiffClient<TextDiff.TextDiffOptions> {
    public TextDiff(String[] args) {
        super(args);
        if (!Files.isRegularFile(Paths.get(opts.srcPath)))
            throw new Option.OptionException("Source must be a file: " + opts.srcPath, opts);
        if (!Files.isRegularFile(Paths.get(opts.dstPath)))
            throw new Option.OptionException("Destination must be a file: " + opts.dstPath, opts);

        if (opts.format == null) {
            opts.format = OutputFormat.TEXT;
            if (opts.output != null) {
                if (opts.output.endsWith(".json"))
                    opts.format = OutputFormat.JSON;
                else if (opts.output.endsWith(".xml"))
                    opts.format = OutputFormat.XML;
            }
        }
    }

    public static class TextDiffOptions extends AbstractDiffClient.DiffOptions {
        protected OutputFormat format;
        protected String output;

        @Override
        public Option[] values() {
            return Option.Context.addValue(super.values(),
                    new Option("-f", String.format("format: %s", Arrays.toString(OutputFormat.values())), 1) {
                        @Override
                        protected void process(String name, String[] args) {
                            try {
                                format = OutputFormat.valueOf(args[0].toUpperCase());
                            } catch (IllegalArgumentException e) {
                                throw new Option.OptionException(String.format(
                                        "No such format '%s', available formats are: %s\n",
                                        args[0].toUpperCase(), Arrays.toString(OutputFormat.values())), e);
                            }
                        }
                    },
                    new Option("-o", "output file", 1) {
                        @Override
                        protected void process(String name, String[] args) {
                            output = args[0];
                        }
                    }
            );
        }

        @Override
        void dump(PrintStream out) {
            super.dump(out);
            out.printf("format: %s\n", format);
            out.printf("output file: %s\n", output == null ? "<stdout>" : output);
        }
    }

    @Override
    protected TextDiffOptions newOptions() {
        return new TextDiffOptions();
    }

    @Override
    public void run() throws Exception {
        Diff diff = getDiff();
        ActionsIoUtils.ActionSerializer serializer = opts.format.getSerializer(
                diff.src, diff.editScript, diff.mappings);
        if (opts.output == null)
            serializer.writeTo(System.out);
        else
            serializer.writeTo(opts.output);
    }

    enum OutputFormat {
        TEXT {
            @Override
            ActionsIoUtils.ActionSerializer getSerializer(TreeContext sctx, EditScript actions, MappingStore mappings)
                    throws IOException {
                return ActionsIoUtils.toText(sctx, actions, mappings);
            }
        },
        XML {
            @Override
            ActionsIoUtils.ActionSerializer getSerializer(TreeContext sctx, EditScript actions, MappingStore mappings)
                    throws IOException {
                return ActionsIoUtils.toXml(sctx, actions, mappings);
            }
        },
        JSON {
            @Override
            ActionsIoUtils.ActionSerializer getSerializer(TreeContext sctx, EditScript actions, MappingStore mappings)
                    throws IOException {
                return ActionsIoUtils.toJson(sctx, actions, mappings);
            }
        };

        abstract ActionsIoUtils.ActionSerializer getSerializer(TreeContext sctx, EditScript actions,
                                                               MappingStore mappings) throws IOException;
    }
}
