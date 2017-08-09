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

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.ITree;

import java.util.LinkedList;
import java.util.List;

/**
 * This implements the unmapped leaves optimization (Theta C).
 *
 */
public class UnmappedLeavesMatcherThetaC extends Matcher {

    /**
     * Instantiates a new matcher for Theta C.
     *
     * @param src the src
     * @param dst the dst
     * @param store the store
     */
    public UnmappedLeavesMatcherThetaC(ITree src, ITree dst, MappingStore store) {
        super(src, dst, store);
    }

    @Override
    protected void addMapping(ITree src, ITree dst) {
        assert (src != null);
        assert (dst != null);
        super.addMapping(src, dst);
    }

    /**
     * Match.
     */
    @Override
    public void match() {
        thetaC();
    }

    private void thetaC() {
        List<ITree> allNodesSrc = src.getTrees();
        List<ITree> allNodesDst = dst.getTrees();
        List<ITree> unmatchedNodes1 = new LinkedList<>();
        List<ITree> unmatchedNodes2 = new LinkedList<>();

        for (ITree node : allNodesSrc) {
            if (!mappings.hasSrc(node)) {
                unmatchedNodes1.add(node);
            }
        }
        for (ITree node : allNodesDst) {
            if (!mappings.hasDst(node)) {
                unmatchedNodes2.add(node);
            }
        }
        for (ITree node : unmatchedNodes1) {
            if (node.getChildren().size() == 0) {

                ITree parent = node.getParent();
                if (mappings.getDst(parent) != null) {
                    ITree partner = mappings.getDst(parent);
                    int pos = parent.getChildren().indexOf(node);
                    if (pos < partner.getChildren().size()) {
                        ITree child = partner.getChildren().get(pos);
                        if (child.getType() == node.getType()) {
                            if (child.getLabel().equals(node.getLabel())) {
                                ITree childPartner = mappings.getSrc(child);
                                if (childPartner != null) {
                                    if (!childPartner.getLabel().equals(node.getLabel())) {
                                        mappings.unlink(childPartner, child);
                                        addMapping(node, child);
                                    }
                                } else {
                                    addMapping(node, child);

                                }
                            } else {
                                ITree childPartner = mappings.getSrc(child);
                                if (childPartner != null) {
                                    if (mappings.getDst(childPartner.getParent()) == null) {
                                        if (!childPartner.getLabel().equals(child.getLabel())) {
                                            mappings.unlink(childPartner, child);
                                            addMapping(node, child);
                                        }
                                    }
                                } else {
                                    addMapping(node, child);
                                }
                            }
                        } else {
                            if (child.getChildren().size() == 1) {
                                child = child.getChildren().get(0);
                                if (child.getType() == node.getType()
                                        && child.getLabel().equals(node.getLabel())) {
                                    ITree childPartner = mappings.getSrc(child);
                                    if (childPartner != null) {
                                        if (!childPartner.getLabel().equals(node.getLabel())) {
                                            mappings.unlink(childPartner, child);
                                            addMapping(node, child);
                                        } else if (mappings
                                                .getDst(childPartner.getParent()) == null) {
                                            mappings.unlink(childPartner, child);
                                            addMapping(node, child);
                                        }
                                    }
                                }
                            } else {
                                for (int i = 0; i < partner.getChildren().size(); i++) {
                                    ITree possibleMatch = partner.getChildren().get(i);
                                    if (possibleMatch.getType() == node.getType()
                                            && possibleMatch.getLabel().equals(node.getLabel())) {
                                        ITree possibleMatchSrc = mappings.getSrc(possibleMatch);
                                        if (possibleMatchSrc == null) {
                                            addMapping(node, possibleMatch);
                                            break;
                                        } else {
                                            if (!possibleMatchSrc.getLabel()
                                                    .equals(possibleMatch.getLabel())) {
                                                mappings.unlink(possibleMatchSrc, possibleMatch);
                                                addMapping(node, possibleMatch);
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
        for (ITree node : unmatchedNodes2) {
            if (mappings.hasSrc(node)) {
                continue;
            }
            if (node.getChildren().size() == 0) {
                ITree parent = node.getParent();
                if (mappings.getSrc(parent) != null) {
                    ITree partner = mappings.getSrc(parent);
                    int pos = parent.getChildren().indexOf(node);
                    if (pos < partner.getChildren().size()) {
                        ITree child = partner.getChildren().get(pos);
                        if (child.getType() == node.getType()) {
                            if (child.getLabel().equals(node.getLabel())) {
                                ITree tree = mappings.getDst(child);
                                if (tree != null) {
                                    if (!tree.getLabel().equals(node.getLabel())) {
                                        mappings.unlink(child, tree);
                                        addMapping(child, node);
                                    }
                                } else {
                                    addMapping(child, node);
                                }
                            } else {
                                ITree childPartner = mappings.getDst(child);
                                if (childPartner != null) {
                                    if (mappings.getSrc(childPartner.getParent()) == null) {
                                        if (!childPartner.getLabel().equals(child.getLabel())) {
                                            mappings.unlink(child, childPartner);
                                            addMapping(child, node);
                                        }
                                    }
                                } else {
                                    addMapping(child, node);

                                }
                            }
                        } else {
                            if (child.getChildren().size() == 1) {
                                child = child.getChildren().get(0);
                                if (child.getType() == node.getType()
                                        && child.getLabel().equals(node.getLabel())) {
                                    ITree childPartner = mappings.getDst(child);
                                    if (childPartner != null) {
                                        if (!childPartner.getLabel().equals(node.getLabel())) {
                                            mappings.unlink(child, childPartner);
                                            addMapping(child, node);
                                        } else if (mappings
                                                .getSrc(childPartner.getParent()) == null) {
                                            mappings.unlink(childPartner, child);
                                            addMapping(node, child);
                                        }
                                    }
                                }
                            } else {
                                for (int i = 0; i < partner.getChildren().size(); i++) {
                                    ITree possibleMatch = partner.getChildren().get(i);
                                    if (possibleMatch.getType() == node.getType()
                                            && possibleMatch.getLabel().equals(node.getLabel())) {
                                        ITree possibleMatchDst = mappings.getDst(possibleMatch);
                                        if (possibleMatchDst == null) {
                                            addMapping(possibleMatch, node);
                                            break;
                                        } else {
                                            if (!possibleMatchDst.getLabel()
                                                    .equals(possibleMatch.getLabel())) {
                                                mappings.unlink(possibleMatch, possibleMatchDst);
                                                addMapping(possibleMatch, node);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (unmatchedNodes2.contains(parent)) {
                    ITree oldParent = parent;
                    parent = parent.getParent();
                    if (mappings.getSrc(parent) != null) {
                        ITree partner = mappings.getSrc(parent);
                        int pos = parent.getChildren().indexOf(oldParent);
                        if (pos < partner.getChildren().size()) {
                            ITree child = partner.getChildren().get(pos);
                            if (child.getType() == node.getType()
                                    && child.getLabel().equals(node.getLabel())) {
                                ITree tree = mappings.getDst(child);
                                if (tree != null) {
                                    if (!tree.getLabel().equals(node.getLabel())) {
                                        mappings.unlink(child, tree);
                                        addMapping(child, node);
                                    }
                                } else {
                                    addMapping(child, node);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
