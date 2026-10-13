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
 * along with GumTree. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2026 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */
package com.github.gumtreediff.gen.kotlin;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TypeSet;
import com.github.gumtreediff.utils.Registry;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory;
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.idea.KotlinLanguage;

import java.io.IOException;
import java.io.Reader;
import java.util.Set;

@Register(id = "kotlin-psi", accept = "\\.kt$", priority = Registry.Priority.MAXIMUM)
public final class KotlinPsiTreeGenerator extends TreeGenerator {
    private static final Set<String> ASSIGNMENT_OPERATOR_TOKENS = Set.of(
            "EQ", "PLUSEQ", "MINUSEQ", "MULTEQ", "DIVEQ", "PERCEQ",
            "ANDANDEQ", "OREQ", "XOREQ", "SHLEQ", "SHREQ", "USHREQ");

    @Override
    protected TreeContext generate(Reader reader) throws IOException {
        StringBuilder content = new StringBuilder();
        char[] buffer = new char[8192];
        int count;
        while ((count = reader.read(buffer)) != -1) {
            content.append(buffer, 0, count);
        }

        var disposable = Disposer.newDisposable();
        try {
            KotlinCoreEnvironment environment = KotlinCoreEnvironment.createForProduction(
                    disposable, new CompilerConfiguration(), EnvironmentConfigFiles.JVM_CONFIG_FILES);
            PsiFile psiFile = PsiFileFactory.getInstance(environment.getProject())
                    .createFileFromText("file.kt", KotlinLanguage.INSTANCE, content.toString());
            TreeContext context = new TreeContext();
            context.setRoot(convert(psiFile, context));
            return context;
        }
        finally {
            Disposer.dispose(disposable);
        }
    }

    private static Tree convert(PsiElement element, TreeContext context) {
        PsiElement firstChild = element.getFirstChild();
        String type;
        if (element instanceof PsiErrorElement) {
            type = "ERROR";
        }
        else if (firstChild == null) {
            type = element.getNode().getElementType().toString();
            if (ASSIGNMENT_OPERATOR_TOKENS.contains(type)) {
                type = "ASSIGNMENT_OPERATOR";
            }
        }
        else {
            type = element.getClass().getSimpleName();
        }

        Tree tree = context.createTree(TypeSet.type(type), firstChild == null ? element.getText() : "");
        TextRange textRange = element.getTextRange();
        tree.setPos(textRange.getStartOffset());
        tree.setLength(textRange.getLength());

        for (PsiElement child = firstChild; child != null; child = child.getNextSibling()) {
            if (!(child instanceof PsiWhiteSpace)) {
                tree.addChild(convert(child, context));
            }
        }
        return tree;
    }
}
