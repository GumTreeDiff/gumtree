package fr.labri.gumtree.gen.jdt;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;

public class Requestor extends FileASTRequestor {
	
	private AbstractJdtVisitor visitor;
	
	public Requestor(AbstractJdtVisitor visitor) {
		super();
		this.visitor = visitor; 
	}
	
	public void acceptAST(String sourceFilePath, CompilationUnit ast) {
		ast.accept(visitor);
	}
	
	public AbstractJdtVisitor getVisitor() {
		return visitor;
	}


}
