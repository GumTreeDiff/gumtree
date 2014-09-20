package fr.labri.gumtree.actions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.labri.gumtree.actions.model.Action;
import fr.labri.gumtree.matchers.Mapping;
import fr.labri.gumtree.matchers.MappingStore;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.tree.ITree;

public abstract class TreeClassifier {
	
	protected Set<ITree> srcUpdTrees;

	protected Set<ITree> dstUpdTrees;

	protected Set<ITree> srcMvTrees;

	protected Set<ITree> dstMvTrees;

	protected Set<ITree> srcDelTrees;

	protected Set<ITree> dstAddTrees;
	
	protected ITree src;
	
	protected ITree dst;
	
	protected MappingStore mappings;
	
	protected List<Action> actions;
	
	public TreeClassifier(ITree src, ITree dst, Set<Mapping> rawMappings, List<Action> actions) {
		this(src, dst, rawMappings);
		this.actions = actions;
		classify();
	}
	
	public TreeClassifier(ITree src, ITree dst, Matcher m) {
		this(src, dst, m.getMappingSet());
		ActionGenerator g = new ActionGenerator(src, dst, m.getMappings());
		g.generate();
		this.actions = g.getActions();
		classify();
	}
	
	private TreeClassifier(ITree src, ITree dst, Set<Mapping> rawMappings) {
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

	public Set<ITree> getSrcUpdTrees() {
		return srcUpdTrees;
	}

	public Set<ITree> getDstUpdTrees() {
		return dstUpdTrees;
	}

	public Set<ITree> getSrcMvTrees() {
		return srcMvTrees;
	}

	public Set<ITree> getDstMvTrees() {
		return dstMvTrees;
	}

	public Set<ITree> getSrcDelTrees() {
		return srcDelTrees;
	}

	public Set<ITree> getDstAddTrees() {
		return dstAddTrees;
	}

}
