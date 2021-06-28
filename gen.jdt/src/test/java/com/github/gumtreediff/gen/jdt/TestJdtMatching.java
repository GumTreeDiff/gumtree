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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.gumtreediff.actions.ChawatheScriptGenerator;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.TreeInsert;
import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.CompositeMatchers.ClassicGumtree;
import com.github.gumtreediff.matchers.ConfigurableMatcher;
import com.github.gumtreediff.matchers.ConfigurationOptions;
import com.github.gumtreediff.matchers.GumtreeProperties;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.heuristic.gt.GreedySubtreeMatcher;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;

public class TestJdtMatching {

    /**
     * This test exposes the generation of a non expected output from
     * GumTreeClassic, default configuration. The test fails as the output is
     * different to the correct one.
     */
    @Test
    public void testSpurious1WithClassic1_Default_0007d191fec7fe2d6a0c4e87594cb286a553f92c() throws IOException {
        String caseDir = "case_1_0007d191fec7fe2d6a0c4e87594cb286a553f92c_ASTInspector/";
        String pathSource = caseDir + "1_0007d191fec7fe2d6a0c4e87594cb286a553f92c_ASTInspector_s.javaa";
        String pathTarget = caseDir + "1_0007d191fec7fe2d6a0c4e87594cb286a553f92c_ASTInspector_t.javaa";

        URL resourceSource = getClass().getClassLoader().getResource(pathSource);
        URL resourceTarget = getClass().getClassLoader().getResource(pathTarget);

        TreeContext leftContext = new JdtTreeGenerator().generateFrom().file(resourceSource.getFile());
        TreeContext rightContext = new JdtTreeGenerator().generateFrom().file(resourceTarget.getFile());
        assertFalse(leftContext.getRoot().isIsomorphicTo(rightContext.getRoot()));

        Matcher matcher = new CompositeMatchers.ClassicGumtree();
        SimplifiedChawatheScriptGenerator edGenerator = new SimplifiedChawatheScriptGenerator();

        MappingStore mappings = matcher.match(leftContext.getRoot(), rightContext.getRoot());

        EditScript actions = edGenerator.computeActions(mappings);

        List<Action> actionsAll = actions.asList();

        // This assertion exposes the wrong behaviour: the ed size must be 1
        assertTrue(actionsAll.size() > 0);

    }

    /**
     * This test configures GumTreeClassic in order have modifiers not matched (but
     * imports are matched).
     */
    @Test
    public void testSpurious1WithClassic_Configured_1_0007d191fec7fe2d6a0c4e87594cb286a553f92c() throws IOException {

        String caseDir = "case_1_0007d191fec7fe2d6a0c4e87594cb286a553f92c_ASTInspector/";
        String pathSource = caseDir + "1_0007d191fec7fe2d6a0c4e87594cb286a553f92c_ASTInspector_s.javaa";
        String pathTarget = caseDir + "1_0007d191fec7fe2d6a0c4e87594cb286a553f92c_ASTInspector_t.javaa";

        URL resourceSource = getClass().getClassLoader().getResource(pathSource);
        URL resourceTarget = getClass().getClassLoader().getResource(pathTarget);

        TreeContext leftContext = new JdtTreeGenerator().generateFrom().file(resourceSource.getFile());
        TreeContext rightContext = new JdtTreeGenerator().generateFrom().file(resourceTarget.getFile());
        assertFalse(leftContext.getRoot().isIsomorphicTo(rightContext.getRoot()));

        ClassicGumtree matcher = new CompositeMatchers.ClassicGumtree();

        GumtreeProperties properties = new GumtreeProperties();

        properties = new GumtreeProperties();
        // Using min = 1, the imports and package declaration are mapped.
        properties.tryConfigure(ConfigurationOptions.st_minprio, 1);

        Tree typeDeclaration = leftContext.getRoot().getChildren().stream()
                .filter(e -> e.getType().name.equals("TypeDeclaration")).findFirst().get();

        Tree oneImportDeclaration = leftContext.getRoot().getChildren().stream()
                .filter(e -> e.getType().name.equals("ImportDeclaration")).findFirst().get();

        Tree modifier = typeDeclaration.getChildren().stream().filter(e -> e.getType().name.equals("Modifier"))
                .findFirst().get();

        ConfigurableMatcher configurableMatcher = (ConfigurableMatcher) matcher;
        configurableMatcher.configure(properties);

        MappingStore mappings = matcher.match(leftContext.getRoot(), rightContext.getRoot());

        // The import is mapped because use use min = 2
        assertTrue(mappings.isSrcMapped(oneImportDeclaration));

        SimplifiedChawatheScriptGenerator edGenerator = new SimplifiedChawatheScriptGenerator();

        EditScript actions = edGenerator.computeActions(mappings);

        List<Action> actionsAll = actions.asList();

        // This assertion exposes the wrong behaviour: the node must be mapped
        assertFalse(mappings.isSrcMapped(modifier));

        // This assertion exposes the wrong behaviour: the ed size must be 1
        assertTrue(actionsAll.size() > 1);

    }

    /**
     * This test configures GumTreeClassic in order to produce the expected output.
     */
    @Test
    public void testSpurious1WithClassic_Configured4Passing_1_0007d191fec7fe2d6a0c4e87594cb286a553f92c()
            throws IOException {

        String caseDir = "case_1_0007d191fec7fe2d6a0c4e87594cb286a553f92c_ASTInspector/";
        String pathSource = caseDir + "1_0007d191fec7fe2d6a0c4e87594cb286a553f92c_ASTInspector_s.javaa";
        String pathTarget = caseDir + "1_0007d191fec7fe2d6a0c4e87594cb286a553f92c_ASTInspector_t.javaa";

        URL resourceSource = getClass().getClassLoader().getResource(pathSource);
        URL resourceTarget = getClass().getClassLoader().getResource(pathTarget);
        TreeContext leftContext = new JdtTreeGenerator().generateFrom().file(resourceSource.getFile());
        TreeContext rightContext = new JdtTreeGenerator().generateFrom().file(resourceTarget.getFile());
        assertFalse(leftContext.getRoot().isIsomorphicTo(rightContext.getRoot()));

        GumtreeProperties properties = new GumtreeProperties();
        // Using min = 1, the imports and package declaration are mapped.
        properties.tryConfigure(ConfigurationOptions.st_minprio, 1);
        properties.tryConfigure(ConfigurationOptions.bu_minsize, 3000);

        Tree typeDeclaration = leftContext.getRoot().getChildren().stream()
                .filter(e -> e.getType().name.equals("TypeDeclaration")).findFirst().get();

        Tree oneImportDeclaration = leftContext.getRoot().getChildren().stream()
                .filter(e -> e.getType().name.equals("ImportDeclaration")).findFirst().get();

        Tree modifier = typeDeclaration.getChildren().stream().filter(e -> e.getType().name.equals("Modifier"))
                .findFirst().get();

        ClassicGumtree matcher = new CompositeMatchers.ClassicGumtree();
        ConfigurableMatcher configurableMatcher = (ConfigurableMatcher) matcher;
        configurableMatcher.configure(properties);

        MappingStore mappings = matcher.match(leftContext.getRoot(), rightContext.getRoot());

        // The import is mapped because use use min = 1
        assertTrue(mappings.isSrcMapped(oneImportDeclaration));

        SimplifiedChawatheScriptGenerator edGenerator = new SimplifiedChawatheScriptGenerator();
        EditScript actions = edGenerator.computeActions(mappings);

        List<Action> actionsAll = actions.asList();

        // This assertion exposes the wrong behaviour: the node must be mapped
        assertTrue(mappings.isSrcMapped(modifier));

        // This assertion exposes the wrong behaviour: the ed size must be 1
        assertEquals(1, actionsAll.size());

        assertEquals("insert-tree", actionsAll.get(0).getName());

    }

    /**
     * This test executes SimpleGT, which works fine.
     */
    @Test
    public void testSpurious1WithSimple_0007d191fec7fe2d6a0c4e87594cb286a553f92c() throws IOException {

        String caseDir = "case_1_0007d191fec7fe2d6a0c4e87594cb286a553f92c_ASTInspector/";
        String pathSource = caseDir + "1_0007d191fec7fe2d6a0c4e87594cb286a553f92c_ASTInspector_s.javaa";
        String pathTarget = caseDir + "1_0007d191fec7fe2d6a0c4e87594cb286a553f92c_ASTInspector_t.javaa";

        URL resourceSource = getClass().getClassLoader().getResource(pathSource);
        URL resourceTarget = getClass().getClassLoader().getResource(pathTarget);

        TreeContext leftContext = new JdtTreeGenerator().generateFrom().file(resourceSource.getFile());
        TreeContext rightContext = new JdtTreeGenerator().generateFrom().file(resourceTarget.getFile());
        assertFalse(leftContext.getRoot().isIsomorphicTo(rightContext.getRoot()));

        Matcher matcher = new CompositeMatchers.SimpleGumtree();
        SimplifiedChawatheScriptGenerator edGenerator = new SimplifiedChawatheScriptGenerator();

        MappingStore mappings = matcher.match(leftContext.getRoot(), rightContext.getRoot());

        EditScript actions = edGenerator.computeActions(mappings);

        List<Action> actionsAll = actions.asList();

        assertEquals(1, actionsAll.size());

        assertEquals("insert-tree", actionsAll.get(0).getName());

    }

    /**
     * This test exposes the incorrect output of GTClassic, default configuration.
     */
    @Test
    public void testNotSpurious1() throws IOException {

        URL resourceSource = getClass().getClassLoader().getResource("case_1_with_spurious/ClassA_s.javaa");
        URL resourceTarget = getClass().getClassLoader().getResource("case_1_with_spurious/ClassA_t.javaa");

        TreeContext leftContext = new JdtTreeGenerator().generateFrom().file(resourceSource.getFile());
        TreeContext rightContext = new JdtTreeGenerator().generateFrom().file(resourceTarget.getFile());
        assertFalse(leftContext.getRoot().isIsomorphicTo(rightContext.getRoot()));

        CompositeMatchers.ClassicGumtree matcher = new CompositeMatchers.ClassicGumtree();
        ChawatheScriptGenerator edGenerator = new ChawatheScriptGenerator();

        MappingStore mappings = matcher.match(leftContext.getRoot(), rightContext.getRoot());

        EditScript actions = edGenerator.computeActions(mappings);

        List<Action> actionsAll = actions.asList();

        assertEquals(1, actionsAll.size());
        assertEquals("update-node", actionsAll.get(0).getName());

    }

    /**
     * This test executes GTSimple, which works fine.
     */
    @Test
    public void testSpurious1WithSimple() throws IOException {

        URL resourceSource = getClass().getClassLoader().getResource("case_1_with_spurious/ClassA_s.javaa");
        URL resourceTarget = getClass().getClassLoader().getResource("case_1_with_spurious/ClassA_t.javaa");

        TreeContext leftContext = new JdtTreeGenerator().generateFrom().file(resourceSource.getFile());
        TreeContext rightContext = new JdtTreeGenerator().generateFrom().file(resourceTarget.getFile());
        assertFalse(leftContext.getRoot().isIsomorphicTo(rightContext.getRoot()));

        Matcher matcher = new CompositeMatchers.SimpleGumtree();
        SimplifiedChawatheScriptGenerator edGenerator = new SimplifiedChawatheScriptGenerator();

        MappingStore mappings = matcher.match(leftContext.getRoot(), rightContext.getRoot());

        EditScript actions = edGenerator.computeActions(mappings);

        List<Action> actionsAll = actions.asList();

        assertEquals(1, actionsAll.size());

        assertEquals("update-node", actionsAll.get(0).getName());

    }

    /**
     * This test checks the node mappings and exposes the unmapped nodes. The test
     * fails and exposes the issue.
     */
    @Test
    public void testSpurious1WithClassicDefault() throws IOException {

        URL resourceSource = getClass().getClassLoader().getResource("case_1_with_spurious/ClassA_s.javaa");
        URL resourceTarget = getClass().getClassLoader().getResource("case_1_with_spurious/ClassA_t.javaa");

        TreeContext leftContext = new JdtTreeGenerator().generateFrom().file(resourceSource.getFile());
        TreeContext rightContext = new JdtTreeGenerator().generateFrom().file(resourceTarget.getFile());
        Tree rootLeft = leftContext.getRoot();
        assertFalse(rootLeft.isIsomorphicTo(rightContext.getRoot()));

        Matcher matcher = new CompositeMatchers.ClassicGumtree();
        SimplifiedChawatheScriptGenerator edGenerator = new SimplifiedChawatheScriptGenerator();

        MappingStore mappings = matcher.match(rootLeft, rightContext.getRoot());

        EditScript actions = edGenerator.computeActions(mappings);

        List<Action> actionsAll = actions.asList();

        // Check size of imports
        assertEquals(2, rootLeft.getChild(1).getMetrics().size);
        assertEquals(2, rootLeft.getChild(2).getMetrics().size);

        // THis is the assertion that fails: it should be one
        assertEquals(1, actionsAll.size());

        assertEquals("update-node", actionsAll.get(0).getName());

        assertEquals(4, rootLeft.getChildren().size());

        assertTrue(mappings.isSrcMapped(rootLeft));
        // TypeDecl
        assertTrue(mappings.isSrcMapped(rootLeft.getChild(3)));
        // ImportDecl
        assertTrue(mappings.isSrcMapped(rootLeft.getChild(2)));
        // ImportDecl
        assertTrue(mappings.isSrcMapped(rootLeft.getChild(1)));
        // PackageDecl
        assertTrue(mappings.isSrcMapped(rootLeft.getChild(0)));

    }

    /**
     * The test configures GTClassic to produce the expected output.
     */
    @Test
    public void testSpurious1WithClassicConfiguredGreedyBottomUpMatcher() throws IOException {

        URL resourceSource = getClass().getClassLoader().getResource("case_1_with_spurious/ClassA_s.javaa");
        URL resourceTarget = getClass().getClassLoader().getResource("case_1_with_spurious/ClassA_t.javaa");

        TreeContext leftContext = new JdtTreeGenerator().generateFrom().file(resourceSource.getFile());
        TreeContext rightContext = new JdtTreeGenerator().generateFrom().file(resourceTarget.getFile());
        assertFalse(leftContext.getRoot().isIsomorphicTo(rightContext.getRoot()));

        Matcher matcher = new CompositeMatchers.ClassicGumtree();
        SimplifiedChawatheScriptGenerator edGenerator = new SimplifiedChawatheScriptGenerator();

        ConfigurableMatcher configurableMatcher = (ConfigurableMatcher) matcher;
        GumtreeProperties properties = new GumtreeProperties();
        // With 1001 fails
        properties.tryConfigure(ConfigurationOptions.bu_minsize, 1002);

        configurableMatcher.configure(properties);

        MappingStore mappings = matcher.match(leftContext.getRoot(), rightContext.getRoot());

        EditScript actions = edGenerator.computeActions(mappings);

        List<Action> actionsAll = actions.asList();

        assertEquals(1, actionsAll.size());

        assertEquals("update-node", actionsAll.get(0).getName());

    }

    /**
     * This test plays with different configuration of Classic in order to have
     * different outputs
     */
    @Test
    public void testSpurious1WithClassicConfiguredGreedySubtreeMatcher() throws IOException {

        URL resourceSource = getClass().getClassLoader().getResource("case_1_with_spurious/ClassA_s.javaa");
        URL resourceTarget = getClass().getClassLoader().getResource("case_1_with_spurious/ClassA_t.javaa");

        TreeContext leftContext = new JdtTreeGenerator().generateFrom().file(resourceSource.getFile());
        TreeContext rightContext = new JdtTreeGenerator().generateFrom().file(resourceTarget.getFile());
        assertFalse(leftContext.getRoot().isIsomorphicTo(rightContext.getRoot()));

        Matcher matcher = new CompositeMatchers.ClassicGumtree();
        SimplifiedChawatheScriptGenerator edGenerator = new SimplifiedChawatheScriptGenerator();

        ConfigurableMatcher configurableMatcher = (ConfigurableMatcher) matcher;
        GumtreeProperties properties = new GumtreeProperties();
        // Let's try with Min height = 1:

        properties.tryConfigure(ConfigurationOptions.st_minprio, 1);

        configurableMatcher.configure(properties);

        MappingStore mappings = matcher.match(leftContext.getRoot(), rightContext.getRoot());

        EditScript actions = edGenerator.computeActions(mappings);

        List<Action> actionsAll = actions.asList();

        assertEquals(1, actionsAll.size());

        assertEquals("update-node", actionsAll.get(0).getName());

        // With Min = 1 the SubTreeMatcher matches the import
        GreedySubtreeMatcher greedyMatcher = new GreedySubtreeMatcher();
        greedyMatcher.configure(properties);

        MappingStore mappingsFromGreedy = greedyMatcher.match(leftContext.getRoot(), rightContext.getRoot());

        // Let's try with Min height = 2, which produces an unexpected output:
        // ImportDecl
        assertTrue(mappingsFromGreedy.isSrcMapped(leftContext.getRoot().getChild(2)));
        // ImportDecl
        assertTrue(mappingsFromGreedy.isSrcMapped(leftContext.getRoot().getChild(1)));
        // PackageDecl
        assertTrue(mappingsFromGreedy.isSrcMapped(leftContext.getRoot().getChild(0)));

        // Now check unmapped when Min = 2 (Default)
        properties = new GumtreeProperties();
        properties.tryConfigure(ConfigurationOptions.st_minprio, 2);
        assertEquals(2, properties.get(ConfigurationOptions.st_minprio));
        greedyMatcher.configure(properties);
        mappingsFromGreedy = greedyMatcher.match(leftContext.getRoot(), rightContext.getRoot());

        assertEquals(2, greedyMatcher.getMinPriority());
        // Check those not mapped with Min = 2
        // ImportDecl
        assertFalse(mappingsFromGreedy.isSrcMapped(leftContext.getRoot().getChild(2)));
        // ImportDecl
        assertFalse(mappingsFromGreedy.isSrcMapped(leftContext.getRoot().getChild(1)));
        // PackageDecl
        assertFalse(mappingsFromGreedy.isSrcMapped(leftContext.getRoot().getChild(0)));

    }

    @Test
    public void testCase_1_20391Classic() throws IOException {
        String caseDir = "case_1_203910661b72775d1a983bf98c25ddde2d2898b9";
        URL resourceSource = getClass().getClassLoader().getResource(
                caseDir + "/1_203910661b72775d1a983bf98c25ddde2d2898b9_Producto_s.javaa");
        URL resourceTarget = getClass().getClassLoader().getResource(
                caseDir + "/1_203910661b72775d1a983bf98c25ddde2d2898b9_Producto_t.javaa");

        TreeContext leftContext = new JdtTreeGenerator().generateFrom().file(resourceSource.getFile());
        TreeContext rightContext = new JdtTreeGenerator().generateFrom().file(resourceTarget.getFile());

        Matcher matcher = new CompositeMatchers.ClassicGumtree();

        SimplifiedChawatheScriptGenerator edGenerator = new SimplifiedChawatheScriptGenerator();

        MappingStore mappings = matcher.match(leftContext.getRoot(), rightContext.getRoot());

        EditScript actions = edGenerator.computeActions(mappings);

        List<Action> actionsAll = actions.asList();

        // There is not failure but the output is incorrect
        assertTrue(actionsAll.size() > 0);

    }

    @Test
    public void testCase_1_20391_Complete_Int2Obj() throws IOException {

        String foldercase = "case_1_203910661b72775d1a983bf98c25ddde2d2898b9";
        URL resourceSource = getClass().getClassLoader()
                .getResource(foldercase + "/1_203910661b72775d1a983bf98c25ddde2d2898b9_Producto_s.javaa");
        URL resourceTarget = getClass().getClassLoader()
                .getResource(foldercase + "/1_203910661b72775d1a983bf98c25ddde2d2898b9_Producto_t.javaa");

        TreeContext leftContext = new JdtTreeGenerator().generateFrom().file(resourceSource.getFile());
        TreeContext rightContext = new JdtTreeGenerator().generateFrom().file(resourceTarget.getFile());

        Matcher matcher = new CompositeMatchers.PartitionGumtreeMatcher();

        SimplifiedChawatheScriptGenerator edGenerator = new SimplifiedChawatheScriptGenerator();

        MappingStore mappings = matcher.match(leftContext.getRoot(), rightContext.getRoot());

        EditScript actions = edGenerator.computeActions(mappings);

        List<Action> actionsAll = actions.asList();

        // There is not failure but the output is incorrect
        assertTrue(actionsAll.size() > 0);

    }

    @Test
    public void testCase_1_0007_Simple() throws IOException {

        String caseDir = "case_1_0007d191fec7fe2d6a0c4e87594cb286a553f92c_ASTInspector/";
        String pathSource = caseDir + "1_0007d191fec7fe2d6a0c4e87594cb286a553f92c_ASTInspector_s.javaa";
        String pathTarget = caseDir + "1_0007d191fec7fe2d6a0c4e87594cb286a553f92c_ASTInspector_t.javaa";

        URL resourceSource = getClass().getClassLoader().getResource(pathSource);
        URL resourceTarget = getClass().getClassLoader().getResource(pathTarget);

        TreeContext leftContext = new JdtTreeGenerator().generateFrom().file(resourceSource.getFile());
        TreeContext rightContext = new JdtTreeGenerator().generateFrom().file(resourceTarget.getFile());

        Matcher matcher = new CompositeMatchers.SimpleGumtree();

        SimplifiedChawatheScriptGenerator edGenerator = new SimplifiedChawatheScriptGenerator();

        MappingStore mappings = matcher.match(leftContext.getRoot(), rightContext.getRoot());

        EditScript actions = edGenerator.computeActions(mappings);

        List<Action> actionsAll = actions.asList();

        // There is not failure but the output is incorrect
        assertTrue(actionsAll.size() > 0);

        // It should be one.
        assertEquals(1, actionsAll.size());

        assertTrue(actionsAll.get(0) instanceof TreeInsert);

        assertEquals("ExpressionStatement", actionsAll.get(0).getNode().getType().name);
        assertEquals("SwitchStatement", actionsAll.get(0).getNode().getParent().getType().name);

    }

    @Test
    public void testCase_1_0007_Classic() throws IOException {

        String caseDir = "case_1_0007d191fec7fe2d6a0c4e87594cb286a553f92c_ASTInspector/";
        String pathSource = caseDir + "1_0007d191fec7fe2d6a0c4e87594cb286a553f92c_ASTInspector_s.javaa";
        String pathTarget = caseDir + "1_0007d191fec7fe2d6a0c4e87594cb286a553f92c_ASTInspector_t.javaa";

        URL resourceSource = getClass().getClassLoader().getResource(pathSource);
        URL resourceTarget = getClass().getClassLoader().getResource(pathTarget);

        TreeContext leftContext = new JdtTreeGenerator().generateFrom().file(resourceSource.getFile());
        TreeContext rightContext = new JdtTreeGenerator().generateFrom().file(resourceTarget.getFile());

        Matcher matcher = new CompositeMatchers.ClassicGumtree();

        SimplifiedChawatheScriptGenerator edGenerator = new SimplifiedChawatheScriptGenerator();

        MappingStore mappings = matcher.match(leftContext.getRoot(), rightContext.getRoot());

        EditScript actions = edGenerator.computeActions(mappings);

        List<Action> actionsAll = actions.asList();

        // There is not failure but the output is incorrect
        assertTrue(actionsAll.size() > 0);

    }

    @Test
    public void testNotSpurious1Complete() throws IOException {

        URL resourceSource = getClass().getClassLoader().getResource("case_1_with_spurious/ClassA_s.javaa");
        URL resourceTarget = getClass().getClassLoader().getResource("case_1_with_spurious/ClassA_t.javaa");

        TreeContext leftContext = new JdtTreeGenerator().generateFrom().file(resourceSource.getFile());
        TreeContext rightContext = new JdtTreeGenerator().generateFrom().file(resourceTarget.getFile());
        assertFalse(leftContext.getRoot().isIsomorphicTo(rightContext.getRoot()));

        CompositeMatchers.PartitionGumtreeMatcher matcher = new CompositeMatchers.PartitionGumtreeMatcher();
        ChawatheScriptGenerator edGenerator = new ChawatheScriptGenerator();

        MappingStore mappings = matcher.match(leftContext.getRoot(), rightContext.getRoot());

        EditScript actions = edGenerator.computeActions(mappings);

        List<Action> actionsAll = actions.asList();

        assertTrue(actionsAll.size() > 0);

    }

    @Test
    public void testCase_1_0a66_Simple() throws IOException {

        String caseDir = "case_1_0a664d752c4b0e5a7fb6f06d005181a0c9dc2905/";
        String pathSource = caseDir + "1_0a664d752c4b0e5a7fb6f06d005181a0c9dc2905_FlowControlService_s.javaa";
        String pathTarget = caseDir + "1_0a664d752c4b0e5a7fb6f06d005181a0c9dc2905_FlowControlService_t.javaa";

        URL resourceSource = getClass().getClassLoader().getResource(pathSource);
        URL resourceTarget = getClass().getClassLoader().getResource(pathTarget);

        assertTrue((new File(resourceSource.getFile())).exists());
        assertTrue((new File(resourceTarget.getFile())).exists());

        TreeContext leftContext = new JdtTreeGenerator().generateFrom().file(resourceSource.getFile());
        TreeContext rightContext = new JdtTreeGenerator().generateFrom().file(resourceTarget.getFile());

        Matcher matcher = new CompositeMatchers.SimpleGumtree();

        SimplifiedChawatheScriptGenerator edGenerator = new SimplifiedChawatheScriptGenerator();

        MappingStore mappings = matcher.match(leftContext.getRoot(), rightContext.getRoot());

        EditScript actions = edGenerator.computeActions(mappings);

        List<Action> actionsAll = actions.asList();

        // There is not failure but the output is incorrect
        assertTrue(actionsAll.size() > 0);

        // It should be one.
        assertEquals(1, actionsAll.size());

        assertTrue(actionsAll.get(0) instanceof Insert);
        assertEquals("ReturnStatement", actionsAll.get(0).getNode().getType().name);

    }

    @Test
    public void testCase_1_0a66_CompleteGumtreeMatcher() throws IOException {

        String caseDir = "case_1_0a664d752c4b0e5a7fb6f06d005181a0c9dc2905/";
        String pathSource = caseDir + "1_0a664d752c4b0e5a7fb6f06d005181a0c9dc2905_FlowControlService_s.javaa";
        String pathTarget = caseDir + "1_0a664d752c4b0e5a7fb6f06d005181a0c9dc2905_FlowControlService_t.javaa";

        URL resourceSource = getClass().getClassLoader().getResource(pathSource);
        URL resourceTarget = getClass().getClassLoader().getResource(pathTarget);

        assertTrue((new File(resourceSource.getFile())).exists());
        assertTrue((new File(resourceTarget.getFile())).exists());

        TreeContext leftContext = new JdtTreeGenerator().generateFrom().file(resourceSource.getFile());
        TreeContext rightContext = new JdtTreeGenerator().generateFrom().file(resourceTarget.getFile());

        Matcher matcher = new CompositeMatchers.PartitionGumtreeMatcher();

        SimplifiedChawatheScriptGenerator edGenerator = new SimplifiedChawatheScriptGenerator();

        MappingStore mappings = matcher.match(leftContext.getRoot(), rightContext.getRoot());

        EditScript actions = edGenerator.computeActions(mappings);

        List<Action> actionsAll = actions.asList();

        // There is not failure but the output is incorrect
        assertTrue(actionsAll.size() > 0);

    }

}
