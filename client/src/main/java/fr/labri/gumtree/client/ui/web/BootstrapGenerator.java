package fr.labri.gumtree.client.ui.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.labri.gumtree.actions.RootAndLeavesClassifier;
import fr.labri.gumtree.algo.StringAlgorithms;
import fr.labri.gumtree.matchers.composite.Matcher;
import fr.labri.gumtree.tree.MappingStore;
import fr.labri.gumtree.tree.Tree;

public final class BootstrapGenerator {
	
	private static final String SRC_MV_SPAN = "<span class=\"%s\" id=\"move-src-%d\" data-title=\"%s\">";
	private static final String DST_MV_SPAN = "<span class=\"%s\" id=\"move-dst-%d\" data-title=\"%s\">";
	private static final String ADD_DEL_SPAN = "<span class=\"%s\" data-title=\"%s\">";
	private static final String UPD_SPAN = "<span class=\"cupd\">";
	private static final String ID_SPAN = "<span class=\"marker\" id=\"mapping-%d\"></span>";
	private static final String END_SPAN = "</span>";
	private static final String END_DIV = "</div>\n";
	private static final String CODE_HEAD = "<div class=\"span6 max-height\">\n<h3>%s</h3>\n<pre class=\"pre max-height\" id=\"%s\">\n";
	private static final String CODE_END = "</pre>\n</div>\n";
	
	private BootstrapGenerator() {
	}

	public static void produceHTML(String srcPath, String dstPath, Tree src, Tree dst, Matcher matcher, String output) throws IOException {
		FileWriter w = new FileWriter(output);
		w.write(produceHTML(srcPath, dstPath, src, dst, matcher));
		w.close();
	}
	
	public static String produceHTML(String srcPath, String dstPath, Tree src, Tree dst, Matcher matcher) throws IOException {
		RootAndLeavesClassifier c = new RootAndLeavesClassifier(src, dst, matcher);
		MappingStore mappings = new MappingStore(matcher.getMappingSet());
		Map<Integer, Integer> ids = new HashMap<Integer, Integer>();
		
		int uId = 1;
		int mId = 1;
		
		TagIndex ltags = new TagIndex();
		for (Tree t: src.getTrees()) {
			if (c.getSrcMvTrees().contains(t)) {
				ids.put(mappings.getDst(t).getId(), mId);
				ltags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
				ltags.addTags(t.getPos(), String.format(SRC_MV_SPAN, "token mv", mId++, tooltip(t)), t.getEndPos(), END_SPAN);
			} if (c.getSrcUpdTrees().contains(t)) {
				ids.put(mappings.getDst(t).getId(), mId);
				ltags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
				ltags.addTags(t.getPos(), String.format(SRC_MV_SPAN, "token upd", mId++, tooltip(t)), t.getEndPos(), END_SPAN);
				
				List<int[]> hunks = StringAlgorithms.hunks(t.getLabel(), mappings.getDst(t).getLabel());
				for(int[] hunk: hunks)
					ltags.addTags(t.getPos() + hunk[0], UPD_SPAN, t.getPos() + hunk[1], END_SPAN);
				
			} if (c.getSrcDelTrees().contains(t)) {
				ltags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
				ltags.addTags(t.getPos(), String.format(ADD_DEL_SPAN, "token del", tooltip(t)), t.getEndPos(), END_SPAN);
			}
		}

		TagIndex rtags = new TagIndex();
		for (Tree t: dst.getTrees()) {
			if (c.getDstMvTrees().contains(t)) {
				int dId = ids.get(t.getId());
				rtags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
				rtags.addTags(t.getPos(), String.format(DST_MV_SPAN, "token mv", dId, tooltip(t)), t.getEndPos(), END_SPAN);

			} if (c.getDstUpdTrees().contains(t)) {
				int dId = ids.get(t.getId());
				rtags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
				rtags.addTags(t.getPos(), String.format(DST_MV_SPAN, "token upd", dId, tooltip(t)), t.getEndPos(), END_SPAN);
				List<int[]> hunks = StringAlgorithms.hunks(mappings.getSrc(t).getLabel(), t.getLabel());
				for(int[] hunk: hunks)
					rtags.addTags(t.getPos() + hunk[2], UPD_SPAN, t.getPos() + hunk[3], END_SPAN);
			} if (c.getDstAddTrees().contains(t)) {
				rtags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
				rtags.addTags(t.getPos(), String.format(ADD_DEL_SPAN, "token add", tooltip(t)), t.getEndPos(), END_SPAN);
			}
		}

		StringWriter w = new StringWriter();
		appendHeader(w);

		BufferedReader r = new BufferedReader(new FileReader(srcPath));
		int cursor = 0;
		
		w.append(String.format(CODE_HEAD, getFileName(srcPath), "src"));
		
		while (r.ready()) {
			char cr = (char) r.read();
			w.append(ltags.getEndTags(cursor));
			w.append(ltags.getStartTags(cursor));
			append(cr, w);
			cursor++;
		}
		w.append(ltags.getEndTags(cursor));
		r.close();
		w.append(CODE_END);

		r = new BufferedReader(new FileReader(dstPath));
		cursor = 0;
		
		w.append(String.format(CODE_HEAD, getFileName(dstPath), "dst"));
	
		while (r.ready()) {
			char cr = (char) r.read();
			w.append(rtags.getEndTags(cursor));
			w.append(rtags.getStartTags(cursor));
			append(cr, w);
			cursor++;
		}
		w.append(rtags.getEndTags(cursor));
		r.close();
		w.append(CODE_END);

		appendFooter(w);
		
		w.close();
		
		return w.toString();
	}
	
	private static String tooltip(Tree t) {
		return t.getParent().getTypeLabel() + "/" + t.getTypeLabel();
	}

	private static void append(char cr, Writer w) throws IOException {
		if (cr == '<') w.append("&lt;");
		else if (cr == '>') w.append("&gt;");
		else if (cr == '&') w.append("&amp;");
		else w.append(cr);
	}

	private static void appendHeader(Writer w) throws IOException {
		w.append("<!DOCTYPE html>\n");
		w.append("<html lang=\"en\">\n");
		w.append("<head>\n");
		w.append("\t<meta charset=\"utf-8\">\n");
		w.append("\t<title>GumTree Diff</title>\n");
		w.append("\t<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
		w.append("\t<link href=\"assets/bootstrap.min.css\" rel=\"stylesheet\">\n");
		w.append("\t<link href=\"assets/gumtree.css\" rel=\"stylesheet\">\n");
		w.append("</head>\n\n");
		w.append("<body>\n\n");
		w.append("<div class=\"container-fluid\">\n");
		w.append("<div class=\"row-fluid\">\n");
		w.append("<div class=\"span12\">\n");
		w.append("<div class=\"btn-toolbar pull-right\">\n");
		w.append("<div class=\"btn-group\">\n");
		w.append("<a href=\"/src/xml\" class=\"btn btn-small\">Src (xml)</a>\n");
		w.append("<a href=\"/src/cxml\" class=\"btn btn-small\">Src (cxml)</a>\n");
		w.append("<a href=\"/src/dot\" class=\"btn btn-small\">Src (dot)</a>\n");
		w.append(END_DIV);
		w.append("<div class=\"btn-group\">\n");
		w.append("<a href=\"/dst/xml\" class=\"btn btn-small\">Dst (xml)</a>\n");
		w.append("<a href=\"/dst/cxml\" class=\"btn btn-small\">Dst (cxml)</a>\n");
		w.append("<a href=\"/dst/dot\" class=\"btn btn-small\">Dst (dot)</a>\n");
		w.append(END_DIV);
		w.append("<div class=\"btn-group\">\n");
		w.append("<a href=\"/script\" class=\"btn btn-small\">Script</a>\n");
		w.append(END_DIV);
		w.append("<div class=\"btn-group\">\n");
		w.append("<a href=\"/quit\" class=\"btn btn-small btn-danger\">Quit</a>\n");
		w.append(END_DIV);
		w.append(END_DIV);
		w.append(END_DIV);
		w.append(END_DIV);
		w.append("<div class=\"row-fluid\">\n");
	}

	private static void appendFooter(Writer w) throws IOException {
		w.append(END_DIV);
		w.append(END_DIV);
		w.append("<script src=\"assets/jquery.min.js\"></script>\n");
		w.append("<script src=\"assets/bootstrap.min.js\"></script>\n");
		w.append("<script src=\"assets/gumtree.js\"></script>\n");
		w.append("</body>\n");
		w.append("</html>\n");
	}
	
	private static String getFileName(String path) {
		String name = new File(path).getName();
		if (name.length() <= 55) return name;
		else return name.substring(0,10) + "..." + name.substring(name.length() - 42);
	}
	
}
