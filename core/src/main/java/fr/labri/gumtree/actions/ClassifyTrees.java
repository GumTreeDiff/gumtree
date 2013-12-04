package fr.labri.gumtree.actions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.labri.gumtree.matchers.composite.Matcher;
import fr.labri.gumtree.tree.Mapping;
import fr.labri.gumtree.tree.MappingStore;
import fr.labri.gumtree.tree.Tree;

public abstract class ClassifyTrees {
	
	protected Set<Tree> srcUpdTrees;

	protected Set<Tree> dstUpdTrees;

	protected Set<Tree> srcMvTrees;

	protected Set<Tree> dstMvTrees;

	protected Set<Tree> srcDelTrees;

	protected Set<Tree> dstAddTrees;
	
	protected Tree src;
	
	protected Tree dst;
	
	protected MappingStore mappings;
	
	protected List<Action> actions;
	
	public ClassifyTrees(Tree src, Tree dst, Set<Mapping> rawMappings, List<Action> actions) {
		this(src, dst, rawMappings);
		this.actions = actions;
		classify();
	}
	
	public ClassifyTrees(Tree src, Tree dst, Matcher m) {
		this(src, dst, m.getMappingSet());
		GenerateActions g = new GenerateActions(src, dst, m.getMappingSet());
		this.actions = g.getActions();
		classify();
	}
	
	private ClassifyTrees(Tree src, Tree dst, Set<Mapping> rawMappings) {
		this.src = src;
		this.dst = dst;
		this.mappings = new MappingStore(rawMappings);
		this.srcDelTrees = new HashSet<>();
		this.srcMvTrees = new HashSet<>();
		this.srcUpdTrees = new HashSet<>();
		this.dstMvTrees = new HashSet<>();
		this.dstAddTrees = new HashSet<>();
		this.dstUpdTrees = new HashSet<>();
	}
	
	public abstract void classify();

	public Set<Tree> getSrcUpdTrees() {
		return srcUpdTrees;
	}

	public Set<Tree> getDstUpdTrees() {
		return dstUpdTrees;
	}

	public Set<Tree> getSrcMvTrees() {
		return srcMvTrees;
	}

	public Set<Tree> getDstMvTrees() {
		return dstMvTrees;
	}

	public Set<Tree> getSrcDelTrees() {
		return srcDelTrees;
	}

	public Set<Tree> getDstAddTrees() {
		return dstAddTrees;
	}

}
