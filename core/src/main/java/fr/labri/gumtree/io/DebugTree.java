package fr.labri.gumtree.io;

import java.util.HashSet;
import java.util.Set;

import fr.labri.gumtree.tree.Mapping;
import fr.labri.gumtree.tree.Tree;

public final class DebugTree {
	
	private DebugTree() {
	}
	
	public static boolean areMappingsCorrect(Set<Mapping> mappings) {
		Set<Tree> src = new HashSet<Tree>();
		Set<Tree> dst = new HashSet<Tree>();
		
		for (Mapping m : mappings) {
			if (!src.contains(m.getFirst())) src.add(m.getFirst());
			else return false;
			
			if (!dst.contains(m.getSecond())) dst.add(m.getSecond());
			else return false;
		}
		return true;
	}

}
