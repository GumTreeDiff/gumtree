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

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.ITree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * This implements the identical subtree optimization Theta A.
 *
 */

public class IdenticalSubtreeMatcherThetaA extends Matcher {

    public IdenticalSubtreeMatcherThetaA(ITree src, ITree dst, MappingStore store) {
        super(src, dst, store);

    }

    @SuppressWarnings({ "checkstyle:AvoidEscapedUnicodeCharacters" })
    private String getHash(ITree node, HashMap<ITree, Integer> quickFind,
            HashMap<ITree, String> stringMap) {
        String tmp = node.getType() + node.getLabel();
        for (ITree child : node.getChildren()) {
            tmp += getHash(child, quickFind, stringMap);
        }
        tmp += "\\u2620";
        quickFind.put(node, tmp.hashCode());
        stringMap.put(node, tmp);
        return tmp;
    }

    private List<ITree> getNodeStream(ITree root) {
        LinkedList<ITree> nodes = new LinkedList<>();
        LinkedList<ITree> workList = new LinkedList<>();
        workList.add(root);
        while (!workList.isEmpty()) {
            ITree node = workList.removeFirst();
            nodes.add(node);
            for (int i = node.getChildren().size() - 1; i >= 0; i--) {
                workList.addFirst(node.getChildren().get(i));
            }
        }
        return nodes;
    }


    /**
     * Match with Theta A.
     */
    @Override
    public void match() {
        newUnchangedMatching();

    }

    private void newUnchangedMatching() {
        HashMap<ITree, Integer> quickFind = new HashMap<>();
        HashMap<ITree, String> stringMap = new HashMap<>();
        getHash(src, quickFind, stringMap);
        getHash(dst, quickFind, stringMap);
        HashMap<String, LinkedList<ITree>> nodeMapOld = new HashMap<>();
        List<ITree> streamOld = getNodeStream(src);
        List<ITree> streamNew = getNodeStream(dst);
        for (ITree node : streamOld) {
            String hashString = stringMap.get(node);
            LinkedList<ITree> nodeList = nodeMapOld.get(hashString);
            if (nodeList == null) {
                nodeList = new LinkedList<>();
                nodeMapOld.put(hashString, nodeList);
            }
            nodeList.add(node);
        }
        HashMap<String, LinkedList<ITree>> nodeMapNew = new HashMap<>();

        for (ITree node : streamNew) {
            String hashString = stringMap.get(node);
            LinkedList<ITree> nodeList = nodeMapNew.get(hashString);
            if (nodeList == null) {
                nodeList = new LinkedList<>();
                nodeMapNew.put(hashString, nodeList);
            }
            nodeList.add(node);
        }

        HashSet<Mapping> pairs = new HashSet<>();
        LinkedList<ITree> workList = new LinkedList<>();
        workList.add(src);

        while (!workList.isEmpty()) {
            ITree node = workList.removeFirst();
            LinkedList<ITree> oldList = nodeMapOld.get(stringMap.get(node));
            assert (oldList != null);
            LinkedList<ITree> newList = nodeMapNew.get(stringMap.get(node));
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
            List<ITree> stream1 = getNodeStream(mapping.getFirst());
            List<ITree> stream2 = getNodeStream(mapping.getSecond());
            stream1 = new ArrayList<>(stream1);
            stream2 = new ArrayList<>(stream2);
            assert (stream1.size() == stream2.size());
            for (int i = 0; i < stream1.size(); i++) {
                ITree oldNode = stream1.get(i);
                ITree newNode = stream2.get(i);
                assert (oldNode.getType() == newNode.getType());
                assert (oldNode.getLabel().equals(newNode.getLabel()));
                this.addMapping(oldNode, newNode);
            }
        }

    }

}
