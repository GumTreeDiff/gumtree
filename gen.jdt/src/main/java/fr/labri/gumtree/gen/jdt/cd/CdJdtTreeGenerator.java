package fr.labri.gumtree.gen.jdt.cd;

import fr.labri.gumtree.gen.jdt.AbstractJdtVisitor;
import fr.labri.gumtree.gen.jdt.AbstractJdtTreeProducer;

public class CdJdtTreeGenerator extends AbstractJdtTreeProducer {

	@Override
	public String getName() {
		return "java-jdt-cd";
	}

	@Override
	protected AbstractJdtVisitor createVisitor() {
		return new CdJdtVisitor();
	}

}
