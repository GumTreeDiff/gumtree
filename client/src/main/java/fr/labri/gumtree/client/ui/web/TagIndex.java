package fr.labri.gumtree.client.ui.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TagIndex {

	private Map<Integer, List<String>> startTags;

	private Map<Integer, List<String>> endTags;

	public TagIndex() {
		startTags = new HashMap<Integer, List<String>>();
		endTags = new HashMap<Integer, List<String>>();
	}

	public void addTags(int pos, String startTag, int endPos, String endTag) {
		addStartTag(pos, startTag);
		addEndTag(endPos, endTag);
	}

	public void addStartTag(int pos, String tag) {
		if (!startTags.containsKey(pos)) startTags.put(pos, new ArrayList<String>());
		startTags.get(pos).add(tag);
	}

	public void addEndTag(int pos, String tag) {
		if (!endTags.containsKey(pos)) endTags.put(pos, new ArrayList<String>());
		endTags.get(pos).add(tag);
	}

	public String getEndTags(int pos) {
		if (!endTags.containsKey(pos)) return "";
		StringBuffer b = new StringBuffer();
		for (String s: endTags.get(pos)) b.append(s);
		return b.toString();
	}


	public String getStartTags(int pos) {
		if (!startTags.containsKey(pos)) return "";
		StringBuffer b = new StringBuffer();
		for (String s: startTags.get(pos)) b.append(s);
		return b.toString();
	}

}
