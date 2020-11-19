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

package com.github.gumtreediff.client;

import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.gen.TreeGenerators;
import com.github.gumtreediff.tree.TreeContext;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Arrays;

@Register(name = "parse", description = "Parse file and dump result.")
public class Serializer extends Client {

    public static final String SYNTAX = "Syntax: parse [options] file ...";

    static class Options implements Option.Context {
        protected OutputFormat format = OutputFormat.TEXT;
        protected String generator = null;
        protected String output = null;
        protected String[] files;

        @Override
        public Option[] values() {
            return new Option[]{
                    new Option("-f", "Output format " + Arrays.toString(OutputFormat.values()), 1) {
                        @Override
                        protected void process(String name, String[] args) {
                            OutputFormat o = OutputFormat.valueOf(args[0].toUpperCase());
                            if (o != null)
                                format = o;
                            else
                                System.err.println("Invalid output type: " + args[0]);
                        }
                    },
                    new Option("-o", "Output filename (or directory if more than one file), defaults to stdout", 1) {
                        @Override
                        protected void process(String name, String[] args) {
                            output = args[0];
                        }
                    },
                    new Option("-g", "Preferred generator to use.", 1) {
                        @Override
                        protected void process(String name, String[] args) {
                            generator = args[0];
                        }
                    }
            };
        }
    }

    enum OutputFormat {
        JSON {
            @Override
            TreeIoUtils.TreeSerializer getSerializer(TreeContext ctx) {
                return TreeIoUtils.toJson(ctx);
            }
        },
        XML {
            @Override
            TreeIoUtils.TreeSerializer getSerializer(TreeContext ctx) {
                return TreeIoUtils.toCompactXml(ctx);
            }
        },
        FULLXML {
            @Override
            TreeIoUtils.TreeSerializer getSerializer(TreeContext ctx) {
                return TreeIoUtils.toXml(ctx);
            }
        },
        DOT {
            @Override
            TreeIoUtils.TreeSerializer getSerializer(TreeContext ctx) {
                return TreeIoUtils.toDot(ctx);
            }
        },
        LISP {
            @Override
            TreeIoUtils.TreeSerializer getSerializer(TreeContext ctx) {
                return TreeIoUtils.toLisp(ctx);
            }
        },
        TEXT  {
            @Override
            TreeIoUtils.TreeSerializer getSerializer(TreeContext ctx) {
                return TreeIoUtils.toText(ctx);
            }
        };

        abstract TreeIoUtils.TreeSerializer getSerializer(TreeContext ctx);
    }

    Options opts = new Options();

    public Serializer(String[] args) {
        super(args);
        args = Option.processCommandLine(args, opts);
        if (args.length == 0)
            throw new Option.OptionException(SYNTAX);

        opts.files = args;
    }

    @Override
    public void run() throws Exception {
        final boolean multiple = opts.files.length > 1;
        if (multiple && opts.output != null)
            Files.createDirectories(FileSystems.getDefault().getPath(opts.output));

        for (String file : opts.files) {
            TreeContext tc = getTreeContext(file);
            opts.format.getSerializer(tc).writeTo(opts.output == null
                    ? System.out
                    : new FileOutputStream(opts.output));
        }
    }

    private TreeContext getTreeContext(String file) throws IOException {
        return TreeGenerators.getInstance().getTree(file, opts.generator);
    }
}