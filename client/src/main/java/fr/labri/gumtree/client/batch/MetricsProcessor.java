package fr.labri.gumtree.client.batch;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import fr.labri.gumtree.actions.ActionGenerator;
import fr.labri.gumtree.actions.model.Action;
import fr.labri.gumtree.actions.model.Delete;
import fr.labri.gumtree.actions.model.Insert;
import fr.labri.gumtree.actions.model.Move;
import fr.labri.gumtree.actions.model.Update;
import fr.labri.gumtree.gen.TreeGeneratorRegistry;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.MatcherFactories;
import fr.labri.gumtree.tree.ITree;

public class MetricsProcessor extends AbstractFilePairsProcessor {
	
	public static void main(String[] args) {
		MetricsProcessor g = new MetricsProcessor(args[0]);
		g.process();
	}

	private FileWriter csv; 

	public MetricsProcessor(String folder) {
		super(folder);
	}

	protected void init() {
		ensureFolder("out");
		copyResource("assets/gen_metrics_stats.r", "out/gen_metrics_stats.r");
		try {
			String fcsv = nextFile("out", "metrics", "csv");
			csv = new FileWriter(fcsv);
			csv.append("FILE_0;FILE_1;SIZE_0;SIZE_1;SIZE_SCRIPT;NB_INS;NB_UP;NB_DEL;NB_MV;T_PARSE;T_MATCH;T_SCRIPT;T_TOTAL\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void processFilePair(String fsrc, String fdst) throws IOException {
		long tic, toc;
		tic = System.currentTimeMillis();
		ITree src = TreeGeneratorRegistry.getInstance().getTree(fsrc).getRoot();
		ITree dst = TreeGeneratorRegistry.getInstance().getTree(fdst).getRoot();
		toc = System.currentTimeMillis();
		int sSrc = src.getSize();
		int sDst = dst.getSize();
		long tParse = toc - tic;
		tic = System.currentTimeMillis();
		Matcher matcher = MatcherFactories.newMatcher(src, dst);
		toc = System.currentTimeMillis();
		long tMatch = toc - tic;
		tic = System.currentTimeMillis();
		ActionGenerator g = new ActionGenerator(src, dst, matcher.getMappings());
		g.generate();
		toc = System.currentTimeMillis();
		long tScript = toc - tic;
		long tTotal = tParse + tMatch + tScript;

		List<Action> actions = g.getActions();
		int sScript = actions.size();
		int sIns = 0, sUp = 0, sDel = 0, sMv = 0;
		for (Action a: actions) {
			if (a instanceof Insert) sIns++;
			else if (a instanceof Delete) sDel++;
			else if (a instanceof Move) sMv++;
			else if (a instanceof Update) sUp++;
		}

		try {
			csv.append(String.format("%s;%s;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d\n", fsrc, fdst, sSrc, sDst, sScript, sIns, sDel, sUp, sMv, tParse, tMatch, tScript, tTotal));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void finish() {
		try {
			csv.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
