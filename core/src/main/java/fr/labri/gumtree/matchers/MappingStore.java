package fr.labri.gumtree.matchers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import fr.labri.gumtree.tree.Tree;

public class MappingStore implements Iterable<Mapping> {
	
	private Map<Tree, Tree> srcs;
	
	private Map<Tree, Tree> dsts;
	
	public MappingStore(Set<Mapping> mappings) {
		srcs = new  HashMap<Tree, Tree>();
		dsts = new HashMap<Tree, Tree>();
		for (Mapping m: mappings) link(m.getFirst(), m.getSecond());
	}
	
	public MappingStore() {
		srcs = new  HashMap<Tree, Tree>();
		dsts = new HashMap<Tree, Tree>();
	}
	
	public MappingStore(int size) {
		srcs = new  HashMap<Tree, Tree>(size);
		dsts = new HashMap<Tree, Tree>(size);
	}
	
	public Set<Mapping> asSet() {
		Set<Mapping> mappings = new HashSet<>();
		for (Tree src : srcs.keySet()) mappings.add(new Mapping(src, srcs.get(src)));
		return mappings;
	}
	
	public MappingStore copy() {
		return new MappingStore(asSet());
	}
	
	public void link(Tree src, Tree dst) {
		srcs.put(src, dst);
		dsts.put(dst, src);
	}
	
	public void unlink(Tree src, Tree dst) {
		srcs.remove(src);
		dsts.remove(dst);
	}
	
	public Tree firstMappedSrcParent(Tree src) {
		Tree p = src.getParent();
		if (p == null) return null;
		else {
			while (!hasSrc(p)) { 
				p = p.getParent();
				if (p == null) return p;
			}
			return p;
		}
	}
	
	public Tree firstMappedDstParent(Tree dst) {
		Tree p = dst.getParent();
		if (p == null) return null;
		else {
			while (!hasDst(p)) {
				p = p.getParent();
				if (p == null) return p;
			}
			return p;
		}
	}
	
	public Tree getDst(Tree src) {
		return srcs.get(src);
	}
	
	public Tree getSrc(Tree dst) {
		return dsts.get(dst);
	}
	
	public boolean hasSrc(Tree src) {
		return srcs.containsKey(src);
	}
	
	public boolean hasDst(Tree dst) {
		return dsts.containsKey(dst);
	}
	
	public boolean has(Tree src, Tree dst) {
		return srcs.get(src) == dst;
	}

	@Override
	public Iterator<Mapping> iterator() {
		return asSet().iterator();
	}

}
