package fr.labri.gumtree.matchers;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import fr.labri.gumtree.tree.ITree;

public class MatcherFactories {
	
	private static Map<Class<? extends MatcherFactory>, MatcherFactory> factories;
	
	static {
		factories = new HashMap<>();
		addFactory(new CompositeMatchers.GumTreeMatcherFactory());
		addFactory(new CompositeMatchers.ChangeDistillerMatcherFactory());
		addFactory(new CompositeMatchers.XyMatcherFactory());
	}
	
	public static MatcherFactory getFactory(Class<? extends MatcherFactory> clazz) {
		if (!factories.containsKey(clazz))
			try {
				factories.put(clazz, clazz.getConstructor().newInstance());
			} catch (InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
		return factories.get(clazz);
	}
	
	@SuppressWarnings("unchecked")
	public static MatcherFactory getFactory(String clazz) {
		try {
			return getFactory((Class<? extends MatcherFactory>) Class.forName(clazz));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} 
		return null;
	}
	
	public static MatcherFactory getDefaultMatcherFactory() {
		return factories.get(CompositeMatchers.GumTreeMatcherFactory.class);
	}
	
	public static Matcher newMatcher(ITree src, ITree dst, Class<? extends MatcherFactory> clazz) {
		return getFactory(clazz).newMatcher(src, dst);
	}
	
	public static Matcher newMatcher(ITree src, ITree dst, String clazz) {
		return getFactory(clazz).newMatcher(src, dst);
	}

	public static Matcher newMatcher(ITree src, ITree dst) {
		return getDefaultMatcherFactory().newMatcher(src, dst);
	}

	private static void addFactory(MatcherFactory factory) {
		factories.put(factory.getClass(), factory);
	}
	
}
