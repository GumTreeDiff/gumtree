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

import com.github.gumtreediff.client.Option;
import com.github.gumtreediff.gen.Registry;
import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.client.Option;
import com.github.gumtreediff.client.Register;
import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;

@Register(name = "axmldiff", description = "Dump annotated xml tree",
        priority = Registry.Priority.LOW, options = AbstractDiffClient.Options.class)
public class AnnotatedXmlDiff extends AbstractDiffClient<AnnotatedXmlDiff.Options> {

    public AnnotatedXmlDiff(String[] args) {
        super(args);
    }

    static class Options extends AbstractDiffClient.Options {
        protected boolean isSrc = true;

        @Override
        public Option[] values() {
            return Option.Context.addValue(super.values(),
                    new Option("--src", String.format("Dump source tree (default: %s)", isSrc ? "yes" : "no"), 0) {
                        @Override
                        protected void process(String name, String[] args) {
                            isSrc = true;
                        }
                    },
                    new Option("--dst", String.format("Dump destination tree (default: %s)", !isSrc
                                    ? "yes" : "no"), 0) {
                        @Override
                        protected void process(String name, String[] args) {
                            isSrc = false;
                        }
                    }
            );
        }
    }

    @Override
    protected Options newOptions() {
        return new Options();
    }

    @Override
    public void run() {
        MappingStore m = matchTrees();
        try {
            TreeIoUtils.toAnnotatedXml((opts.isSrc)
                            ? getSrcTreeContext()
                            : getDstTreeContext(), opts.isSrc, m
            ).writeTo(System.out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
