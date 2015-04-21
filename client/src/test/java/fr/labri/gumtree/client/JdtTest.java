package fr.labri.gumtree.client;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.labri.gumtree.actions.ActionGenerator;
import fr.labri.gumtree.actions.model.Action;
import fr.labri.gumtree.gen.jdt.JdtTreeGenerator;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.MatcherFactories;
import fr.labri.gumtree.tree.Tree;

public class JdtTest {
	
	private Path file1;
	private Path file2;

	@Before
	public void before() throws IOException {
		file1 = Files.createTempFile("", ".java");
		file2 = Files.createTempFile("", ".java");
	}
	
	@After
	public void after() {
		file1.toFile().delete();
		file2.toFile().delete();
	}
	
	private List<Action> getActions(String string1, String string2)
			throws IOException {
		Files.write(file1, string1.getBytes(), StandardOpenOption.WRITE);
		Files.write(file2, string2.getBytes(), StandardOpenOption.WRITE);
		
		return generateActions(file1, file2);
	}
	
	private List<Action> generateActions(Path file1, Path file2) throws IOException {
		JdtTreeGenerator treeGenerator = new JdtTreeGenerator();
		Tree tree1 = treeGenerator.fromFile(file1.toAbsolutePath().toString());
		Tree tree2 = treeGenerator.fromFile(file2.toAbsolutePath().toString());
		
		Matcher matcher = MatcherFactories.newMatcher(tree1, tree2);
		matcher.match();
		
		ActionGenerator generator = new ActionGenerator(tree1, tree2, matcher.getMappings());
		generator.generate();
		return generator.getActions();
	}

	@Test
	public void testEmptyFiles() throws IOException {
		List<Action> actions = getActions("", "");
		
		assertEquals(0, actions.size());
	}
	
	@Test
	public void testOneEmptyFile() throws IOException {
		String emptyClass = "public class A{}";
		List<Action> actions = getActions(emptyClass, "");
		
		assertEquals(3, actions.size());
	}
	
	@Test
	public void testGarbagee() throws IOException {
		List<Action> actions = getActions("bklafdfdsafduh43b", "");
		assertEquals(0, actions.size());
	}
}
