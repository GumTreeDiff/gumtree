package fr.labri.gumtree.matchers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import fr.labri.gumtree.tree.ITree;

public class MappingStore implements Iterable<Mapping> {
	
	private Map<ITree, ITree> srcs;
	private Map<ITree, ITree> dsts;
	
	public MappingStore(Set<Mapping> mappings) {
		this();
		for (Mapping m: mappings) link(m.getFirst(), m.getSecond());
	}
	
	public MappingStore() {
		srcs = new  HashMap<>();
		dsts = new HashMap<>();
	}
	
	public Set<Mapping> asSet() {
		Set<Mapping> mappings = new HashSet<>();
		for (ITree src : srcs.keySet()) 
			mappings.add(new Mapping(src, srcs.get(src)));
		return mappings;
	}
	
	public MappingStore copy() {
		return new MappingStore(asSet());
	}
	
	public void link(ITree src, ITree dst) {
		srcs.put(src, dst);
		dsts.put(dst, src);
	}
	
	public void unlink(ITree src, ITree dst) {
		srcs.remove(src);
		dsts.remove(dst);
	}
	
	public ITree firstMappedSrcParent(ITree src) {
		ITree p = src.getParent();
		if (p == null) return null;
		else {
			while (!hasSrc(p)) { 
				p = p.getParent();
				if (p == null) return p;
			}
			return p;
		}
	}
	
	public ITree firstMappedDstParent(ITree dst) {
		ITree p = dst.getParent();
		if (p == null) return null;
		else {
			while (!hasDst(p)) {
				p = p.getParent();
				if (p == null) return p;
			}
			return p;
		}
	}
	
	public ITree getDst(ITree src) {
		return srcs.get(src);
	}
	
	public ITree getSrc(ITree dst) {
		return dsts.get(dst);
	}
	
	public boolean hasSrc(ITree src) {
		return srcs.containsKey(src);
	}
	
	public boolean hasDst(ITree dst) {
		return dsts.containsKey(dst);
	}
	
	public boolean has(ITree src, ITree dst) {
		return srcs.get(src) == dst;
	}

	@Override
	public Iterator<Mapping> iterator() {
		return asSet().iterator();
	}
	
	@Override
	public String toString() {
		return asSet().toString();
	}

}
