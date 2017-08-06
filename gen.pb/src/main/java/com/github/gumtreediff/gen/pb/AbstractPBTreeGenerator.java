package com.github.gumtreediff.gen.pb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.Exception;
import java.io.InputStream;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.io.ActionsIoUtils;

import fast.Fast;
import fast.Fast.Data;
import fast.Fast.Element;

@Register(id = "protobuf", accept = "\\.pb")
public class AbstractPBTreeGenerator extends TreeGenerator {

    private Deque<ITree> trees = new ArrayDeque<>();

    protected static Map<Integer, Integer> chars;

    public AbstractPBTreeGenerator() {
    }

    @Override
    public TreeContext generate(Reader r) throws IOException {
         TreeContext context = new TreeContext();
         return context;
    }

    static int pos = 0;
    static int id = 1;
    @Override
    public TreeContext generateFromFile(String input) throws IOException {
	    // System.out.println("generating from file " + input);
	    fast.Fast.Data data = fast.Fast.Data.parseFrom(new FileInputStream(input));
	    fast.Fast.Element element = data.getElement();
            TreeContext context = new TreeContext();
	    try {
		    id = 1;
		    pos = 0;
		    buildTree(context, element);
	    } catch (Exception e) {
		    e.printStackTrace();
	    }
	    trees.clear();
            return context;
    }

    @SuppressWarnings("unchecked")
    protected void buildTree(TreeContext context, fast.Fast.Element element) throws Exception {
            int type = element.getKindValue();
            String tokenName = element.getKind().toString();
	    String text = element.getText().toStringUtf8();
	    String tail = element.getTail().toStringUtf8();
	    int length = text.length();
	    int start = pos;
	    // System.out.println(tokenName);
            ITree t = context.createTree(type, text, tokenName);
	    pos += length;
            if (trees.isEmpty())
                context.setRoot(t);
            else
                t.setParentAndUpdateChildren(trees.peek());
            if (element.getChildCount() > 0) {
                trees.push(t);
                for (fast.Fast.Element child : element.getChildList())
                    buildTree(context, child);
                trees.pop();
            }
	    pos += tail.length();
            t.setPos(start);
            t.setLength(pos - start); // FIXME check if this + 1 make sense ?
	    // System.out.println("pos = "  + start + " length = " + (pos - start + 1));
	    t.setId(id++);
	    // System.out.println("id = "  + t.getId());
	    ActionsIoUtils.pb_mappings.put(t, element);
    }
}
