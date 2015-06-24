package com.github.gumtreediff.gen.jdt.cd;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.jdt.AbstractJdtVisitor;
import com.github.gumtreediff.gen.jdt.AbstractJdtTreeGenerator;

@Register(id = "java-jdt-cd")
public class CdJdtTreeGenerator extends AbstractJdtTreeGenerator {
    @Override
    protected AbstractJdtVisitor createVisitor() {
        return new CdJdtVisitor();
    }
}
