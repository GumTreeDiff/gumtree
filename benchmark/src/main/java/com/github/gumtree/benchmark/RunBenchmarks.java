/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * GumTree is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2026 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtree.benchmark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Runs selected datasets into one benchmark report.
 */
public final class RunBenchmarks {

    private RunBenchmarks() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            throw new IllegalArgumentException("Expected OUTPUT_FILE DATASET... --matchers MATCHER...");
        }
        int matcherIndex = Arrays.asList(args).indexOf("--matchers");
        if (matcherIndex < 2) {
            throw new IllegalArgumentException("Expected at least one dataset and --matchers");
        }
        String output = args[0];
        List<String> matchers = List.of(Arrays.copyOfRange(args, matcherIndex + 1, args.length));
        for (int i = 1; i < matcherIndex; i++) {
            System.setProperty("gumtree.benchmark.append", Boolean.toString(i > 1));
            List<String> datasetArgs = new ArrayList<>();
            datasetArgs.add(args[i]);
            datasetArgs.add(output);
            datasetArgs.addAll(matchers);
            RunOnDataset.main(datasetArgs.toArray(String[]::new));
        }
        System.clearProperty("gumtree.benchmark.append");
    }
}
