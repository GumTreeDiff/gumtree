package fr.labri.gumtree.gen.jdt.cd;

import fr.labri.gumtree.gen.jdt.AbstractJdtVisitor;
import fr.labri.gumtree.gen.jdt.AbstractJdtTreeGenerator;

public class CdJdtTreeGenerator extends AbstractJdtTreeGenerator {

	@Override
	public String getName() {
		return "java-jdt-cd";
	}

	@Override
	protected AbstractJdtVisitor createVisitor() {
		return new CdJdtVisitor();
	}

}
