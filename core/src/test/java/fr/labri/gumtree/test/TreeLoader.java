package fr.labri.gumtree.test;

import fr.labri.gumtree.io.TreeIoUtils;
import fr.labri.gumtree.tree.ITree;
import fr.labri.gumtree.tree.Pair;

public class TreeLoader {

	private TreeLoader() {};
	
	public static Pair<ITree, ITree> getActionExample() {
		return new Pair<ITree, ITree>(load("/action_v0.xml"), load("/action_v1.xml"));
	}
	
	public static Pair<ITree, ITree> getZsCustomExample() {
		return new Pair<ITree, ITree>(load("/zs_v0.xml"), load("/zs_v1.xml"));
	}
	
	public static Pair<ITree, ITree> getZsSlideExample() {
		return new Pair<ITree, ITree>(load("/zs_slide_v0.xml"), load("/zs_slide_v1.xml"));
	}
	
	public static Pair<ITree, ITree> getDummyExample() {
		return new Pair<ITree, ITree>(load("/Dummy_v0.xml"), load("/Dummy_v1.xml"));
	} 
	
	public static ITree load(String name) {		
		return TreeIoUtils.fromXml(System.class.getResourceAsStream(name)).getRoot();
	}
}
