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

import java.util.LinkedList;
import java.util.List;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeUtils;

/**
 * This implements the unmapped leaves optimization (Theta C).
 */
public class UnmappedLeavesMatcherThetaC implements Matcher {

    private Tree src;
    private Tree dst;
    private MappingStore mappings;

    @Override
    public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
        this.src = src;
        this.dst = dst;
        this.mappings = mappings;
        thetaC();
        return mappings;
    }

    private void thetaC() {
        List<Tree> allNodesSrc = TreeUtils.preOrder(src);
        List<Tree> allNodesDst = TreeUtils.preOrder(dst);
        List<Tree> unmatchedNodes1 = new LinkedList<>();
        List<Tree> unmatchedNodes2 = new LinkedList<>();

        for (Tree node : allNodesSrc) {
            if (!mappings.isSrcMapped(node)) {
                unmatchedNodes1.add(node);
            }
        }
        for (Tree node : allNodesDst) {
            if (!mappings.isDstMapped(node)) {
                unmatchedNodes2.add(node);
            }
        }
        for (Tree node : unmatchedNodes1) {
            if (node.getChildren().size() == 0) {

                Tree parent = node.getParent();
                if (mappings.getDstForSrc(parent) != null) {
                    Tree partner = mappings.getDstForSrc(parent);
                    int pos = parent.getChildren().indexOf(node);
                    if (pos < partner.getChildren().size()) {
                        Tree child = partner.getChildren().get(pos);
                        if (child.getType() == node.getType()) {
                            if (child.getLabel().equals(node.getLabel())) {
                                Tree childPartner = mappings.getSrcForDst(child);
                                if (childPartner != null) {
                                    if (!childPartner.getLabel().equals(node.getLabel())) {
                                        mappings.removeMapping(childPartner, child);
                                        mappings.addMapping(node, child);
                                    }
                                } else {
                                    mappings.addMapping(node, child);

                                }
                            } else {
                                Tree childPartner = mappings.getSrcForDst(child);
                                if (childPartner != null) {
                                    if (mappings.getDstForSrc(childPartner.getParent()) == null) {
                                        if (!childPartner.getLabel().equals(child.getLabel())) {
                                            mappings.removeMapping(childPartner, child);
                                            mappings.addMapping(node, child);
                                        }
                                    }
                                } else {
                                    mappings.addMapping(node, child);
                                }
                            }
                        } else {
                            if (child.getChildren().size() == 1) {
                                child = child.getChildren().get(0);
                                if (child.getType() == node.getType() && child.getLabel().equals(node.getLabel())) {
                                    Tree childPartner = mappings.getSrcForDst(child);
                                    if (childPartner != null) {
                                        if (!childPartner.getLabel().equals(node.getLabel())) {
                                            mappings.removeMapping(childPartner, child);
                                            mappings.addMapping(node, child);
                                        } else if (mappings.getDstForSrc(childPartner.getParent()) == null) {
                                            mappings.removeMapping(childPartner, child);
                                            mappings.addMapping(node, child);
                                        }
                                    }
                                }
                            } else {
                                for (int i = 0; i < partner.getChildren().size(); i++) {
                                    Tree possibleMatch = partner.getChildren().get(i);
                                    if (possibleMatch.getType() == node.getType()
                                            && possibleMatch.getLabel().equals(node.getLabel())) {
                                        Tree possibleMatchSrc = mappings.getSrcForDst(possibleMatch);
                                        if (possibleMatchSrc == null) {
                                            mappings.addMapping(node, possibleMatch);
                                            break;
                                        } else {
                                            if (!possibleMatchSrc.getLabel().equals(possibleMatch.getLabel())) {
                                                mappings.removeMapping(possibleMatchSrc, possibleMatch);
                                                mappings.addMapping(node, possibleMatch);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        for (Tree node : unmatchedNodes2) {
            if (mappings.isSrcMapped(node)) {
                continue;
            }
            if (node.getChildren().size() == 0) {
                Tree parent = node.getParent();
                if (mappings.getSrcForDst(parent) != null) {
                    Tree partner = mappings.getSrcForDst(parent);
                    int pos = parent.getChildren().indexOf(node);
                    if (pos < partner.getChildren().size()) {
                        Tree child = partner.getChildren().get(pos);
                        if (child.getType() == node.getType()) {
                            if (child.getLabel().equals(node.getLabel())) {
                                Tree tree = mappings.getDstForSrc(child);
                                if (tree != null) {
                                    if (!tree.getLabel().equals(node.getLabel())) {
                                        mappings.removeMapping(child, tree);
                                        mappings.addMapping(child, node);
                                    }
                                } else {
                                    mappings.addMapping(child, node);
                                }
                            } else {
                                Tree childPartner = mappings.getDstForSrc(child);
                                if (childPartner != null) {
                                    if (mappings.getSrcForDst(childPartner.getParent()) == null) {
                                        if (!childPartner.getLabel().equals(child.getLabel())) {
                                            mappings.removeMapping(child, childPartner);
                                            mappings.addMapping(child, node);
                                        }
                                    }
                                } else {
                                    mappings.addMapping(child, node);

                                }
                            }
                        } else {
                            if (child.getChildren().size() == 1) {
                                child = child.getChildren().get(0);
                                if (child.getType() == node.getType() && child.getLabel().equals(node.getLabel())) {
                                    Tree childPartner = mappings.getDstForSrc(child);
                                    if (childPartner != null) {
                                        if (!childPartner.getLabel().equals(node.getLabel())) {
                                            mappings.removeMapping(child, childPartner);
                                            mappings.addMapping(child, node);
                                        } else if (mappings.getSrcForDst(childPartner.getParent()) == null) {
                                            mappings.removeMapping(childPartner, child);
                                            mappings.addMapping(node, child);
                                        }
                                    }
                                }
                            } else {
                                for (int i = 0; i < partner.getChildren().size(); i++) {
                                    Tree possibleMatch = partner.getChildren().get(i);
                                    if (possibleMatch.getType() == node.getType()
                                            && possibleMatch.getLabel().equals(node.getLabel())) {
                                        Tree possibleMatchDst = mappings.getDstForSrc(possibleMatch);
                                        if (possibleMatchDst == null) {
                                            mappings.addMapping(possibleMatch, node);
                                            break;
                                        } else {
                                            if (!possibleMatchDst.getLabel().equals(possibleMatch.getLabel())) {
                                                mappings.removeMapping(possibleMatch, possibleMatchDst);
                                                mappings.addMapping(possibleMatch, node);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (unmatchedNodes2.contains(parent)) {
                    Tree oldParent = parent;
                    parent = parent.getParent();
                    if (mappings.getSrcForDst(parent) != null) {
                        Tree partner = mappings.getSrcForDst(parent);
                        int pos = parent.getChildren().indexOf(oldParent);
                        if (pos < partner.getChildren().size()) {
                            Tree child = partner.getChildren().get(pos);
                            if (child.getType() == node.getType() && child.getLabel().equals(node.getLabel())) {
                                Tree tree = mappings.getDstForSrc(child);
                                if (tree != null) {
                                    if (!tree.getLabel().equals(node.getLabel())) {
                                        mappings.removeMapping(child, tree);
                                        mappings.addMapping(child, node);
                                    }
                                } else {
                                    mappings.addMapping(child, node);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
