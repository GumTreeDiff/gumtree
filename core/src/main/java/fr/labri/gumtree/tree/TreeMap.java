package fr.labri.gumtree.tree;

import java.util.HashMap;
import java.util.Map;

public class TreeMap {
	
	Map<Integer, Tree> trees;
	
	public TreeMap(Tree tree) {
		trees = new HashMap<Integer, Tree>();
		for(Tree t: tree.getTrees()) trees.put(t.getId(), t);
	}
	
	public Tree getTree(int id) {
		return trees.get(id);
	}

}
