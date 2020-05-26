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
 * Copyright 2011-2016 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2016 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.matchers.heuristic.gt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.gumtreediff.matchers.Configurable;
import com.github.gumtreediff.matchers.GumTreeProperties;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.SimilarityMetrics;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeUtils;
import com.github.gumtreediff.tree.Type;
import com.github.gumtreediff.utils.SequenceAlgorithms;

public class SimpleBottomUpMatcher implements Matcher, Configurable {
	public static double SIM_THRESHOLD;

	public SimpleBottomUpMatcher() {
		configure();
	}

	@Override
	public void configure() {
		SIM_THRESHOLD = GumTreeProperties.getPropertyDouble("gt.bum.smt.sbup");
	}

	@Override
	public MappingStore match(ITree src, ITree dst, MappingStore mappings) {
		Implementation impl = new Implementation(src, dst, mappings);
		impl.match();
		return impl.mappings;
	}

	private static class Implementation {
		private final ITree src;
		private final ITree dst;
		private final MappingStore mappings;

		public Implementation(ITree src, ITree dst, MappingStore mappings) {
			this.src = src;
			this.dst = dst;
			this.mappings = mappings;
		}

		public void match() {
			for (ITree t : src.postOrder()) {
				if (t.isRoot()) {
					mappings.addMapping(t, this.dst);
					lastChanceMatch(t, this.dst);
					break;
				} else if (!(mappings.isSrcMapped(t) || t.isLeaf())) {
					List<ITree> candidates = getDstCandidates(t);
					ITree best = null;
					double max = -1D;
					int tSize = t.getDescendants().size();

					for (ITree cand : candidates) {
						double threshold = 1D / (1D + Math.log(cand.getDescendants().size() + tSize));
						double sim = SimilarityMetrics.chawatheSimilarity(t, cand, mappings);
						if (sim > max && sim >= threshold) {
							max = sim;
							best = cand;
						}
					}

					if (best != null) {
						lastChanceMatch(t, best);
						mappings.addMapping(t, best);
					}
				}
			}
		}

		protected List<ITree> getDstCandidates(ITree src) {
			List<ITree> seeds = new ArrayList<>();
			for (ITree c : src.getDescendants()) {
				ITree m = mappings.getDstForSrc(c);
				if (m != null)
					seeds.add(m);
			}
			List<ITree> candidates = new ArrayList<>();
			Set<ITree> visited = new HashSet<>();
			for (ITree seed : seeds) {
				while (seed.getParent() != null) {
					ITree parent = seed.getParent();
					if (visited.contains(parent))
						break;
					visited.add(parent);
					if (parent.getType() == src.getType() && !mappings.isDstMapped(parent) && !parent.isRoot())
						candidates.add(parent);
					seed = parent;
				}
			}

			return candidates;
		}

		protected void lastChanceMatch(ITree src, ITree dst) {
			lcsEqualMatching(src, dst);
			lcsStructureMatching(src, dst);
			if (src.isRoot() && dst.isRoot())
				histogramMatching(src, dst);
			else if (!(src.isRoot() || dst.isRoot()))
				if (src.getParent().getType() == dst.getParent().getType())
					histogramMatching(src, dst);
		}

		protected void lcsEqualMatching(ITree src, ITree dst) {
			List<ITree> srcChildren = src.getChildren();
			List<ITree> dstChildren = dst.getChildren();

			List<int[]> lcs = SequenceAlgorithms.longestCommonSubsequenceWithIsomorphism(srcChildren, dstChildren);
			for (int[] x : lcs) {
				ITree t1 = srcChildren.get(x[0]);
				ITree t2 = dstChildren.get(x[1]);
				if (mappings.areSrcsUnmapped(TreeUtils.preOrder(t1))
						&& mappings.areDstsUnmapped(TreeUtils.preOrder(t2)))
					mappings.addMappingRecursively(t1, t2);
			}
		}

		protected void lcsStructureMatching(ITree src, ITree dst) {
			List<ITree> srcChildren = src.getChildren();
			List<ITree> dstChildren = dst.getChildren();

			List<int[]> lcs = SequenceAlgorithms.longestCommonSubsequenceWithIsostructure(srcChildren, dstChildren);
			for (int[] x : lcs) {
				ITree t1 = srcChildren.get(x[0]);
				ITree t2 = dstChildren.get(x[1]);
				if (mappings.areSrcsUnmapped(TreeUtils.preOrder(t1))
						&& mappings.areDstsUnmapped(TreeUtils.preOrder(t2)))
					mappings.addMappingRecursively(t1, t2);
			}
		}

		protected void histogramMatching(ITree src, ITree dst) {
			List<ITree> srcChildren = src.getChildren();
			List<ITree> dstChildren = dst.getChildren();

			Map<Type, List<ITree>> srcHistogram = new HashMap<>();
			for (ITree c : srcChildren) {
				if (!srcHistogram.containsKey(c.getType()))
					srcHistogram.put(c.getType(), new ArrayList<>());
				srcHistogram.get(c.getType()).add(c);
			}

			Map<Type, List<ITree>> dstHistogram = new HashMap<>();
			for (ITree c : dstChildren) {
				if (!dstHistogram.containsKey(c.getType()))
					dstHistogram.put(c.getType(), new ArrayList<>());
				dstHistogram.get(c.getType()).add(c);
			}

			for (Type t : srcHistogram.keySet()) {
				if (dstHistogram.containsKey(t) && srcHistogram.get(t).size() == 1 && dstHistogram.get(t).size() == 1) {
					ITree t1 = srcHistogram.get(t).get(0);
					ITree t2 = dstHistogram.get(t).get(0);
					if (mappings.areBothUnmapped(t1, t2)) {
						mappings.addMapping(t1, t2);
						lastChanceMatch(t1, t2);
					}
				}
			}
		}
	}

}
