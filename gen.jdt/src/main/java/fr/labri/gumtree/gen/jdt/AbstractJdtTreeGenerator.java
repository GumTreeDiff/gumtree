package fr.labri.gumtree.gen.jdt;

import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;

import fr.labri.gumtree.io.TreeGenerator;
import fr.labri.gumtree.tree.Tree;

public abstract class AbstractJdtTreeGenerator extends TreeGenerator {
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Tree generate(String file) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		Map pOptions = JavaCore.getOptions();
		pOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
		pOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
		pOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
		parser.setCompilerOptions(pOptions);
		Requestor req = new Requestor(createVisitor());
		parser.createASTs(new String[] { file }, null, new String[] {}, req, null);
		return req.getVisitor().getTree();
	}

	@Override
	public boolean handleFile(String file) {
		return file.endsWith(".java");
	}
	
	protected abstract AbstractJdtVisitor createVisitor();

}