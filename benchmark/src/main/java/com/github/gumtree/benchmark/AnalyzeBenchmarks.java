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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2026 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtree.benchmark;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates a standalone HTML report from benchmark CSV files.
 */
public final class AnalyzeBenchmarks {
    private static final int RUNTIME_SAMPLES = 5;

    private AnalyzeBenchmarks() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected REPORT_DIRECTORY OUTPUT_HTML");
        }
        Path reportDirectory = Path.of(args[0]);
        Path output = Path.of(args[1]);
        List<Run> runs;
        if (Files.isRegularFile(reportDirectory)) {
            runs = List.of(new Run(reportDirectory.getFileName().toString(), read(reportDirectory)));
        } else {
            try (var files = Files.list(reportDirectory)) {
                runs = files.filter(path -> path.getFileName().toString().endsWith(".csv"))
                        .sorted(Comparator.<Path, String>comparing(path -> path.getFileName().toString()).reversed())
                        .map(path -> new Run(path.getFileName().toString(), read(path)))
                        .collect(Collectors.toList());
            }
        }
        if (runs.isEmpty() || runs.stream().allMatch(run -> run.measurements().isEmpty())) {
            throw new IllegalArgumentException("No benchmark CSV files found in " + reportDirectory);
        }
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write(render(runs));
        }
    }

    static List<Measurement> read(Path file) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            List<Measurement> result = new ArrayList<>();
            boolean hasDataset = !lines.isEmpty() && lines.get(0).startsWith("dataset;");
            for (int i = 1; i < lines.size(); i++) {
                String[] fields = lines.get(i).split(";", -1);
                int offset = hasDataset ? 1 : 0;
                if (fields.length < offset + 12) {
                    continue;
                }
                long[] runtimes = new long[RUNTIME_SAMPLES];
                for (int sample = 0; sample < RUNTIME_SAMPLES; sample++) {
                    runtimes[sample] = Long.parseLong(fields[offset + 2 + sample]);
                }
                result.add(new Measurement(
                        hasDataset ? fields[0] : file.getFileName().toString().replace(".csv", ""),
                        fields[offset], fields[offset + 1], median(runtimes),
                        Long.parseLong(fields[offset + 7]), Long.parseLong(fields[offset + 8]),
                        Long.parseLong(fields[offset + 9]), Long.parseLong(fields[offset + 10]),
                        Long.parseLong(fields[offset + 11])));
            }
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read benchmark report " + file, e);
        }
    }

    static String render(List<Run> runs) {
        StringBuilder html = new StringBuilder("""
                <!doctype html>
                <html><head><meta charset="utf-8">
                <title>GumTree benchmark analysis</title>
                <style>
                body{font:14px system-ui,sans-serif;margin:2em;color:#222}
                table{border-collapse:collapse;margin:1em 0;width:100%}
                th,td{border:1px solid #ccc;padding:.35em;text-align:right}
                th{background:#eee;position:sticky;top:0} td:first-child,th:first-child{text-align:left}
                h1,h2{margin-top:1.5em} .win{background:#d9f2d9} .loss{background:#f8dddd}
                </style></head><body><h1>GumTree benchmark analysis</h1>
                """);
        for (Run run : runs) {
            appendRun(html, run);
        }
        html.append("</body></html>");
        return html.toString();
    }

    private static void appendRun(StringBuilder html, Run run) {
        List<Measurement> measurements = run.measurements();
        if (measurements.isEmpty()) {
            return;
        }
        Map<String, List<Measurement>> byAlgorithm = measurements.stream()
                .collect(Collectors.groupingBy(Measurement::algorithm, LinkedHashMap::new, Collectors.toList()));
        List<String> algorithms = new ArrayList<>(byAlgorithm.keySet());
        algorithms.sort(String::compareTo);
        html.append("<h2>Run: ").append(escape(run.name())).append("</h2><p>")
                .append(measurements.size()).append(" measurements across ")
                .append(algorithms.size()).append(" algorithms.</p>");
        appendGlobalSummary(html, algorithms, byAlgorithm);
        appendPairwiseSummary(html, algorithms, measurements);
        appendCaseDetails(html, measurements);
    }

    private static void appendGlobalSummary(StringBuilder html, List<String> algorithms,
            Map<String, List<Measurement>> byAlgorithm) {
        html.append("<h2>Global indicators</h2><table><tr><th rowspan=\"2\">Algorithm</th>")
                .append("<th rowspan=\"2\">Cases</th><th colspan=\"5\">Runtime (ms)</th>")
                .append("<th colspan=\"5\">Script size</th></tr><tr>");
        appendFiveNumberHeaders(html);
        appendFiveNumberHeaders(html);
        html.append("</tr>");
        for (String algorithm : algorithms) {
            List<Measurement> values = byAlgorithm.get(algorithm);
            double[] runtimes = values.stream().mapToDouble(value -> value.runtimeNanos / 1_000_000.0)
                    .toArray();
            double[] sizes = values.stream().mapToDouble(Measurement::scriptSize).toArray();
            html.append("<tr><td>").append(escape(algorithm)).append("</td><td>").append(values.size())
                    .append("</td>");
            appendFiveNumberSummary(html, runtimes);
            appendFiveNumberSummary(html, sizes);
            html.append("</tr>");
        }
        html.append("</table>");
    }

    private static void appendPairwiseSummary(StringBuilder html, List<String> algorithms,
            List<Measurement> measurements) {
        Map<String, Measurement> cases = measurements.stream().collect(Collectors.toMap(
                value -> value.dataset + "\u0000" + value.caseName + "\u0000" + value.algorithm,
                value -> value, (first, second) -> first, LinkedHashMap::new));
        html.append("<h2>Pairwise comparison</h2><table><tr><th>Algorithm A</th><th>Algorithm B</th>")
                .append("<th>Cases</th><th>Runtime A wins</th><th>Ties</th><th>Runtime B wins</th>")
                .append("<th>Size A wins</th><th>Size ties</th><th>Size B wins</th></tr>");
        for (int i = 0; i < algorithms.size(); i++) {
            for (int j = i + 1; j < algorithms.size(); j++) {
                String first = algorithms.get(i);
                String second = algorithms.get(j);
                List<Measurement[]> pairs = measurements.stream()
                        .filter(value -> value.algorithm.equals(first))
                        .map(value -> new Measurement[] {value,
                                cases.get(value.dataset + "\u0000" + value.caseName + "\u0000" + second)})
                        .filter(pair -> pair[1] != null)
                        .toList();
                long wins = pairs.stream().filter(pair -> pair[0].runtimeNanos < pair[1].runtimeNanos).count();
                long losses = pairs.stream().filter(pair -> pair[0].runtimeNanos > pair[1].runtimeNanos).count();
                long sizeWins = pairs.stream()
                        .filter(pair -> pair[0].scriptSize() < pair[1].scriptSize()).count();
                long sizeLosses = pairs.stream()
                        .filter(pair -> pair[0].scriptSize() > pair[1].scriptSize()).count();
                html.append("<tr><td>").append(escape(first)).append("</td><td>").append(escape(second))
                        .append("</td><td>").append(pairs.size()).append("</td><td>").append(wins)
                        .append("</td><td>").append(pairs.size() - wins - losses).append("</td><td>")
                        .append(losses).append("</td><td>").append(sizeWins).append("</td><td>")
                        .append(pairs.size() - sizeWins - sizeLosses).append("</td><td>")
                        .append(sizeLosses).append("</td></tr>");
            }
        }
        html.append("</table>");
    }

    private static void appendCaseDetails(StringBuilder html, List<Measurement> measurements) {
        html.append("<h2>Per-case details</h2><table><tr><th>Dataset</th><th>Case</th>")
                .append("<th>Algorithm</th><th>Runtime (ms)</th><th>Script size</th>")
                .append("<th>Insert</th><th>Delete</th><th>Update</th><th>Move</th></tr>");
        measurements.stream().sorted(Comparator.comparing(Measurement::dataset)
                .thenComparing(Measurement::caseName).thenComparing(Measurement::algorithm)).forEach(value ->
                html.append("<tr><td>").append(escape(value.dataset)).append("</td><td>")
                        .append(escape(value.caseName)).append("</td><td>").append(escape(value.algorithm))
                        .append("</td><td>").append(format(value.runtimeNanos / 1_000_000.0)).append("</td><td>")
                        .append(value.scriptSize()).append("</td><td>").append(value.insertions).append("</td><td>")
                        .append(value.deletions).append("</td><td>").append(value.updates).append("</td><td>")
                        .append(value.moves).append("</td></tr>"));
        html.append("</table>");
    }

    private static long median(long[] values) {
        long[] copy = values.clone();
        java.util.Arrays.sort(copy);
        return copy[copy.length / 2];
    }

    private static double percentile(double[] values, double percentile) {
        if (values.length == 0) {
            return 0;
        }
        double position = percentile * (values.length - 1);
        int lower = (int) position;
        int upper = Math.min(lower + 1, values.length - 1);
        return values[lower] + (values[upper] - values[lower]) * (position - lower);
    }

    private static void appendFiveNumberSummary(StringBuilder html, double[] values) {
        java.util.Arrays.sort(values);
        for (double percentile : new double[] {0, 0.25, 0.5, 0.75, 1}) {
            html.append("<td>").append(format(percentile(values, percentile))).append("</td>");
        }
    }

    private static void appendFiveNumberHeaders(StringBuilder html) {
        html.append("<th>Min</th><th>Q1</th><th>Median</th><th>Q3</th><th>Max</th>");
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    record Measurement(String dataset, String caseName, String algorithm, long runtimeNanos,
                       long scriptSize, long insertions, long deletions, long updates, long moves) {
    }

    record Run(String name, List<Measurement> measurements) {
    }
}
