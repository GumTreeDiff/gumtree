package fr.labri.gumtree.tree;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MultiMappingStore implements Iterable<Mapping> {

	private Map<Tree, Set<Tree>> srcs;

	private Map<Tree, Set<Tree>> dsts;

	public MultiMappingStore(Set<Mapping> mappings) {
		srcs = new  HashMap<Tree, Set<Tree>>();
		dsts = new HashMap<Tree, Set<Tree>>();
		for (Mapping m: mappings) link(m.getFirst(), m.getSecond());
	}

	public MultiMappingStore() {
		srcs = new  HashMap<Tree, Set<Tree>>();
		dsts = new HashMap<Tree, Set<Tree>>();
	}

	public Set<Mapping> getMappings() {
		Set<Mapping> mappings = new HashSet<>();
		for (Tree src : srcs.keySet())
			for(Tree dst: srcs.get(src))
				mappings.add(new Mapping(src, dst));
		return mappings;
	}

	public void link(Tree src, Tree dst) {
		if (!srcs.containsKey(src)) srcs.put(src, new HashSet<Tree>());
		srcs.get(src).add(dst);
		if (!dsts.containsKey(dst)) dsts.put(dst, new HashSet<Tree>());
		dsts.get(dst).add(src);
	}

	public void unlink(Tree src, Tree dst) {
		srcs.get(src).remove(dst);
		dsts.get(dst).remove(src);
	}

	public Set<Tree> getDst(Tree src) {
		return srcs.get(src);
	}

	public Set<Tree> getSrcs() {
		return srcs.keySet();
	}
	
	public Set<Tree> getDsts() {
		return dsts.keySet();
	}
	
	public Set<Tree> getSrc(Tree dst) {
		return dsts.get(dst);
	}

	public boolean hasSrc(Tree src) {
		return srcs.containsKey(src);
	}

	public boolean hasDst(Tree dst) {
		return dsts.containsKey(dst);
	}

	public boolean has(Tree src, Tree dst) {
		return srcs.get(src).contains(dst);
	}
	
	public boolean isSrcUnique(Tree src) {
		return srcs.get(src).size() == 1 && dsts.get(srcs.get(src).iterator().next()).size() == 1;
	}

	@Override
	public Iterator<Mapping> iterator() {
		return getMappings().iterator();
	}

}
