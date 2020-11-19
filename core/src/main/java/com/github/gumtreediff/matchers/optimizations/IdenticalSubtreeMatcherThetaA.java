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
 * Copyright 2015-2016 Georg Dotzler <georg.dotzler@fau.de>
 * Copyright 2015-2016 Marius Kamp <marius.kamp@fau.de>
 */
package com.github.gumtreediff.matchers.optimizations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.Tree;

/**
 * This implements the identical subtree optimization Theta A.
 */

public class IdenticalSubtreeMatcherThetaA implements Matcher {

    private Tree src;
    private Tree dst;
    private MappingStore mappings;

    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        this.src = src;
        this.dst = dst;
        this.mappings = mappings;
        newUnchangedMatching();
        return mappings;
    }

    @SuppressWarnings({"checkstyle:AvoidEscapedUnicodeCharacters"})
    private String getHash(Tree node, HashMap<Tree, Integer> quickFind, HashMap<Tree, String> stringMap) {
        String tmp = node.getType() + node.getLabel();
        for (Tree child : node.getChildren()) {
            tmp += getHash(child, quickFind, stringMap);
        }
        tmp += "\\u2620";
        quickFind.put(node, tmp.hashCode());
        stringMap.put(node, tmp);
        return tmp;
    }

    private List<Tree> getNodeStream(Tree root) {
        LinkedList<Tree> nodes = new LinkedList<>();
        LinkedList<Tree> workList = new LinkedList<>();
        workList.add(root);
        while (!workList.isEmpty()) {
            Tree node = workList.removeFirst();
            nodes.add(node);
            for (int i = node.getChildren().size() - 1; i >= 0; i--) {
                workList.addFirst(node.getChildren().get(i));
            }
        }
        return nodes;
    }

    private void newUnchangedMatching() {
        HashMap<Tree, Integer> quickFind = new HashMap<>();
        HashMap<Tree, String> stringMap = new HashMap<>();
        getHash(src, quickFind, stringMap);
        getHash(dst, quickFind, stringMap);
        HashMap<String, LinkedList<Tree>> nodeMapOld = new HashMap<>();
        List<Tree> streamOld = getNodeStream(src);
        List<Tree> streamNew = getNodeStream(dst);
        for (Tree node : streamOld) {
            String hashString = stringMap.get(node);
            LinkedList<Tree> nodeList = nodeMapOld.get(hashString);
            if (nodeList == null) {
                nodeList = new LinkedList<>();
                nodeMapOld.put(hashString, nodeList);
            }
            nodeList.add(node);
        }
        HashMap<String, LinkedList<Tree>> nodeMapNew = new HashMap<>();

        for (Tree node : streamNew) {
            String hashString = stringMap.get(node);
            LinkedList<Tree> nodeList = nodeMapNew.get(hashString);
            if (nodeList == null) {
                nodeList = new LinkedList<>();
                nodeMapNew.put(hashString, nodeList);
            }
            nodeList.add(node);
        }

        HashSet<Mapping> pairs = new HashSet<>();
        LinkedList<Tree> workList = new LinkedList<>();
        workList.add(src);

        while (!workList.isEmpty()) {
            Tree node = workList.removeFirst();
            LinkedList<Tree> oldList = nodeMapOld.get(stringMap.get(node));
            assert (oldList != null);
            LinkedList<Tree> newList = nodeMapNew.get(stringMap.get(node));
            if (oldList.size() == 1 && newList != null && newList.size() == 1) {
                if (node.getChildren().size() > 0) {
                    assert (stringMap.get(node).equals(stringMap.get(newList.getFirst())));
                    pairs.add(new Mapping(node, newList.getFirst()));
                    oldList.remove(node);
                    newList.removeFirst();

                }
            } else {
                workList.addAll(node.getChildren());
            }
        }
        for (Mapping mapping : pairs) {
            List<Tree> stream1 = getNodeStream(mapping.first);
            List<Tree> stream2 = getNodeStream(mapping.second);
            stream1 = new ArrayList<>(stream1);
            stream2 = new ArrayList<>(stream2);
            assert (stream1.size() == stream2.size());
            for (int i = 0; i < stream1.size(); i++) {
                Tree oldNode = stream1.get(i);
                Tree newNode = stream2.get(i);
                assert (oldNode.getType() == newNode.getType());
                assert (oldNode.getLabel().equals(newNode.getLabel()));
                mappings.addMapping(oldNode, newNode);
            }
        }

    }

}
