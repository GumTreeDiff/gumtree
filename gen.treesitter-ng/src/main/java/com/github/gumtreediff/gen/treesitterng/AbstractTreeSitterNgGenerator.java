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
 * Copyright 2021 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */
package com.github.gumtreediff.gen.treesitterng;

import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TypeSet;
import com.github.gumtreediff.utils.Pair;
import org.treesitter.*;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.*;

public abstract class AbstractTreeSitterNgGenerator extends TreeGenerator {

    private static final String RULES_FILE = "rules.yml";

    private static final String YAML_IGNORED = "ignored";
    private static final String YAML_LABEL_IGNORED = "label_ignored";
    private static final String YAML_FLATTENED = "flattened";
    private static final String YAML_ALIASED = "aliased";

    private static final Map<String, Map<String, Object>> RULES;

    static {
        Yaml yaml = new Yaml();
        RULES = yaml.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(RULES_FILE));
    }

    @Override
    protected TreeContext generate(Reader r) {
        TSParser parser = new TSParser();
        TSLanguage language = getTreeSitterLanguage();
        parser.setLanguage(language);
        BufferedReader bufferedReader = new BufferedReader(r);
        List<String> contentLines = bufferedReader.lines().toList();
        String content = String.join(System.lineSeparator(), contentLines);
        TSTree tree = parser.parseString(null, content);
        Map<String, Object> currentRule = RULES.getOrDefault(getLanguageName(), new HashMap<>());
        return generateFromTreeSitterTree(contentLines, currentRule, tree);
    }

    private static String getLabel(List<String> contentLines, TSNode node) {
        int startRow = node.getStartPoint().getRow();
        int startColumn = node.getStartPoint().getColumn();
        int endRow = node.getEndPoint().getRow();
        int endColumn = node.getEndPoint().getColumn();
        List<String> substringLines;
        // tree-sitter handles string by byte array, so we need this.
        String startRowStr = contentLines.get(startRow);
        byte[] startRowBytes = startRowStr.getBytes();
        if (startRow == endRow) {
            // endColumn == startRowBytes.length + 1 when the label in tree-sitter contains line separator
            if (endColumn == startRowBytes.length + 1) {
                substringLines = Collections.singletonList(startRowStr);
            }
            else {
                substringLines = Collections.singletonList(new String(
                        startRowBytes, startColumn, endColumn - startColumn));
            }
        }
        else {
            substringLines = new ArrayList<>();
            String endRowStr = contentLines.get(endRow);
            byte[] endRowBytes = endRowStr.getBytes();
            String startLineSubstring;
            if (startColumn > startRowBytes.length) {
                // usually, line separator is not the start char of a tree node label
                // if this situation happened, just put an empty string at start
                startLineSubstring = "";
            } else {
                startLineSubstring = new String(startRowBytes, 0, startColumn);
            }
            List<String> middleLines = contentLines.subList(startRow + 1, endRow);
            String endLineSubstring;
            if (endColumn > endRowStr.length()) {
                endLineSubstring = endRowStr;
            }
            else {
                endLineSubstring = new String(endRowBytes, 0, endColumn);
            }
            substringLines.add(startLineSubstring);
            substringLines.addAll(middleLines);
            substringLines.add(endLineSubstring);
        }
        return String.join(System.lineSeparator(), substringLines);
    }

    private static int calculateOffset(List<String> contentLines, TSPoint point) {
        int startRow = point.getRow();
        int startColumn = point.getColumn();
        int offset = 0;
        for (int i = 0; i < startRow; i++) {
            offset += contentLines.get(i).length() + System.lineSeparator().length();
        }
        offset += startColumn;
        return offset;
    }

    /**
     * try match node's type or node and its ancestors' types in given ruleSet.
     *
     * @param ruleSet a rule's list or rule's ketSet if the rule is a map.
     * @param node the node to match.
     * @return matched types. null if not matched.
     */
    protected static String matchNodeOrAncestorTypes(Collection<String> ruleSet, TSNode node) {
        int depth = 1;
        String nodeType = node.getType();
        StringBuilder typesBuilder = new StringBuilder(nodeType);
        TSNode ancestor = node.getParent();
        int maxDepthInRuleSet = 1;
        for (String rule : ruleSet) {
            String[] ruleTypes = rule.split(" ");
            if (ruleTypes.length > maxDepthInRuleSet) {
                maxDepthInRuleSet = ruleTypes.length;
            }
            if (ruleTypes.length == 1 && rule.equals(nodeType)) {
                // matched directly
                return nodeType;
            }
        }
        while (!ancestor.isNull() && depth <= maxDepthInRuleSet) {
            typesBuilder.insert(0, ' ');
            typesBuilder.insert(0, ancestor.getType());
            String typesString = typesBuilder.toString();
            if (ruleSet.contains(typesString)) {
                return typesString;
            }
            ancestor = ancestor.getParent();
            depth++;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected static Pair<Tree, Boolean> tsNode2GumTree(
            List<String> contentLines, Map<String, Object> currentRule, TreeContext context, TSNode node) {
        String type = node.getType();
        if (currentRule.containsKey(YAML_IGNORED)) {
            List<String> ignores = (List<String>) currentRule.get(YAML_IGNORED);
            if (matchNodeOrAncestorTypes(ignores, node) != null) {
                return null;
            }
        }
        boolean ignoreLabel = false;
        if (currentRule.containsKey(YAML_LABEL_IGNORED)) {
            List<String> ignores = (List<String>) currentRule.get(YAML_LABEL_IGNORED);
            if (matchNodeOrAncestorTypes(ignores, node) != null) {
                ignoreLabel = true;
            }
        }
        if (currentRule.containsKey(YAML_ALIASED)) {
            Map<String, String> alias = (Map<String, String>) currentRule.get(YAML_ALIASED);
            String matchedTypes = matchNodeOrAncestorTypes(alias.keySet(), node);
            if (matchedTypes != null) {
                type = alias.get(matchedTypes);
            }
        }
        boolean flatten = false;
        if (currentRule.containsKey(YAML_FLATTENED)) {
            List<String> flattenList = (List<String>) currentRule.get(YAML_FLATTENED);
            if (matchNodeOrAncestorTypes(flattenList, node) != null) {
                flatten = true;
            }
        }
        Tree tree;
        // attach label for non ignore-label leafs or flattened nodes
        if ((node.getChildCount() == 0 && !ignoreLabel) || flatten) {
            String label = getLabel(contentLines, node);
            tree = context.createTree(TypeSet.type(type), label);
        }
        else {
            tree = context.createTree(TypeSet.type(type));
        }
        tree.setPos(calculateOffset(contentLines, node.getStartPoint()));
        int endOffset = calculateOffset(contentLines, node.getEndPoint());
        tree.setLength(endOffset - tree.getPos());
        return new Pair<>(tree, flatten);
    }

    private static TreeContext generateFromTreeSitterTree(
            List<String> contentLines, Map<String, Object> currentRule, TSTree tree) {
        TSNode rootNode = tree.getRootNode();
        TreeContext context = new TreeContext();
        Pair<Tree, Boolean> rootPair = tsNode2GumTree(contentLines, currentRule, context, rootNode);
        if (rootPair == null) {
            return context;
        }
        context.setRoot(rootPair.first);
        Stack<TSNode> tsNodeStack = new Stack<>();
        Stack<Pair<Tree, Boolean>> treeStack = new Stack<>();
        tsNodeStack.push(rootNode);
        treeStack.push(rootPair);
        while (!tsNodeStack.isEmpty()) {
            TSNode tsNodeNow = tsNodeStack.pop();
            Pair<Tree, Boolean> treeNow = treeStack.pop();
            // if node was flattened, ignore children
            if (treeNow.second) {
                continue;
            }
            int childCount = tsNodeNow.getChildCount();
            for (int i = 0; i < childCount; i++) {
                TSNode child = tsNodeNow.getChild(i);
                Pair<Tree, Boolean> childTree = tsNode2GumTree(contentLines, currentRule, context, child);
                if (childTree != null) {
                    treeNow.first.addChild(childTree.first);
                    tsNodeStack.push(child);
                    treeStack.push(childTree);
                }
            }
        }
        return context;
    }

    protected abstract TSLanguage getTreeSitterLanguage();

    protected abstract String getLanguageName();
}
