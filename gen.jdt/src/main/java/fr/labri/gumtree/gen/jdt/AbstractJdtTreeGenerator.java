package fr.labri.gumtree.gen.jdt;

import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;

import fr.labri.gumtree.io.TreeGenerator;
import fr.labri.gumtree.tree.Tree;

public abstract class AbstractJdtTreeGenerator extends TreeGenerator {
	
	public Tree generate(String file) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		configureParser(parser);
		Requestor req = new Requestor(createVisitor());
		parser.createASTs(new String[] { file }, null, new String[] {}, req, null);
		return req.getVisitor().getTree();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void configureParser(ASTParser parser) {
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		Map pOptions = JavaCore.getOptions();
		pOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
		pOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
		pOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
		parser.setCompilerOptions(pOptions);
	}
	
	@Override
	public Tree generateFromString(String contents) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource(contents.toCharArray());
		configureParser(parser);
		ASTNode astNode = parser.createAST(null);
		AbstractJdtVisitor visitor = createVisitor();
		astNode.accept(visitor);
		return visitor.getTree();
	}

	@Override
	public boolean handleFile(String file) {
		return file.endsWith(".java");
	}
	
	protected abstract AbstractJdtVisitor createVisitor();

}
