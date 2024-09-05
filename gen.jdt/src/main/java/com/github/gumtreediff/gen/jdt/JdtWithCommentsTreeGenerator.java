/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.gen.jdt;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Registry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;





/* Created by pourya on 2024-09-05*/
@Register(id = "java-jdtc", accept = "\\.java$", priority = Registry.Priority.MAXIMUM)
public class JdtWithCommentsTreeGenerator extends AbstractJdtTreeGenerator {
    @Override
    protected AbstractJdtVisitor createVisitor(IScanner scanner) {
        return new JdtVisitor(scanner);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public TreeContext generate(Reader r) throws IOException {
        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        Map pOptions = JavaCore.getOptions();
        pOptions.put(JavaCore.COMPILER_COMPLIANCE, JAVA_VERSION);
        pOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JAVA_VERSION);
        pOptions.put(JavaCore.COMPILER_SOURCE, JAVA_VERSION);
        pOptions.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
        parser.setCompilerOptions(pOptions);
        char[] source = readerToCharArray(r);
        parser.setSource(source);
        IScanner scanner = ToolFactory.createScanner(false, false, false, false);
        scanner.setSource(source);
        AbstractJdtVisitor v = createVisitor(scanner);
        ASTNode node = parser.createAST(null);
        if ((node.getFlags() & ASTNode.MALFORMED) != 0) // bitwise flag to check if the node has a syntax error
            throw new SyntaxException(this, r, null);
        node.accept(v);
        if (node instanceof CompilationUnit)
        {
            List commentList = ((CompilationUnit) node).getCommentList();
            for (Object o : commentList) {
                ASTNode comment = (ASTNode) o;
                comment.accept(new JdtCommentVisitor(scanner, v.getTreeContext()));
            }
        }
        return v.getTreeContext();
    }
}


