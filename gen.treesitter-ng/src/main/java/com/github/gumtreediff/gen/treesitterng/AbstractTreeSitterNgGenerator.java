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
import org.treesitter.*;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Converts a tree-sitter tree into a GumTree tree in a single tree cursor traversal.
 * Positions and lengths are UTF-16 offsets (the offsets of Java strings), converted from the UTF-8 byte
 * offsets of tree-sitter, and labels are the source text of the nodes.
 */
public abstract class AbstractTreeSitterNgGenerator extends TreeGenerator {

    private static final String RULES_FILE = "rules.yml";

    private static final String YAML_IGNORED = "ignored";
    private static final String YAML_LABEL_IGNORED = "label_ignored";
    private static final String YAML_FLATTENED = "flattened";
    private static final String YAML_ALIASED = "aliased";

    private static final Map<String, Map<String, Object>> RULES;

    private static final Map<String, LanguageRules> LANGUAGE_RULES = new ConcurrentHashMap<>();

    static {
        Yaml yaml = new Yaml();
        RULES = yaml.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(RULES_FILE));
    }

    @Override
    protected TreeContext generate(Reader r) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[8192];
        int n;
        while ((n = r.read(buf)) != -1) {
            sb.append(buf, 0, n);
        }
        String content = sb.toString();
        if (content.isEmpty())
            return emptyContext();

        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        // one char per byte means that the byte offsets are already char offsets
        int[] charOffsets = bytes.length == content.length() ? null : charOffsets(content, bytes.length);
        LanguageRules rules = LANGUAGE_RULES.computeIfAbsent(getLanguageName(),
                name -> new LanguageRules(RULES.getOrDefault(name, Collections.emptyMap())));
        try (TSParser parser = new TSParser()) {
            parser.setLanguage(getTreeSitterLanguage());
            try (TSTree tree = parse(parser, bytes)) {
                return generateFromTreeSitterTree(content, charOffsets, rules, tree);
            }
        }
    }

    /**
     * Parses the UTF-8 bytes of the content. TSParser.parseString drops the U+FEFF characters of its input,
     * which shifts the byte offsets of all the nodes after them.
     */
    private static TSTree parse(TSParser parser, byte[] bytes) throws IOException {
        byte[] readBuffer = new byte[bytes.length];
        TSTree tree = parser.parse(readBuffer, null, (buffer, offset, position) -> {
            int length = Math.min(buffer.length, bytes.length - offset);
            if (length <= 0)
                return 0;
            System.arraycopy(bytes, offset, buffer, 0, length);
            return length;
        }, TSInputEncoding.TSInputEncodingUTF8);
        if (tree == null)
            throw new IOException("tree-sitter could not parse the content");
        return tree;
    }

    /**
     * Computes the char (UTF-16) offset of each UTF-8 byte offset of the content, as encoded by
     * String.getBytes, which replaces an unpaired surrogate with a single byte.
     */
    private static int[] charOffsets(String content, int byteLength) {
        int[] offsets = new int[byteLength + 1];
        int byteOffset = 0;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            int length;
            if (c < 0x80)
                length = 1;
            else if (c < 0x800)
                length = 2;
            else if (Character.isHighSurrogate(c) && i + 1 < content.length()
                    && Character.isLowSurrogate(content.charAt(i + 1)))
                length = 4;
            else if (Character.isSurrogate(c))
                length = 1;
            else
                length = 3;
            Arrays.fill(offsets, byteOffset, byteOffset + length, i);
            byteOffset += length;
            if (length == 4)
                i++;
        }
        offsets[byteOffset] = content.length();
        return offsets;
    }

    private static int charOffset(int[] charOffsets, int byteOffset) {
        return charOffsets == null ? byteOffset : charOffsets[byteOffset];
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
        // the parent is computed from the root by tree-sitter, so it is only computed when needed
        TSNode ancestor = node.getParent();
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

    /**
     * Traverses the tree-sitter tree with a tree cursor, keeping the types of the ancestors of the current node
     * for the rules, instead of asking tree-sitter for the parent of each node.
     */
    private static TreeContext generateFromTreeSitterTree(
            String content, int[] charOffsets, LanguageRules rules, TSTree tree) {
        TreeContext context = new TreeContext();
        List<String> ancestorTypes = new ArrayList<>();
        Deque<Tree> ancestors = new ArrayDeque<>();
        try (TSTreeCursor cursor = new TSTreeCursor(tree.getRootNode())) {
            while (true) {
                TSNode node = cursor.currentNode();
                String nodeType = node.getType();
                boolean visitChildren = false;
                Tree gumTree = null;
                if (rules.ignored.match(nodeType, ancestorTypes) == null) {
                    boolean ignoreLabel = rules.labelIgnored.match(nodeType, ancestorTypes) != null;
                    String type = nodeType;
                    String aliasedTypes = rules.aliased.match(nodeType, ancestorTypes);
                    if (aliasedTypes != null)
                        type = rules.aliases.get(aliasedTypes);
                    boolean flatten = rules.flattened.match(nodeType, ancestorTypes) != null;
                    int start = charOffset(charOffsets, node.getStartByte());
                    int end = charOffset(charOffsets, node.getEndByte());
                    int childCount = node.getChildCount();
                    // attach label for non ignore-label leafs or flattened nodes
                    if ((childCount == 0 && !ignoreLabel) || flatten)
                        gumTree = context.createTree(TypeSet.type(type), content.substring(start, end));
                    else
                        gumTree = context.createTree(TypeSet.type(type));
                    gumTree.setPos(start);
                    gumTree.setLength(end - start);
                    if (ancestors.isEmpty())
                        context.setRoot(gumTree);
                    else
                        ancestors.peek().addChild(gumTree);
                    // if node was flattened, ignore children
                    visitChildren = !flatten && childCount > 0;
                }
                else if (ancestors.isEmpty()) {
                    // the root is ignored
                    return context;
                }
                if (visitChildren && cursor.gotoFirstChild()) {
                    ancestors.push(gumTree);
                    ancestorTypes.add(nodeType);
                    continue;
                }
                while (!cursor.gotoNextSibling()) {
                    if (ancestors.isEmpty() || !cursor.gotoParent())
                        return context;
                    ancestors.pop();
                    ancestorTypes.remove(ancestorTypes.size() - 1);
                    if (ancestors.isEmpty())
                        return context;
                }
            }
        }
    }

    protected abstract TSLanguage getTreeSitterLanguage();

    protected abstract String getLanguageName();

    private TreeContext emptyContext() {
        TreeContext ctx = new TreeContext();
        Tree root = ctx.createTree(TypeSet.type("empty_root"));
        root.setPos(0);
        root.setLength(0);
        ctx.setRoot(root);
        return ctx;
    }

    /**
     * The rules of a language in rules.yml.
     */
    private static class LanguageRules {
        private final RuleSet ignored;
        private final RuleSet labelIgnored;
        private final RuleSet flattened;
        private final RuleSet aliased;
        private final Map<String, String> aliases;

        @SuppressWarnings("unchecked")
        private LanguageRules(Map<String, Object> rule) {
            ignored = new RuleSet((Collection<String>) rule.get(YAML_IGNORED));
            labelIgnored = new RuleSet((Collection<String>) rule.get(YAML_LABEL_IGNORED));
            flattened = new RuleSet((Collection<String>) rule.get(YAML_FLATTENED));
            aliases = (Map<String, String>) rule.getOrDefault(YAML_ALIASED, Collections.emptyMap());
            aliased = new RuleSet(aliases.keySet());
        }
    }

    /**
     * A set of rules, each one a node type or the space-separated types of ancestors ending with the node type.
     */
    private static class RuleSet {
        private final Set<String> rules;
        private final Set<String> nodeTypeRules = new HashSet<>();
        private int maxDepth = 1;

        private RuleSet(Collection<String> rules) {
            this.rules = rules == null ? Collections.emptySet() : new HashSet<>(rules);
            for (String rule : this.rules) {
                int depth = rule.split(" ").length;
                maxDepth = Math.max(maxDepth, depth);
                if (depth == 1)
                    nodeTypeRules.add(rule);
            }
        }

        /**
         * Same as matchNodeOrAncestorTypes, where the last ancestor type is the type of the parent.
         */
        private String match(String nodeType, List<String> ancestorTypes) {
            if (rules.isEmpty())
                return null;
            if (nodeTypeRules.contains(nodeType))
                return nodeType;
            StringBuilder typesBuilder = new StringBuilder(nodeType);
            for (int depth = 1, i = ancestorTypes.size() - 1; i >= 0 && depth <= maxDepth; depth++, i--) {
                typesBuilder.insert(0, ' ');
                typesBuilder.insert(0, ancestorTypes.get(i));
                String typesString = typesBuilder.toString();
                if (rules.contains(typesString))
                    return typesString;
            }
            return null;
        }
    }
}
