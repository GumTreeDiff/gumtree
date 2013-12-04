package fr.labri.gumtree.actions;

import java.util.HashSet;
import java.util.Set;

import fr.labri.gumtree.tree.Mapping;
import fr.labri.gumtree.tree.MappingStore;
import fr.labri.gumtree.tree.Tree;

public class ProcessMappings {

	private Set<Mapping> rawMappings;

	private MappingStore mappings;

	private Tree src;

	private Tree dst;

	private Set<Tree> srcDelRoots;

	private Set<Tree> srcUpd;

	private Set<Tree> srcMv;

	private Set<Tree> srcMvRoots;

	private Set<Tree> srcMis;

	private Set<Tree> dstAddRoots;

	private Set<Tree> dstUpd;

	private Set<Tree> dstMv;

	private Set<Tree> dstMvRoots;

	private Set<Tree> dstMis;

	public ProcessMappings(Tree src, Tree dst, Set<Mapping> mappings) {
		this.src = src;
		this.dst = dst;
		this.rawMappings = mappings;
		this.mappings = new MappingStore(mappings);
		detectAddAndDel();
		detectUpd();
		detectMvTrees();
		detectMvRoots();
		detectMisRoots();
	}

	private void detectMvTrees() {
		srcMv = new HashSet<Tree>();
		dstMv = new HashSet<Tree>();
		for (Tree t1: src.getDescendants()) {
			if (mappings.hasSrc(t1)) {
				Tree t2 = mappings.getDst(t1);
				Tree p1 = t1.getParent();
				Tree p2 = t2.getParent();
				if (mappings.has(p1, p2)) continue;
				else { srcMv.add(t1); dstMv.add(t2); } 
			}
		}
	}

	private void detectMvRoots() {
		srcMvRoots = new HashSet<Tree>();
		dstMvRoots = new HashSet<Tree>();
		for (Tree t1: srcMv) {
			Tree t2 = mappings.getDst(t1);
			Tree p1 = mappings.firstMappedSrcParent(t1);
			Tree p2 = mappings.firstMappedDstParent(t2);
			if (!mappings.has(p1, p2)) { srcMvRoots.add(t1); dstMvRoots.add(t2); }
		}
	}

	private void detectMisRoots() {
		srcMis = new HashSet<Tree>();
		dstMis = new HashSet<Tree>();
		for (Tree t1: srcMv) {
			Tree t2 = mappings.getDst(t1);
			Tree p1 = mappings.firstMappedSrcParent(t1);
			Tree p2 = mappings.firstMappedDstParent(t2);
			if (mappings.has(p1, p2) && p1 != null & p2 != null) { 
				int pos1 = p1.getChildPosition(t1);
				int pos2 = p2.getChildPosition(t2);
				if (pos1 != pos2) { srcMis.add(t1); dstMis.add(t2); }
			}
		}
	}

	private void detectAddAndDel() {
		srcDelRoots = new HashSet<Tree>();
		dstAddRoots = new HashSet<Tree>();
		for (Tree t: src.getDescendants()) if (!mappings.hasSrc(t)) if (mappings.hasSrc(t.getParent())) srcDelRoots.add(t);
		for (Tree t: dst.getDescendants()) if (!mappings.hasDst(t)) if (mappings.hasDst(t.getParent())) dstAddRoots.add(t);
	}

	private void detectUpd() {
		srcUpd = new HashSet<Tree>();
		dstUpd = new HashSet<Tree>();
		for (Mapping m: rawMappings) 
			if (!m.getFirst().getLabel().equals(m.getSecond().getLabel()) && !"".equals(m.getFirst().getLabel())) { 
				srcUpd.add(m.getFirst());
				dstUpd.add(m.getSecond()); 
			}
	}

	public Set<Tree> getSrcDelRoots() {
		return srcDelRoots;
	}

	public void setSrcDelRoots(Set<Tree> srcDelRoots) {
		this.srcDelRoots = srcDelRoots;
	}

	public Set<Tree> getSrcUpd() {
		return srcUpd;
	}

	public void setSrcUpd(Set<Tree> srcUpd) {
		this.srcUpd = srcUpd;
	}

	public Set<Tree> getSrcMvRoots() {
		return srcMvRoots;
	}

	public void setSrcMvRoots(Set<Tree> srcMvRoots) {
		this.srcMvRoots = srcMvRoots;
	}

	public Set<Tree> getSrcMis() {
		return srcMis;
	}

	public void setSrcMis(Set<Tree> srcMis) {
		this.srcMis = srcMis;
	}

	public Set<Tree> getDstAddRoots() {
		return dstAddRoots;
	}

	public void setDstAddRoots(Set<Tree> dstAddRoots) {
		this.dstAddRoots = dstAddRoots;
	}

	public Set<Tree> getDstUpd() {
		return dstUpd;
	}

	public void setDstUpd(Set<Tree> dstUpd) {
		this.dstUpd = dstUpd;
	}

	public Set<Tree> getDstMvRoots() {
		return dstMvRoots;
	}

	public void setDstMvRoots(Set<Tree> dstMvRoots) {
		this.dstMvRoots = dstMvRoots;
	}

	public Set<Tree> getDstMis() {
		return dstMis;
	}

	public void setDstMis(Set<Tree> dstMis) {
		this.dstMis = dstMis;
	}
}
