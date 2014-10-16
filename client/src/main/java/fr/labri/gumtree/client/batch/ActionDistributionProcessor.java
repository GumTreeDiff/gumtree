package fr.labri.gumtree.client.batch;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import fr.labri.gumtree.actions.RootsClassifier;
import fr.labri.gumtree.client.TreeGeneratorRegistry;
import fr.labri.gumtree.matchers.MappingStore;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.MatcherFactories;
import fr.labri.gumtree.tree.ITree;
import fr.labri.gumtree.tree.TreeContext;

public class ActionDistributionProcessor extends AbstractFilePairsProcessor {
	
	public static void main(String[] args) {
		ActionDistributionProcessor g = new ActionDistributionProcessor(args[0]);
		g.process();
	}

	private Map<String, Integer> dist; 

	public ActionDistributionProcessor(String folder) {
		super(folder);
	}

	protected void init() {
		ensureFolder("out");
		dist = new HashMap<>();
	}

	@Override
	public void processFilePair(String fsrc, String fdst) throws IOException {
		TreeContext src = TreeGeneratorRegistry.getInstance().getTree(fsrc);
		TreeContext dst = TreeGeneratorRegistry.getInstance().getTree(fdst);
		Matcher matcher = MatcherFactories.newMatcher(src.getRoot(), dst.getRoot());
		RootsClassifier c = new RootsClassifier(src, dst, matcher);
		c.classify();
		MappingStore mappings = matcher.getMappings();
		for (ITree t: c.getDstUpdTrees()) inc("UPD " + dst.getTypeLabel(t.getType()) + " IN " + dst.getTypeLabel(t.getParent().getType()));
		for (ITree t: c.getSrcDelTrees()) if (!c.getSrcDelTrees().contains(t.getParent())) inc("DEL " + src.getTypeLabel(t.getType()) + " IN " + src.getTypeLabel(t.getParent().getType()));
		for (ITree t: c.getDstAddTrees()) if (!c.getDstAddTrees().contains(t.getParent())) inc("ADD " + dst.getTypeLabel(t) + " IN " + dst.getTypeLabel(t.getParent()));
		for (ITree t: c.getSrcMvTrees()) if (!c.getSrcMvTrees().contains(t.getParent())) inc("MOV " + src.getTypeLabel(t)
				+ " FROM " + src.getTypeLabel(t.getParent()) + " TO " + dst.getTypeLabel(mappings.getDst(t).getParent()));
	}
	
	private void inc(String key) {
		if (!dist.containsKey(key)) dist.put(key, 1);
		else dist.put(key, dist.get(key) + 1);
	}

	
	protected void finish() {
		try {
			String name = nextFile("out", "dist", "csv");
			FileWriter w = new FileWriter(name);
			w.append("ACTION;NB\n");
			TreeSet<String> keys = new TreeSet<>(dist.keySet());
			for (String key: keys)
				w.append(String.format("%s;%d\n", key, dist.get(key)));
			
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
