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
 * Copyright 2026 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.matchers.heuristic.gt;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.SimilarityMetrics;
import com.github.gumtreediff.tree.Tree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Matches bottom-up candidates using PQ-gram context and resolves competing
 * destination candidates globally at each matching round.
 */
public class PqGramBottomUpMatcher extends SimpleBottomUpMatcher {
    private static final int ANCESTOR_CONTEXT = 2;
    private static final int SIBLING_CONTEXT = 3;
    private static final double STRUCTURAL_WEIGHT = 0.75;
    private static final double PQ_GRAM_WEIGHT = 0.25;
    private static final String PADDING = "\u0000";

    private final Map<Tree, Map<String, Integer>> signatures = new IdentityHashMap<>();

    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        signatures.clear();
        while (matchRound(src, mappings)) {
            // Each round makes newly matched descendants available as anchors.
        }
        mappings.addMapping(src, dst);
        lastChanceMatch(mappings, src, dst);
        return mappings;
    }

    private boolean matchRound(Tree src, MappingStore mappings) {
        List<Candidate> candidates = new ArrayList<>();
        for (Tree source : src.postOrder()) {
            if (source.isRoot() || source.isLeaf() || mappings.isSrcMapped(source))
                continue;

            Candidate best = bestCandidate(source, mappings);
            if (best != null)
                candidates.add(best);
        }

        candidates.sort(Comparator.comparingDouble(Candidate::score).reversed()
                .thenComparing(Comparator.comparingDouble(Candidate::structuralSimilarity).reversed()));
        boolean matched = false;
        for (Candidate candidate : candidates) {
            if (!mappings.areBothUnmapped(candidate.source(), candidate.destination()))
                continue;
            lastChanceMatch(mappings, candidate.source(), candidate.destination());
            if (mappings.areBothUnmapped(candidate.source(), candidate.destination())) {
                mappings.addMapping(candidate.source(), candidate.destination());
                matched = true;
            }
        }
        return matched;
    }

    private Candidate bestCandidate(Tree source, MappingStore mappings) {
        Candidate best = null;
        int sourceSize = source.getDescendants().size();
        for (Tree destination : getDstCandidates(mappings, source)) {
            double threshold = Double.isNaN(simThreshold)
                    ? 1D / (1D + Math.log(destination.getDescendants().size() + sourceSize))
                    : simThreshold;
            double structuralSimilarity = SimilarityMetrics.chawatheSimilarity(source, destination, mappings);
            if (structuralSimilarity < threshold)
                continue;

            double score = STRUCTURAL_WEIGHT * structuralSimilarity
                    + PQ_GRAM_WEIGHT * pqGramSimilarity(source, destination);
            Candidate candidate = new Candidate(source, destination, structuralSimilarity, score);
            if (best == null || candidate.score() > best.score()
                    || candidate.score() == best.score()
                    && candidate.structuralSimilarity() > best.structuralSimilarity()) {
                best = candidate;
            }
        }
        return best;
    }

    private double pqGramSimilarity(Tree source, Tree destination) {
        return diceSimilarity(signature(source), signature(destination));
    }

    private double diceSimilarity(Map<String, Integer> sourceSignature, Map<String, Integer> destinationSignature) {
        int common = 0;
        int sourceSize = 0;
        int destinationSize = 0;

        for (Map.Entry<String, Integer> entry : sourceSignature.entrySet()) {
            sourceSize += entry.getValue();
            common += Math.min(entry.getValue(), destinationSignature.getOrDefault(entry.getKey(), 0));
        }
        for (int count : destinationSignature.values())
            destinationSize += count;

        return 2D * common / (sourceSize + destinationSize);
    }

    private Map<String, Integer> signature(Tree tree) {
        return signatures.computeIfAbsent(tree, this::buildSignature);
    }

    private Map<String, Integer> buildSignature(Tree tree) {
        StringBuilder stem = new StringBuilder();
        Tree ancestor = tree;
        for (int i = 0; i < ANCESTOR_CONTEXT; i++) {
            ancestor = ancestor.getParent();
            stem.append(ancestor == null ? PADDING : ancestor.getType().name).append('|');
        }

        List<String> children = new ArrayList<>();
        for (int i = 0; i < SIBLING_CONTEXT - 1; i++)
            children.add(PADDING);
        for (Tree child : tree.getChildren())
            children.add(child.getType().name);
        for (int i = 0; i < SIBLING_CONTEXT - 1; i++)
            children.add(PADDING);

        Map<String, Integer> grams = new HashMap<>();
        for (int i = 0; i <= children.size() - SIBLING_CONTEXT; i++) {
            StringBuilder gram = new StringBuilder(stem);
            for (int j = 0; j < SIBLING_CONTEXT; j++)
                gram.append(children.get(i + j)).append('|');
            grams.merge(gram.toString(), 1, Integer::sum);
        }
        return grams;
    }

    private record Candidate(Tree source, Tree destination, double structuralSimilarity, double score) {
    }
}
