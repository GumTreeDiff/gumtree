package fr.labri.gumtree.gen.jdt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;

import fr.labri.gumtree.io.TreeGenerator;
import fr.labri.gumtree.tree.TreeContext;

public abstract class AbstractJdtTreeGenerator extends TreeGenerator {
	
	private static char[] readerToCharArray(Reader r) throws IOException {
		StringBuilder fileData = new StringBuilder(1000);
		BufferedReader br = new BufferedReader(r);
 
		char[] buf = new char[10];
		int numRead = 0;
		while ((numRead = br.read(buf)) != -1) {
			System.out.println(numRead);
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
		br.close();
 
		return  fileData.toString().toCharArray();	
	}
	
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public TreeContext generate(Reader r) throws IOException {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		Map pOptions = JavaCore.getOptions();
		pOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
		pOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
		pOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
		parser.setCompilerOptions(pOptions);
		parser.setSource(readerToCharArray(r));
		AbstractJdtVisitor v = createVisitor();
		parser.createAST(null).accept(v);
		return v.getTreeContext();
	}

	@Override
	public boolean handleFile(String file) {
		return file.endsWith(".java");
	}
	
	protected abstract AbstractJdtVisitor createVisitor();

}
