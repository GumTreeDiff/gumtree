package gen.jdt;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.junit.Test;

import fr.labri.gumtree.gen.jdt.JdtTreeGenerator;
import fr.labri.gumtree.tree.Tree;

public class Java8Test {
	
	private String input = "public class A{public void m(){new ArrayList<Object>().stream().forEach(a -> {});}}";

	@Test
	public void testJava8Syntax() throws IOException {
		Path path = Files.createTempFile("", ".java");
		Files.write(path, input.getBytes(), StandardOpenOption.WRITE);
		Tree tree = new JdtTreeGenerator().fromFile(path.toAbsolutePath().toString());
		assertEquals(24, tree.getSize());
		path.toFile().delete();
	}
}
