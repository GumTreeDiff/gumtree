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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.TreeDelete;
import com.github.gumtreediff.actions.model.TreeInsert;
import com.github.gumtreediff.matchers.GumtreeProperties;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.HungarianAlgorithm;

/**
 * Uses maximum-weight bipartite assignment to align unmatched siblings.
 *
 * <p>The matcher evaluates that globally aligned mapping against the Simple
 * matcher and retains the mapping with the smaller simplified edit script.
 * This keeps the additional, cubic sibling alignment conservative.</p>
 */
public class AssignmentMatcher extends SimpleBottomUpMatcher {
    private static final int MAX_ASSIGNMENT_WIDTH = 64;
    private static final double UNMATCHED_COST = 0.5D;
    private static final double MINIMUM_SCORE = 0.5D;
    private GumtreeProperties properties = new GumtreeProperties();

    @Override
    public void configure(GumtreeProperties properties) {
        super.configure(properties);
        this.properties = properties;
    }

    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        MappingStore simpleMappings = new MappingStore(mappings);
        greedySubtreeMatch(src, dst, simpleMappings);
        SimpleBottomUpMatcher simpleMatcher = new SimpleBottomUpMatcher();
        simpleMatcher.configure(properties);
        simpleMatcher.match(src, dst, simpleMappings);

        MappingStore assignmentMappings = new MappingStore(mappings);
        greedySubtreeMatch(src, dst, assignmentMappings);
        if (assignmentMappings.areBothUnmapped(src, dst))
            assignmentMappings.addMapping(src, dst);
        alignMappedParents(assignmentMappings);

        MappingStore selected = actionNodeCount(assignmentMappings) < actionNodeCount(simpleMappings)
                ? assignmentMappings : simpleMappings;
        for (Mapping mapping : selected) {
            if (mappings.areBothUnmapped(mapping.first, mapping.second))
                mappings.addMapping(mapping.first, mapping.second);
        }
        return mappings;
    }

    private void greedySubtreeMatch(Tree src, Tree dst, MappingStore mappings) {
        GreedySubtreeMatcher matcher = new GreedySubtreeMatcher();
        matcher.configure(properties);
        matcher.match(src, dst, mappings);
    }

    private void alignMappedParents(MappingStore mappings) {
        ArrayDeque<Mapping> pending = new ArrayDeque<>();
        Set<Tree> visited = new HashSet<>();
        for (Mapping mapping : mappings)
            pending.addLast(mapping);

        while (!pending.isEmpty()) {
            Mapping parent = pending.removeFirst();
            if (!visited.add(parent.first))
                continue;

            lastChanceMatch(mappings, parent.first, parent.second);
            for (Tree sourceChild : parent.first.getChildren()) {
                Tree destinationChild = mappings.getDstForSrc(sourceChild);
                if (destinationChild != null)
                    pending.addLast(new Mapping(sourceChild, destinationChild));
            }

            List<Tree> sourceChildren = unmappedChildren(parent.first, mappings, true);
            List<Tree> destinationChildren = unmappedChildren(parent.second, mappings, false);
            if (sourceChildren.isEmpty() || destinationChildren.isEmpty()
                    || sourceChildren.size() > MAX_ASSIGNMENT_WIDTH
                    || destinationChildren.size() > MAX_ASSIGNMENT_WIDTH)
                continue;

            int[] assignments = new HungarianAlgorithm(costs(sourceChildren, destinationChildren)).execute();
            for (int sourceIndex = 0; sourceIndex < sourceChildren.size(); sourceIndex++) {
                int destinationIndex = assignments[sourceIndex];
                if (destinationIndex < 0 || destinationIndex >= destinationChildren.size())
                    continue;
                Tree source = sourceChildren.get(sourceIndex);
                Tree destination = destinationChildren.get(destinationIndex);
                if (mappings.isMappingAllowed(source, destination)
                        && similarity(source, destination) > MINIMUM_SCORE) {
                    mappings.addMapping(source, destination);
                    pending.addLast(new Mapping(source, destination));
                }
            }
        }
    }

    private List<Tree> unmappedChildren(Tree parent, MappingStore mappings, boolean source) {
        List<Tree> children = new ArrayList<>();
        for (Tree child : parent.getChildren()) {
            if (source ? !mappings.isSrcMapped(child) : !mappings.isDstMapped(child))
                children.add(child);
        }
        return children;
    }

    private double[][] costs(List<Tree> sources, List<Tree> destinations) {
        int sourceCount = sources.size();
        int destinationCount = destinations.size();
        double[][] costs = new double[sourceCount + destinationCount][sourceCount + destinationCount];
        for (int source = 0; source < sourceCount; source++) {
            for (int destination = 0; destination < destinationCount; destination++)
                costs[source][destination] = 1D - similarity(sources.get(source), destinations.get(destination));
            for (int dummy = destinationCount; dummy < costs.length; dummy++)
                costs[source][dummy] = UNMATCHED_COST;
        }
        for (int dummy = sourceCount; dummy < costs.length; dummy++) {
            for (int destination = 0; destination < destinationCount; destination++)
                costs[dummy][destination] = UNMATCHED_COST;
        }
        return costs;
    }

    private double similarity(Tree source, Tree destination) {
        if (!source.hasSameType(destination))
            return 0D;
        if (source.isLeaf() && destination.isLeaf())
            return source.getLabel().equals(destination.getLabel()) ? 1D : 0.55D;

        double labels = source.getLabel().equals(destination.getLabel()) ? 1D : 0D;
        double children = childTypeDice(source, destination);
        double sizes = 1D - (double) Math.abs(source.getMetrics().size - destination.getMetrics().size)
                / Math.max(source.getMetrics().size, destination.getMetrics().size);
        return 0.2D * labels + 0.6D * children + 0.2D * sizes;
    }

    private double childTypeDice(Tree source, Tree destination) {
        int common = 0;
        boolean[] used = new boolean[destination.getChildren().size()];
        for (Tree sourceChild : source.getChildren()) {
            for (int i = 0; i < destination.getChildren().size(); i++) {
                if (!used[i] && sourceChild.hasSameType(destination.getChild(i))) {
                    used[i] = true;
                    common++;
                    break;
                }
            }
        }
        return 2D * common / (source.getChildren().size() + destination.getChildren().size());
    }

    private int actionNodeCount(MappingStore mappings) {
        int count = 0;
        for (Action action : new SimplifiedChawatheScriptGenerator().computeActions(mappings)) {
            if (action instanceof Move || action instanceof TreeInsert || action instanceof TreeDelete)
                count += action.getNode().getMetrics().size;
            else
                count++;
        }
        return count;
    }
}
