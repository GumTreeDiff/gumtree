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
 * Copyright 2026 GumTree contributors
 */

package com.github.gumtreediff.matchers.heuristic.gt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.gumtreediff.matchers.ConfigurationOptions;
import com.github.gumtreediff.matchers.GumtreeProperties;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Register;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Registry;
import com.google.common.collect.Sets;

/**
 * Matches exact subtrees first, then uses descendant evidence and sibling order
 * to match changed nodes.
 */
@Register(id = "gumtree-contextual", priority = Registry.Priority.HIGH)
public class ContextualMatcher implements Matcher {
    private double minimumSimilarity = Double.NaN;
    private final SimpleBottomUpMatcher simpleMatcher = new SimpleBottomUpMatcher();

    private Map<Tree, Map<Tree, Integer>> descendantSupport;
    private MappingStore mappings;
    private Set<Tree> aligned;

    @Override
    public void configure(GumtreeProperties properties) {
        minimumSimilarity = properties.tryConfigure(ConfigurationOptions.bu_minsim, minimumSimilarity);
        simpleMatcher.configure(properties);
    }

    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        this.mappings = mappings;
        descendantSupport = new HashMap<>();
        aligned = new HashSet<>();

        new GreedySubtreeMatcher().match(src, dst, mappings);
        simpleMatcher.match(src, dst, mappings);
        List<Mapping> initialMappings = new ArrayList<>();
        for (Mapping mapping : mappings)
            initialMappings.add(mapping);
        for (Mapping mapping : initialMappings)
            addDescendantEvidence(mapping.first, mapping.second);
        for (Mapping mapping : initialMappings)
            alignChildren(mapping.first, mapping.second);

        for (Tree source : src.postOrder()) {
            if (mappings.isSrcMapped(source))
                continue;

            if (source.isRoot()) {
                if (mappings.areBothUnmapped(source, dst)) {
                    addMapping(source, dst);
                    alignChildren(source, dst);
                }
                continue;
            }
            if (source.isLeaf())
                continue;

            Tree best = findBestCandidate(source);
            if (best != null) {
                addMapping(source, best);
                alignChildren(source, best);
            }
        }

        if (!mappings.isSrcMapped(src) && mappings.areBothUnmapped(src, dst)) {
            addMapping(src, dst);
            alignChildren(src, dst);
        }
        return mappings;
    }

    private Tree findBestCandidate(Tree source) {
        Map<Tree, Integer> candidates = descendantSupport.get(source);
        if (candidates == null)
            return null;

        int sourceSize = source.getMetrics().size - 1;
        Tree best = null;
        double bestScore = -1D;
        for (Map.Entry<Tree, Integer> candidate : candidates.entrySet()) {
            Tree destination = candidate.getKey();
            if (destination.isRoot() || !mappings.isMappingAllowed(source, destination))
                continue;

            int destinationSize = destination.getMetrics().size - 1;
            int denominator = Math.max(sourceSize, destinationSize);
            if (denominator == 0)
                continue;
            double score = (double) candidate.getValue() / denominator;
            double threshold = Double.isNaN(minimumSimilarity)
                    ? 1D / (1D + Math.log(sourceSize + destinationSize))
                    : minimumSimilarity;
            if (score < threshold)
                continue;

            if (score > bestScore || (score == bestScore && isBetterTie(source, destination, best))) {
                best = destination;
                bestScore = score;
            }
        }
        return best;
    }

    private boolean isBetterTie(Tree source, Tree destination, Tree currentBest) {
        if (currentBest == null)
            return true;
        boolean sameLabel = source.getLabel().equals(destination.getLabel());
        boolean bestSameLabel = source.getLabel().equals(currentBest.getLabel());
        if (sameLabel != bestSameLabel)
            return sameLabel;

        Tree sourceParent = source.getParent();
        if (sourceParent != null && mappings.isSrcMapped(sourceParent)) {
            Tree mappedParent = mappings.getDstForSrc(sourceParent);
            boolean sameParent = destination.getParent() == mappedParent;
            boolean bestSameParent = currentBest.getParent() == mappedParent;
            if (sameParent != bestSameParent)
                return sameParent;
        }
        return Math.abs(source.positionInParent() - destination.positionInParent())
                < Math.abs(source.positionInParent() - currentBest.positionInParent());
    }

    private void addMapping(Tree source, Tree destination) {
        if (!mappings.isMappingAllowed(source, destination))
            return;
        mappings.addMapping(source, destination);
        addDescendantEvidence(source, destination);
    }

    private void addDescendantEvidence(Tree source, Tree destination) {
        for (Tree sourceAncestor = source.getParent(); sourceAncestor != null;
                sourceAncestor = sourceAncestor.getParent()) {
            if (mappings.isSrcMapped(sourceAncestor))
                continue;
            for (Tree destinationAncestor = destination.getParent(); destinationAncestor != null;
                    destinationAncestor = destinationAncestor.getParent()) {
                if (!mappings.isDstMapped(destinationAncestor)
                        && sourceAncestor.hasSameType(destinationAncestor)) {
                    descendantSupport.computeIfAbsent(sourceAncestor, ignored -> new HashMap<>())
                            .merge(destinationAncestor, 1, Integer::sum);
                }
            }
        }
    }

    private void alignChildren(Tree source, Tree destination) {
        ArrayDeque<Mapping> pending = new ArrayDeque<>();
        pending.add(new Mapping(source, destination));
        while (!pending.isEmpty()) {
            Mapping parentMapping = pending.removeFirst();
            Tree sourceParent = parentMapping.first;
            Tree destinationParent = parentMapping.second;
            if (!aligned.add(sourceParent))
                continue;

            List<Tree> sourceChildren = unmappedSourceChildren(sourceParent);
            List<Tree> destinationChildren = unmappedDestinationChildren(destinationParent);
            for (Tree sourceChild : sourceChildren) {
                Tree best = null;
                for (Tree destinationChild : destinationChildren) {
                    if (mappings.isDstMapped(destinationChild)
                            || !sourceChild.hasSameType(destinationChild))
                        continue;
                    if (sourceChild.isLeaf() != destinationChild.isLeaf())
                        continue;
                    int childSupport = descendantSupport.getOrDefault(sourceChild, Map.of())
                            .getOrDefault(destinationChild, 0);
                    if (!sourceChild.isLeaf() && !destinationChild.isLeaf()
                            && childSupport == 0 && !sourceChild.getLabel().equals(destinationChild.getLabel()))
                        continue;
                    if (best == null || isBetterChildMatch(sourceChild, destinationChild, best))
                        best = destinationChild;
                }
                if (best != null) {
                    Tree destinationChild = best;
                    addMapping(sourceChild, destinationChild);
                    pending.addLast(new Mapping(sourceChild, destinationChild));
                }
            }
        }
    }

    private boolean isBetterChildMatch(Tree source, Tree candidate, Tree currentBest) {
        boolean sameSubtree = source.getMetrics().hash == candidate.getMetrics().hash
                && source.isIsomorphicTo(candidate);
        boolean bestSameSubtree = source.getMetrics().hash == currentBest.getMetrics().hash
                && source.isIsomorphicTo(currentBest);
        if (sameSubtree != bestSameSubtree)
            return sameSubtree;

        boolean sameLabel = source.getLabel().equals(candidate.getLabel());
        boolean bestSameLabel = source.getLabel().equals(currentBest.getLabel());
        if (sameLabel != bestSameLabel)
            return sameLabel;

        Map<Tree, Integer> support = descendantSupport.get(source);
        int candidateSupport = support == null ? 0 : support.getOrDefault(candidate, 0);
        int bestSupport = support == null ? 0 : support.getOrDefault(currentBest, 0);
        if (candidateSupport != bestSupport)
            return candidateSupport > bestSupport;
        return Math.abs(source.positionInParent() - candidate.positionInParent())
                < Math.abs(source.positionInParent() - currentBest.positionInParent());
    }

    private List<Tree> unmappedSourceChildren(Tree parent) {
        List<Tree> children = new ArrayList<>();
        for (Tree child : parent.getChildren()) {
            if (!mappings.isSrcMapped(child))
                children.add(child);
        }
        return children;
    }

    private List<Tree> unmappedDestinationChildren(Tree parent) {
        List<Tree> children = new ArrayList<>();
        for (Tree child : parent.getChildren()) {
            if (!mappings.isDstMapped(child))
                children.add(child);
        }
        return children;
    }

    @Override
    public Set<ConfigurationOptions> getApplicableOptions() {
        return Sets.newHashSet(ConfigurationOptions.bu_minsim);
    }
}
