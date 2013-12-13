package fr.labri.gumtree.gen.antlr;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.Parser;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;

import fr.labri.gumtree.io.TreeGenerator;
import fr.labri.gumtree.tree.Tree;

public abstract class AbstractAntlrTreeGenerator extends TreeGenerator {
	
	protected Map<CommonTree, Tree> trees;

	protected static Map<Integer, String> names;

	protected static Map<Integer, Integer> chars;

	protected CommonTokenStream tokens;
	
	public AbstractAntlrTreeGenerator() {
		loadNames();
	}
	
	protected abstract CommonTree getStartSymbol(String file) throws RecognitionException, IOException;
	
	@Override
	public Tree generate(String file) throws IOException {
		try {
			loadChars(file);
			CommonTree ct = getStartSymbol(file);
			trees = new HashMap<CommonTree, Tree>();
			return toTree(ct);
		} catch (RecognitionException e) {
			System.out.println("at " + e.line + ":" + e.charPositionInLine);
			e.printStackTrace();
		}
		return null;
	}
	
	protected abstract Parser getEmptyParser();
	
	protected Tree toTree(CommonTree ct) {
		Tree t = null;
		
		if (ct.getText().equals(names.get(ct.getType())))
			t = new Tree(ct.getType());
		else
			t = new Tree(ct.getType(), ct.getText());
		
		t.setTypeLabel(names.get(ct.getType()));
		
		int[] pos = getPosAndLength(ct);
		t.setPos(pos[0]);
		t.setLength(pos[1]);
		
		if (ct.getParent() != null )
			t.setParentAndUpdateChildren(trees.get(ct.getParent()));
		
		if (ct.getChildCount() > 0) { 
			trees.put(ct, t);
			for (CommonTree cct : (List<CommonTree>) ct.getChildren()) toTree(cct);
		}
		
		return t;
	}

	
	private void loadNames() {
		names = new HashMap<Integer, String>();
		Parser p = getEmptyParser();
		for (Field f : p.getClass().getFields()) {
			if (f.getType().equals(int.class)) {
				try {
					names.put(f.getInt(p), f.getName());
				} catch (Exception e) {
					e.printStackTrace();
				} 
			}
		}
	}
	
	private void loadChars(String file) throws IOException {
		chars = new HashMap<Integer, Integer>();
		BufferedReader r = new BufferedReader(new FileReader(file));
		int line = 0;
		int chrs = 0;
		while (r.ready()) {
			String cur = r.readLine();
			chrs += cur.length() + 1;
			chars.put(line, chrs);
			line++;
		}
		r.close();
	}
	
	@SuppressWarnings("serial")
	private int[] getPosAndLength(CommonTree ct) {
		//if (ct.getTokenStartIndex() == -1 || ct.getTokenStopIndex() == -1) System.out.println("yoooo" + ct.toStringTree());
		int start = (ct.getTokenStartIndex() == -1) ? 0 : new CommonToken(tokens.get(ct.getTokenStartIndex())) { int getPos() { return start; } } .getPos();
		int stop = (ct.getTokenStopIndex() == -1) ? 0 : new CommonToken(tokens.get(ct.getTokenStopIndex())) { int getPos() { return stop; } } .getPos();
		return new int[] { start, stop - start + 1 };
	}


}
