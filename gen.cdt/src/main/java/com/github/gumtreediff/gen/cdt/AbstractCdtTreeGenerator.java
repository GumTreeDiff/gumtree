/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2026 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */
package com.github.gumtreediff.gen.cdt;

import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TypeSet;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.parser.AbstractCLikeLanguage;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.core.runtime.CoreException;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Collections;

abstract class AbstractCdtTreeGenerator extends TreeGenerator {
    private final AbstractCLikeLanguage language;
    private final String fileName;

    AbstractCdtTreeGenerator(AbstractCLikeLanguage language, String fileName) {
        this.language = language;
        this.fileName = fileName;
    }

    @Override
    @SuppressWarnings("deprecation")
    protected TreeContext generate(Reader reader) throws IOException {
        StringWriter writer = new StringWriter();
        reader.transferTo(writer);
        String source = writer.toString();
        try {
            IASTTranslationUnit unit = language.getASTTranslationUnit(
                    FileContent.create(fileName, source.toCharArray()),
                    new ScannerInfo(Collections.emptyMap(), new String[0]),
                    IncludeFileContentProvider.getEmptyFilesProvider(),
                    null,
                    ILanguage.OPTION_IS_SOURCE_UNIT,
                    new DefaultLogService());
            TreeContext context = new TreeContext();
            context.setRoot(createTree(context, unit, source, true));
            return context;
        } catch (CoreException e) {
            throw new IOException("Eclipse CDT could not parse " + fileName, e);
        }
    }

    private Tree createTree(TreeContext context, IASTNode node, String source, boolean root) {
        IASTNode[] children = node.getChildren();
        String label = children.length == 0 ? node.getRawSignature() : "";
        if (label == null)
            label = "";
        Tree tree = context.createTree(TypeSet.type(node.getClass().getSimpleName()), label);
        if (root) {
            tree.setPos(0);
            tree.setLength(source.length());
        } else if (node.getFileLocation() != null) {
            tree.setPos(node.getFileLocation().getNodeOffset());
            tree.setLength(node.getFileLocation().getNodeLength());
        } else {
            tree.setPos(0);
            tree.setLength(0);
        }
        for (IASTNode child : children)
            tree.addChild(createTree(context, child, source, false));
        return tree;
    }
}
