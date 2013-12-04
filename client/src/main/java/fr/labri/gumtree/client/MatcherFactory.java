package fr.labri.gumtree.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import fr.labri.gumtree.matchers.composite.GumTreeMatcher;
import fr.labri.gumtree.matchers.composite.Matcher;
import fr.labri.gumtree.tree.Tree;

public class MatcherFactory {
	
	@SuppressWarnings("unchecked")
	public static Matcher createMatcher(Tree src, Tree dst, String name) {
		if (name != null) {
			try {
				Class<? extends Matcher> clazz = (Class<? extends Matcher>) Class.forName(name);
				Constructor<? extends Matcher> constructor = clazz.getConstructor(Tree.class, Tree.class);
				Matcher m = constructor.newInstance(src, dst);
				return m;
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		return new GumTreeMatcher(src, dst);
	}
	
	public static Matcher createMatcher(Tree src, Tree dst) {
		return createMatcher(src, dst, null);
	}

}
