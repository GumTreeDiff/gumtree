package fr.labri.gumtree.matchers.optimal.zs;

import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;
import fr.labri.gumtree.tree.ITree;

public class ComputeTreeCost {

	private static QGramsDistance qgram = new QGramsDistance();

	public ComputeTreeCost() {
	}

	public double updateCost(ITree src, ITree dst) {
		if (src.getType() == dst.getType())
			if ("".equals(src.getLabel()) || "".equals(dst.getLabel())) return 1;
			else return 1D - qgram.getSimilarity(src.getLabel(), dst.getLabel());
		else return 3D;
	}

	public double insertCost(ITree tree) {
		return 1D;
	}

	public double deleteCost(ITree tree) {
		return 1D;
	}

}
