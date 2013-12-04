package fr.labri.gumtree.matchers.optimal.zs;

import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;
import fr.labri.gumtree.tree.Tree;

public class ComputeTreeCost {

	private static QGramsDistance qgram = new QGramsDistance();

	public ComputeTreeCost() {
	}

	public double updateCost(Tree src, Tree dst) {
		if (src.getType() == dst.getType())
			if ("".equals(src.getLabel()) || "".equals(dst.getLabel())) return 1;
			else return 1D - qgram.getSimilarity(src.getLabel(), dst.getLabel());
		else return 3D;
	}

	public double insertCost(Tree tree) {
		return 1D;
	}

	public double deleteCost(Tree tree) {
		return 1D;
	}

}
