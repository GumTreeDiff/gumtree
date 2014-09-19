package fr.labri.gumtree.matchers.heuristic.cd;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;
import fr.labri.gumtree.matchers.Mapping;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.MatcherFactory;
import fr.labri.gumtree.tree.Tree;
import fr.labri.gumtree.tree.TreeUtils;

public class ChangeDistillerLeavesMatcher extends Matcher {

	public static final double LABEL_SIM_THRESHOLD = 0.5D;

	private static final QGramsDistance QGRAM = new QGramsDistance();

	public ChangeDistillerLeavesMatcher(Tree src, Tree dst) {
		super(src, dst);
	}

	@Override
	public void match() {
		List<Tree> dstLeaves = retainLeaves(TreeUtils.postOrder(dst));

		List<Mapping> leafMappings = new LinkedList<Mapping>();

		for (Iterator<Tree> srcLeaves = TreeUtils.leafIterator(
				TreeUtils.postOrderIterator(src)); srcLeaves.hasNext();) {
			for (Tree dstLeaf: dstLeaves) {
				Tree srcLeaf = srcLeaves.next();
				if (srcLeaf.isMatchable(dstLeaf)) {
					double sim = QGRAM.getSimilarity(srcLeaf.getLabel(), dstLeaf.getLabel());
					if (sim > LABEL_SIM_THRESHOLD) leafMappings.add(new Mapping(srcLeaf, dstLeaf));
				}
			}
		}

		Set<Tree> srcIgnored = new HashSet<>();
		Set<Tree> dstIgnored = new HashSet<>();
		Collections.sort(leafMappings, new LeafMappingComparator());
		while (leafMappings.size() > 0) {
			Mapping best = leafMappings.remove(0);
			if (!(srcIgnored.contains(best.getFirst()) || dstIgnored.contains(best.getSecond()))) {
				addMapping(best.getFirst(),best.getSecond());
				srcIgnored.add(best.getFirst());
				dstIgnored.add(best.getSecond());
			}
		}
	}

	public List<Tree> retainLeaves(List<Tree> trees) {
		Iterator<Tree> tIt = trees.iterator();
		while (tIt.hasNext()) {
			Tree t = tIt.next();
			if (!t.isLeaf()) tIt.remove();
		}
		return trees;
	}

	private class LeafMappingComparator implements Comparator<Mapping> {

		@Override
		public int compare(Mapping m1, Mapping m2) {
			return Double.compare(sim(m1), sim(m2));
		}

		public double sim(Mapping m) {
			return QGRAM.getSimilarity(m.getFirst().getLabel(), m.getSecond().getLabel());
		}

	}
	
	public static class ChangeDistillerLeavesMatcherFactory implements MatcherFactory {

		@Override
		public Matcher newMatcher(Tree src, Tree dst) {
			return new ChangeDistillerLeavesMatcher(src, dst);
		}
		
	}

}
