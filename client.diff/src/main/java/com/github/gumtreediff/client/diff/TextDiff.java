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

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.client.Option;
import com.github.gumtreediff.client.Register;
import com.github.gumtreediff.io.ActionsIoUtils;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.TreeContext;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

@Register(name = "diff", description = "Dump actions in our textual format",
        options = AbstractDiffClient.Options.class)
public class TextDiff extends AbstractDiffClient<TextDiff.Options> {

    public TextDiff(String[] args) {
        super(args);

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

    public static class Options extends AbstractDiffClient.Options {
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
                                System.err.printf("No such format '%s', available formats are: %s\n",
                                        args[0].toUpperCase(), Arrays.toString(OutputFormat.values()));
                                System.exit(-1);
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
            out.printf("output file: %s\n", output == null ? "<Stdout>" : output);
        }
    }

    @Override
    protected Options newOptions() {
        return new Options();
    }

    @Override
    public void run() {
        Matcher m = matchTrees();
        ActionGenerator g = new ActionGenerator(getSrcTreeContext().getRoot(),
                getDstTreeContext().getRoot(), m.getMappings());
        g.generate();
        List<Action> actions = g.getActions();
        try {
            ActionsIoUtils.ActionSerializer serializer = opts.format.getSerializer(
                    getSrcTreeContext(), actions, m.getMappings());
            if (opts.output == null)
                serializer.writeTo(System.out);
            else
                serializer.writeTo(opts.output);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    enum OutputFormat { // TODO make a registry for that also ?
        TEXT {
            @Override
            ActionsIoUtils.ActionSerializer getSerializer(TreeContext sctx, List<Action> actions, MappingStore mappings)
                    throws IOException {
                return ActionsIoUtils.toText(sctx, actions, mappings);
            }
        },
        XML {
            @Override
            ActionsIoUtils.ActionSerializer getSerializer(TreeContext sctx, List<Action> actions, MappingStore mappings)
                    throws IOException {
                return ActionsIoUtils.toXml(sctx, actions, mappings);
            }
        },
        JSON {
            @Override
            ActionsIoUtils.ActionSerializer getSerializer(TreeContext sctx, List<Action> actions, MappingStore mappings)
                    throws IOException {
                return ActionsIoUtils.toJson(sctx, actions, mappings);
            }
        };

        abstract ActionsIoUtils.ActionSerializer getSerializer(TreeContext sctx, List<Action> actions,
                                                               MappingStore mappings) throws IOException;
    }
}
