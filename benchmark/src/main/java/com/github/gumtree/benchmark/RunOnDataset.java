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

package com.github.gumtree.benchmark;

import com.github.gumtreediff.actions.*;
import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.gen.TreeGenerators;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.gen.treesitterng.PythonTreeSitterNgTreeGenerator;
import com.github.gumtreediff.io.DirectoryComparator;
import com.github.gumtreediff.matchers.*;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import org.atteo.classindex.ClassIndex;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Supplier;

public class RunOnDataset {
    private static final int TIME_MEASURES = 5;
    private static String ROOT_FOLDER;
    private static FileWriter OUTPUT;
    private static final List<MatcherConfig> configurations = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        configurations.clear();
        initMatchers();

        if (args.length > 0 && (args[0].equals("--list-matchers") || args[0].equals("-l")
                || args[0].equals("--help") || args[0].equals("-h"))) {
            printAvailableMatchers();
            return;
        }

        if (args.length < 2) {
            System.err.println("Wrong command. Expected arguments: INPUT_FOLDER OUTPUT_FILE [MATCHERS...]. Got: "
                    + Arrays.toString(args));
            printAvailableMatchers();
            System.exit(1);
        }
        ROOT_FOLDER = new File(args[0]).getAbsolutePath();
        TreeGenerators.getInstance().install(
                JdtTreeGenerator.class, JdtTreeGenerator.class.getAnnotation(Register.class));

        TreeGenerators.getInstance().install(
                PythonTreeSitterNgTreeGenerator.class,
                PythonTreeSitterNgTreeGenerator.class.getAnnotation(Register.class)
        );

        File outputFile = new File(args[1]);
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }
        boolean append = Boolean.getBoolean("gumtree.benchmark.append");
        OUTPUT = new FileWriter(outputFile, append);

        if (!append) {
            StringBuilder header = new StringBuilder("dataset;case;algorithm;");
            for (int i = 1; i <= TIME_MEASURES; i++) {
                header.append("t").append(i).append(";");
            }
            header.append("s;ni;nd;nu;nm");
            OUTPUT.append(header + "\n");
        }

        for (int i = 2; i < args.length; i++) {
            String arg = args[i];
            if (arg.contains(",")) {
                for (String part : arg.split(",")) {
                    if (!part.trim().isEmpty()) {
                        configurations.add(resolveMatcher(part));
                    }
                }
            } else if (!arg.trim().isEmpty()) {
                configurations.add(resolveMatcher(arg));
            }
        }

        if (configurations.isEmpty()) {
            configurations.add(resolveMatcher("simple"));
        }

        int limit = Integer.getInteger("gumtree.benchmark.limit", -1);
        DirectoryComparator comparator = new DirectoryComparator(args[0] + "/before", args[0] + "/after");
        comparator.compare();
        int done = 0;
        int size = comparator.getModifiedFiles().size();
        int totalToRun = (limit > 0 && limit < size) ? limit : size;
        for (Pair<File, File> pair : comparator.getModifiedFiles()) {
            if (limit > 0 && done >= limit) {
                break;
            }
            done++;
            int pct = (int) (((float) done / (float) totalToRun) * 100);
            System.out.printf("\r%s %s%%  Done (%d/%d)", displayBar(pct), pct, done, totalToRun);
            try {
                handleCase(pair.first, pair.second);
            } catch (SyntaxException e) {
                System.out.println("\nProblem parsing " + pair.first.getPath());
            }
        }
        System.out.println();
        OUTPUT.close();
    }

    private static void initMatchers() {
        ClassIndex.getSubclasses(Matcher.class).forEach(gen -> {
            com.github.gumtreediff.matchers.Register a =
                    gen.getAnnotation(com.github.gumtreediff.matchers.Register.class);
            if (a != null) {
                Matchers.getInstance().install(gen, a);
            }
        });
    }

    private static Map<String, Supplier<MatcherConfig>> getPresets() {
        Map<String, Supplier<MatcherConfig>> presets = new LinkedHashMap<>();
        presets.put("simple", () -> new MatcherConfig("simple",
                CompositeMatchers.SimpleGumtree::new, mediumMinSim()));
        presets.put("auto", () -> new MatcherConfig("auto",
                AutoMatchers.SimpleGumtreeAutoMt::new, new GumtreeProperties()));
        presets.put("auto-st", () -> new MatcherConfig("auto-st",
                AutoMatchers.SimpleGumtreeAuto::new, new GumtreeProperties()));
        presets.put("hybrid", () -> new MatcherConfig("hybrid",
                CompositeMatchers.HybridGumtree::new, mediumBuMinsize()));
        presets.put("hybrid-100", () -> new MatcherConfig("hybrid-100",
                CompositeMatchers.HybridGumtree::new, mediumBuMinsize()));
        presets.put("classic", () -> new MatcherConfig("classic",
                CompositeMatchers.ClassicGumtree::new, mediumBuMinsize()));
        presets.put("opt-100", () -> new MatcherConfig("opt-100",
                CompositeMatchers.ClassicGumtree::new, mediumBuMinsize()));
        presets.put("opt-1000", () -> new MatcherConfig("opt-1000",
                CompositeMatchers.ClassicGumtree::new, largeBuMinsize()));
        presets.put("stable", () -> new MatcherConfig("stable",
                CompositeMatchers.SimpleGumtreeStable::new, mediumMinSim()));
        presets.put("simple-id", () -> new MatcherConfig("simple-id",
                CompositeMatchers.SimpleIdGumtree::new, mediumMinSim()));
        presets.put("hybrid-id", () -> new MatcherConfig("hybrid-id",
                CompositeMatchers.HybridIdGumtree::new, mediumBuMinsize()));
        presets.put("cd", () -> new MatcherConfig("change-distiller",
                CompositeMatchers.ChangeDistiller::new));
        presets.put("change-distiller", () -> new MatcherConfig("change-distiller",
                CompositeMatchers.ChangeDistiller::new));
        presets.put("xy", () -> new MatcherConfig("xy",
                CompositeMatchers.XyMatcher::new));
        return presets;
    }

    private static MatcherConfig resolveMatcher(String name) {
        String trimmed = name.trim();
        String key = trimmed.toLowerCase();
        Map<String, Supplier<MatcherConfig>> presets = getPresets();
        if (presets.containsKey(key)) {
            return presets.get(key).get();
        }

        Matcher m = Matchers.getInstance().getMatcher(trimmed);
        if (m != null) {
            return new MatcherConfig(trimmed, () -> Matchers.getInstance().getMatcher(trimmed));
        }
        m = Matchers.getInstance().getMatcher(key);
        if (m != null) {
            return new MatcherConfig(key, () -> Matchers.getInstance().getMatcher(key));
        }

        Class<? extends Matcher> matcherClass = tryLoadClass(trimmed);
        if (matcherClass != null) {
            return new MatcherConfig(matcherClass.getSimpleName(), () -> {
                try {
                    Constructor<? extends Matcher> ctor = matcherClass.getConstructor();
                    return ctor.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        throw new IllegalArgumentException("Unknown matcher: '" + name + "'. Available presets/matchers: "
                + String.join(", ", getAvailableMatcherNames()));
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Matcher> tryLoadClass(String className) {
        String[] candidates = new String[] {
            className,
            "com.github.gumtreediff.matchers." + className,
            "com.github.gumtreediff.matchers.CompositeMatchers$" + className,
            "com.github.gumtreediff.matchers.AutoMatchers$" + className
        };
        for (String candidate : candidates) {
            try {
                Class<?> clazz = Class.forName(candidate);
                if (Matcher.class.isAssignableFrom(clazz)) {
                    return (Class<? extends Matcher>) clazz;
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    private static List<String> getAvailableMatcherNames() {
        Set<String> names = new LinkedHashSet<>(getPresets().keySet());
        for (var entry : Matchers.getInstance().getEntries()) {
            names.add(entry.id);
        }
        return new ArrayList<>(names);
    }

    public static void printAvailableMatchers() {
        System.out.println("Available matcher presets:");
        for (String preset : getPresets().keySet()) {
            System.out.println("  - " + preset);
        }
        System.out.println("\nAvailable registered matcher IDs:");
        for (var entry : Matchers.getInstance().getEntries()) {
            System.out.println("  - " + entry.id);
        }
    }

    private static void handleCase(File src, File dst) throws IOException {
        TreeContext srcT = TreeGenerators.getInstance().getTree(src.getAbsolutePath());
        TreeContext dstT = TreeGenerators.getInstance().getTree(dst.getAbsolutePath());
        for (MatcherConfig config : configurations) {
            Matcher m = config.instantiate();
            handleMatcher(src.getAbsolutePath().substring(ROOT_FOLDER.length() + 1),
                    config.name, m, srcT, dstT);
        }
    }

    private static void handleMatcher(String file, String matcher, Matcher m,
            TreeContext src, TreeContext dst) throws IOException {
        long[] times = new long[TIME_MEASURES];
        MappingStore mappings = null;
        for (int i = 0; i < TIME_MEASURES; i++) {
            long startedTime = System.nanoTime();
            mappings = m.match(src.getRoot(), dst.getRoot());
            long elapsedTime = System.nanoTime() - startedTime;
            times[i] = elapsedTime;
        }
        Arrays.sort(times);
        EditScriptGenerator g = new SimplifiedChawatheScriptGenerator();
        EditScript s = g.computeActions(mappings);

        int nbIns = 0;
        int nbDel = 0;
        int nbMov = 0;
        int nbUpd = 0;
        for (Action a : s) {
            if (a instanceof Insert)
                nbIns++;
            else if (a instanceof Delete)
                nbDel++;
            else if (a instanceof Update)
                nbUpd++;
            else if (a instanceof Move)
                nbMov += a.getNode().getMetrics().size;
            else if (a instanceof TreeInsert)
                nbIns += a.getNode().getMetrics().size;
            else if (a instanceof TreeDelete)
                nbDel += a.getNode().getMetrics().size;
        }

        OUTPUT.append(new File(ROOT_FOLDER).getName() + ";");
        OUTPUT.append(file + ";");
        OUTPUT.append(matcher + ";");
        for (int i = 0; i < TIME_MEASURES; i++)
            OUTPUT.append(times[i] + ";");
        int size = s.size();
        OUTPUT.append(size + ";");
        OUTPUT.append(nbIns + ";");
        OUTPUT.append(nbDel + ";");
        OUTPUT.append(nbUpd + ";");
        OUTPUT.append(nbMov + "\n");
    }

    private static class MatcherConfig {
        public final String name;
        private final Supplier<Matcher> matcherFactory;
        private final GumtreeProperties props;

        public MatcherConfig(String name, Supplier<Matcher> matcherFactory, GumtreeProperties props) {
            this.name = name;
            this.matcherFactory = matcherFactory;
            this.props = props;
        }

        public MatcherConfig(String name, Supplier<Matcher> matcherFactory) {
            this.name = name;
            this.matcherFactory = matcherFactory;
            this.props = new GumtreeProperties();
        }

        public Matcher instantiate() {
            Matcher m = matcherFactory.get();
            m.configure(props);
            return m;
        }
    }

    private static GumtreeProperties mediumMinSim() {
        GumtreeProperties props = new GumtreeProperties();
        props.put(ConfigurationOptions.bu_minsim, 0.5);
        return props;
    }

    private static GumtreeProperties mediumBuMinsize() {
        GumtreeProperties props = new GumtreeProperties();
        props.put(ConfigurationOptions.bu_minsize, 100);
        props.put(ConfigurationOptions.bu_minsim, 0.5);
        return props;
    }

    private static GumtreeProperties largeBuMinsize() {
        GumtreeProperties props = new GumtreeProperties();
        props.put(ConfigurationOptions.bu_minsize, 1000);
        props.put(ConfigurationOptions.bu_minsim, 0.5);
        return props;
    }

    private static String displayBar(int i) {
        StringBuilder sb = new StringBuilder();

        int x = i / 2;
        sb.append("|");
        for (int k = 0; k < 50; k++)
            sb.append(String.format("%s", ((x <= k) ? " " : "=")));
        sb.append("|");

        return sb.toString();
    }
}
