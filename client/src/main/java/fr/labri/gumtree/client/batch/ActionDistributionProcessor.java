package fr.labri.gumtree.client.batch;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import fr.labri.gumtree.actions.RootsClassifier;
import fr.labri.gumtree.client.MatcherFactory;
import fr.labri.gumtree.client.TreeGeneratorRegistry;
import fr.labri.gumtree.matchers.composite.Matcher;
import fr.labri.gumtree.tree.MappingStore;
import fr.labri.gumtree.tree.Tree;

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
		Tree src = TreeGeneratorRegistry.getInstance().getTree(fsrc);
		Tree dst = TreeGeneratorRegistry.getInstance().getTree(fdst);
		Matcher matcher = MatcherFactory.createMatcher(src, dst);
		RootsClassifier c = new RootsClassifier(src, dst, matcher);
		c.classify();
		MappingStore mappings = matcher.getMappings();
		for (Tree t: c.getDstUpdTrees()) inc("UPD " + t.getTypeLabel() + " IN " + t.getParent().getTypeLabel());
		for (Tree t: c.getSrcDelTrees()) if (!c.getSrcDelTrees().contains(t.getParent())) inc("DEL " + t.getTypeLabel() + " IN " + t.getParent().getTypeLabel());
		for (Tree t: c.getDstAddTrees()) if (!c.getDstAddTrees().contains(t.getParent())) inc("ADD " + t.getTypeLabel() + " IN " + t.getParent().getTypeLabel());
		for (Tree t: c.getSrcMvTrees()) if (!c.getSrcMvTrees().contains(t.getParent())) inc("MOV " + t.getTypeLabel()
				+ " FROM " + t.getParent().getTypeLabel() + " TO " + mappings.getDst(t).getParent().getTypeLabel());
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
