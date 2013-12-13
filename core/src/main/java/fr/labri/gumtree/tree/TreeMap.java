package fr.labri.gumtree.tree;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class TreeMap {
	
	private TIntObjectMap<Tree> trees;
	
	public TreeMap(Tree tree) {
		trees = new TIntObjectHashMap<Tree>();
		for(Tree t: tree.getTrees()) trees.put(t.getId(), t);
	}
	
	public Tree getTree(int id) {
		return trees.get(id);
	}

}
