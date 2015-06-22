package fr.labri.gumtree.gen.jdt.cd;

import fr.labri.gumtree.gen.Register;
import fr.labri.gumtree.gen.jdt.AbstractJdtVisitor;
import fr.labri.gumtree.gen.jdt.AbstractJdtTreeGenerator;

@Register(id = "java-jdt-cd")
public class CdJdtTreeGenerator extends AbstractJdtTreeGenerator {
    @Override
    protected AbstractJdtVisitor createVisitor() {
        return new CdJdtVisitor();
    }
}
