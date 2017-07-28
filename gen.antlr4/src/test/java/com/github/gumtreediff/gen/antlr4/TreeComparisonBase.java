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
 * Copyright 2017 Svante Schubert <svante.schubert gmail com>
 */
package com.github.gumtreediff.gen.antlr4;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * Provides basic functionality to compare test output files with saved
 * references by string compare.
 */
public abstract class TreeComparisonBase {

    /**
     * The default encoding when loading a file as String for comparison
     */
    protected static final String DEFAULT_ENCODING = "UTF-8";
    private static final String TEST_INPUT_PATH
            = "build" + File.separatorChar + "resources" + File.separatorChar + "test" + File.separatorChar;
    private static final String TEST_OUTPUT_PATH
            = "build" + File.separatorChar + "created-test-files" + File.separatorChar;
    private static final String REFERENCE_INPUT_PATH
            = "build" + File.separatorChar + "resources" + File.separatorChar + "test" + File.separatorChar
            + "references" + File.separatorChar;
    private static final Logger LOG = Logger.getLogger(TreeComparisonBase.class.getName());
    private final String[][] mTestCouples;

    public TreeComparisonBase(String[][] testCouples) {
        mTestCouples = testCouples;
    }

    @Test
    /**
     * Creating and comparing graphs from test files having default encoding
     * (UTF-8)
     */
    public void compareTestCouples() {
        File outputDir = new File(TEST_OUTPUT_PATH);
        outputDir.mkdir();
        for (String[] testPair : mTestCouples) {
            createTreeDumps(testPair[0], testPair[1]);
        }
    }

    @After
    @Test
    public void allReferencesWithTestResultComparison() {
        if (!checkTreeDumpsForRegression(REFERENCE_INPUT_PATH, DEFAULT_ENCODING)) {
            Assert.fail("There was at least one change of a test output!");
        }
    }

    /**
     * Create graphs from given test file names.
     *
     * @param srcName1 name of first test input for grammar
     * (originally from src/resources/test/)
     * @param srcName2 name of second test input for grammar
     * (originally from src/resources/test/)
     */
    protected void createTreeDumps(String srcName1, String srcName2) {
        LOG.log(Level.INFO, "Comparing the files:\n{0}{1}\nand\n{2}{3}",
                new Object[]{TEST_INPUT_PATH, srcName1, TEST_INPUT_PATH, srcName2});
        Run.initGenerators();

        TreeContext ctxSrc1 = null;
        TreeContext ctxSrc2 = null;
        try {
            ctxSrc1 = getTreeGenerator().generateFromFile(TEST_INPUT_PATH + srcName1);
            assert (ctxSrc1 != null);
            ctxSrc2 = getTreeGenerator().generateFromFile(TEST_INPUT_PATH + srcName2);
            assert (ctxSrc2 != null);
        } catch (IOException ex) {
            Logger.getLogger(TreeComparisonBase.class.getName()).log(Level.SEVERE, null, ex);
        }
        // dump both trees in various formats
        saveTrees(ctxSrc1, ctxSrc2, srcName1, srcName2);
        ITree src = ctxSrc1.getRoot();
        ITree dst = ctxSrc2.getRoot();
        Matcher m = Matchers.getInstance().getMatcher(src, dst); // retrieve the default matcher
        m.match();
        // dump both tress in annotated XML
        saveAnnotatedTrees(ctxSrc1, ctxSrc2, m.getMappings(), srcName1, srcName2);
        ActionGenerator g = new ActionGenerator(src, dst, m.getMappings());
        g.generate();
        java.util.List<Action> actions = g.getActions(); // return the actions
        // dump actions (the edit script)
        saveActions(actions,
                new File(TEST_OUTPUT_PATH + File.separatorChar + srcName1 + "_TO_" + srcName2 + "_Changes.txt"));
    }

    /**
     * @param debug may enable additional debug output
     *
     * @return the TreeGenerator for a specific ANTL4 grammar
     */
    protected abstract TreeGenerator getTreeGenerator();

    /**
     * Compares the new test output files of created trees with the existing
     * references. NOTE: When the difference results from a new feature, you
     * need to update the references.
     */
    protected boolean checkTreeDumpsForRegression(String referenceInputPath, String encoding) {
        boolean noRegression = false;
        File dir = new File(referenceInputPath);
        File[] referenceDirecotry = dir.listFiles();
        if (referenceDirecotry != null) {
            noRegression = true;
            for (File referenceFile : referenceDirecotry) {
                String fileName = referenceFile.getName();
                String referenceString = loadFileAsString(
                        new File(TEST_OUTPUT_PATH + File.separatorChar + fileName), encoding);
                String testString = loadFileAsString(referenceFile, encoding);
                if (testString == null || testString.isEmpty()) {
                    LOG.log(Level.SEVERE, "The test result of {0} is empty", fileName);
                    noRegression = false;
                } else if (!referenceString.equals(testString)) {
                    LOG.log(Level.SEVERE, "Test and reference files differ!\n"
                            + "The original reference of {0} has been:\n{1}", new Object[]{fileName, referenceString});
                    LOG.log(Level.SEVERE, "But The new created file is :\n{0}", testString);
                    noRegression = false;
                }
            }
        } else {
            throw new RuntimeException(referenceInputPath + " is not a directory!");
        }
        return noRegression;
    }

    private void saveTrees(TreeContext ctxSrc1, TreeContext ctxSrc2, String srcName1, String srcName2) {
        try {
            TreeIoUtils.toXml(ctxSrc1).writeTo(new File(TEST_OUTPUT_PATH + File.separatorChar + srcName1 + ".xml"));
            TreeIoUtils.toXml(ctxSrc2).writeTo(new File(TEST_OUTPUT_PATH + File.separatorChar + srcName2 + ".xml"));

            TreeIoUtils.toCompactXml(ctxSrc1).writeTo(
                    new File(TEST_OUTPUT_PATH + File.separatorChar + srcName1 + "_srcCompact.xml"));
            TreeIoUtils.toCompactXml(ctxSrc2).writeTo(
                    new File(TEST_OUTPUT_PATH + File.separatorChar + srcName2 + "_dstCompact.xml"));

            TreeIoUtils.toDot(ctxSrc1).writeTo(
                    new File(TEST_OUTPUT_PATH + File.separatorChar + srcName1 + "_src.dot"));
            TreeIoUtils.toDot(ctxSrc2).writeTo(
                    new File(TEST_OUTPUT_PATH + File.separatorChar + srcName2 + "_dst.dot"));

            TreeIoUtils.toJson(ctxSrc1).writeTo(
                    new File(TEST_OUTPUT_PATH + File.separatorChar + srcName1 + "_src.json"));
            TreeIoUtils.toJson(ctxSrc2).writeTo(
                    new File(TEST_OUTPUT_PATH + File.separatorChar + srcName2 + "_dst.json"));

            TreeIoUtils.toLisp(ctxSrc1).writeTo(
                    new File(TEST_OUTPUT_PATH + File.separatorChar + srcName1 + "_src.lisp"));
            TreeIoUtils.toLisp(ctxSrc2).writeTo(
                    new File(TEST_OUTPUT_PATH + File.separatorChar + srcName2 + "_dst.lisp"));
        } catch (Exception ex) {
            Logger.getLogger(AbstractAntlr4TreeGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void saveAnnotatedTrees(TreeContext ctxSrc1, TreeContext ctxSrc2,
            MappingStore ms, String srcName1, String srcName2) {
        try {
            TreeIoUtils.toAnnotatedXml(ctxSrc1, true, ms).writeTo(
                    new File(TEST_OUTPUT_PATH + File.separatorChar + srcName1 + "_srcAnnotated.xml"));
            TreeIoUtils.toAnnotatedXml(ctxSrc2, false, ms).writeTo(
                    new File(TEST_OUTPUT_PATH + File.separatorChar + srcName2 + "_dstAnnotated.xml"));
        } catch (Exception ex) {
            Logger.getLogger(TreeComparisonBase.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void saveActions(List<Action> actions, File fileDestination) {
        try (PrintWriter out = new PrintWriter(fileDestination)) {
            for (Action a : actions) {
                out.println(a.toString());
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TreeComparisonBase.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @param file the file to be loaded, when accessing a test file, you
     * might use <code>newTestOutputFile(String relativeFilePath)</code>.
     * @return the data from the given file as a String
     */
    private static String loadFileAsString(File file, String encoding) {
        FileInputStream input = null;
        String result = null;
        try {
            input = new FileInputStream(file);
            byte[] fileData = new byte[input.available()];
            input.read(fileData);
            input.close();
            result = new String(fileData, encoding);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TreeComparisonBase.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TreeComparisonBase.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                input.close();
            } catch (IOException ex) {
                Logger.getLogger(TreeComparisonBase.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return result;
    }
}
