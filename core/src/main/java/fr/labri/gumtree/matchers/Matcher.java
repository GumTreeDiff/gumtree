package fr.labri.gumtree.matchers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import fr.labri.gumtree.tree.Tree;

public abstract class Matcher {
	
	public final static Logger LOGGER = Logger.getLogger("fr.labri.gumtree.matchers");
	
	protected Tree src;
	
	protected Tree dst;

	protected MappingStore mappings;
	
	public Matcher(Tree src, Tree dst) {
		this.src = src;
		this.dst = dst;
		this.mappings = new MappingStore();
	}
	
	public abstract void match();
	
	public MappingStore getMappings() {
		return mappings;
	}
	
	public void setMappings(MappingStore mappings) {
		this.mappings = mappings;
	}
	
	public Set<Mapping> getMappingSet() {
		return mappings.asSet();
	}
	
	public Tree getSrc() {
		return src;
	}

	public void setSrc(Tree src) {
		this.src = src;
	}

	public Tree getDst() {
		return dst;
	}

	public void setDst(Tree dst) {
		this.dst = dst;
	}
	
	protected void addMapping(Tree src, Tree dst) {
		src.setMatched(true);
		dst.setMatched(true);
		mappings.link(src, dst);
	}
	
	protected void addFullMapping(Tree src, Tree dst) {
		List<Tree> csrcs = src.getTrees();
		List<Tree> cdsts = dst.getTrees();
		for (int i = 0; i < csrcs.size(); i++) {
			Tree csrc = csrcs.get(i);
			Tree cdst = cdsts.get(i);
			addMapping(csrc, cdst);
		}
	}
	
	protected double chawatheSimilarity(Tree src, Tree dst) {
		int max = Math.max(src.getDescendants().size(), dst.getDescendants().size());
		return (double) numberOfCommonDescendants(src, dst) / (double) max;
	}
	
	protected double diceSimilarity(Tree src, Tree dst) {
		double c = (double) numberOfCommonDescendants(src, dst);
		return (2D * c) / ((double) src.getDescendants().size() + (double) dst.getDescendants().size());
	}
	
	protected double jaccardSimilarity(Tree src, Tree dst) {
		double num = (double) numberOfCommonDescendants(src, dst);
		double den = (double) src.getDescendants().size() + (double) dst.getDescendants().size() - num;
		return num/den;
	}
	
	protected int numberOfCommonDescendants(Tree src, Tree dst) {
		Set<Tree> dstDescs = new HashSet<Tree>(dst.getDescendants());
		int common = 0;
		
		for (Tree t: src.getDescendants()) {
			Tree m = mappings.getDst(t);
			if (m != null && dstDescs.contains(m)) common++;
		}

		return common;
	}
	
	protected void clean() {
		for (Tree t: src.getTrees()) if (!mappings.hasSrc(t)) t.setMatched(false);
		for (Tree t: dst.getTrees()) if (!mappings.hasDst(t)) t.setMatched(false);
	}

}
