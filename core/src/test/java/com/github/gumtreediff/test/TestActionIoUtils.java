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

package com.github.gumtreediff.test;

import com.github.gumtreediff.actions.ChawatheScriptGenerator;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.io.ActionsIoUtils;
import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import com.github.gumtreediff.tree.TreeContext;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;

public class TestActionIoUtils {
    
    @Test
    public void testActionsIoUtilsInsert() throws Exception {
        String tree1 = "<tree type=\"0\" typeLabel=\"0\" pos=\"0\" length=\"100\"></tree>";
        String tree2 = "<tree type=\"0\" typeLabel=\"0\" pos=\"101\" length=\"100\">"
                + "<tree type=\"1\" typeLabel=\"1\" pos=\"102\" length=\"2\"></tree></tree>";
        TreeContext tc1 = TreeIoUtils.fromXml().generateFrom().string(tree1);
        TreeContext tc2 = TreeIoUtils.fromXml().generateFrom().string(tree2);
        MappingStore ms = new MappingStore(tc1.getRoot(), tc2.getRoot());
        ms.addMapping(tc1.getRoot(), tc2.getRoot());
        ChawatheScriptGenerator gen = new ChawatheScriptGenerator();
        EditScript es = gen.computeActions(ms);
        assertEquals("===\ninsert-node\n---\n1 [102,104]\nto\n0 [0,100]\nat 0", es.get(0).toString());
        assertEquals("===\n" +
                "match\n" +
                "---\n" +
                "0 [0,100]\n" +
                "0 [101,201]\n" +
                "===\n" +
                "insert-node\n" +
                "---\n" +
                "1 [102,104]\n" +
                "to\n" +
                "0 [0,100]\n" +
                "at 0\n", ActionsIoUtils.toText(tc1, es, ms).toString());
        assertEquals("{\n" +
                "  \"matches\": [\n" +
                "    {\n" +
                "      \"src\": \"0 [0,100]\",\n" +
                "      \"dest\": \"0 [101,201]\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"actions\": [\n" +
                "    {\n" +
                "      \"action\": \"insert-node\",\n" +
                "      \"tree\": \"1 [102,104]\",\n" +
                "      \"parent\": \"0 [0,100]\",\n" +
                "      \"at\": 0\n" +
                "    }\n" +
                "  ]\n" +
                "}", ActionsIoUtils.toJson(tc1, es, ms).toString());
        assertEquals("<?xml version=\"1.0\" ?>\n" +
                "<matches>\n" +
                "  <match src=\"0 [0,100]\" dest=\"0 [101,201]\"/>\n" +
                "</matches>\n" +
                "<actions>\n" +
                "  <insert-node tree=\"1 [102,104]\" parent=\"0 [0,100]\" at=\"0\"/>\n" +
                "</actions>\n", ActionsIoUtils.toXml(tc1, es, ms).toString());
    }

    @Test
    public void testActionsIoUtilsMove() throws Exception {
        String tree1 = "<tree type=\"0\" typeLabel=\"0\" pos=\"0\" length=\"100\">"
                + "<tree type=\"1\" typeLabel=\"1\" pos=\"1\" length=\"10\"/>"
                + "<tree type=\"2\" typeLabel=\"2\" pos=\"11\" length=\"10\"/></tree>";
        String tree2 = "<tree type=\"0\" typeLabel=\"0\" pos=\"101\" length=\"100\">"
                + "<tree type=\"2\" typeLabel=\"2\" pos=\"101\" length=\"10\"/>"
                + "<tree type=\"1\" typeLabel=\"1\" pos=\"111\" length=\"10\"/></tree>";
        TreeContext tc1 = TreeIoUtils.fromXml().generateFrom().string(tree1);
        TreeContext tc2 = TreeIoUtils.fromXml().generateFrom().string(tree2);
        MappingStore ms = new MappingStore(tc1.getRoot(), tc2.getRoot());
        ms.addMapping(tc1.getRoot(), tc2.getRoot());
        ms.addMapping(tc1.getRoot().getChild(0), tc2.getRoot().getChild(1));
        ms.addMapping(tc1.getRoot().getChild(1), tc2.getRoot().getChild(0));
        ChawatheScriptGenerator gen = new ChawatheScriptGenerator();
        EditScript es = gen.computeActions(ms);
        assertEquals("===\n" +
                "move-tree\n" +
                "---\n" +
                "1 [1,11]\n" +
                "to\n" +
                "0 [0,100]\n" +
                "at 1", es.get(0).toString());
        assertThat(ActionsIoUtils.toText(tc1, es, ms).toString(), containsString("===\n" +
                "move-tree\n" +
                "---\n" +
                "1 [1,11]\n" +
                "to\n" +
                "0 [0,100]\n" +
                "at 1\n"));
        assertThat(ActionsIoUtils.toJson(tc1, es, ms).toString(), containsString("  \"actions\": [\n" +
                "    {\n" +
                "      \"action\": \"move-tree\",\n" +
                "      \"tree\": \"1 [1,11]\",\n" +
                "      \"parent\": \"0 [0,100]\",\n" +
                "      \"at\": 1\n" +
                "    }\n" +
                "  ]\n"));
        assertThat(ActionsIoUtils.toXml(tc1, es, ms).toString(), containsString("<actions>\n" +
                "  <move-tree tree=\"1 [1,11]\" parent=\"0 [0,100]\" at=\"1\"/>\n" +
                "</actions>\n"));
    }

    @Test
    public void testActionsIoUtilsUpdate() throws Exception {
        String tree1 = "<tree type=\"0\" typeLabel=\"0\" pos=\"0\" length=\"100\">"
                + "<tree type=\"0\" label=\"foo\" typeLabel=\"0\" pos=\"1\" length=\"10\"/></tree>";
        String tree2 = "<tree type=\"0\" typeLabel=\"0\" pos=\"101\" length=\"100\">"
                + "<tree type=\"0\" label=\"bar\" typeLabel=\"0\" pos=\"101\" length=\"10\"/></tree>";
        TreeContext tc1 = TreeIoUtils.fromXml().generateFrom().string(tree1);
        TreeContext tc2 = TreeIoUtils.fromXml().generateFrom().string(tree2);
        MappingStore ms = new MappingStore(tc1.getRoot(), tc2.getRoot());
        ms.addMapping(tc1.getRoot(), tc2.getRoot());
        ms.addMapping(tc1.getRoot().getChild(0), tc2.getRoot().getChild(0));
        ChawatheScriptGenerator gen = new ChawatheScriptGenerator();
        EditScript es = gen.computeActions(ms);
        assertEquals("===\n" +
                "update-node\n" +
                "---\n" +
                "0: foo [1,11]\n" +
                "replace foo by bar", es.get(0).toString());
        assertThat(ActionsIoUtils.toText(tc1, es, ms).toString(), containsString("===\n" +
                        "update-node\n" +
                        "---\n" +
                        "0: foo [1,11]\n" +
                        "replace foo by bar\n"));
        assertThat(ActionsIoUtils.toJson(tc1, es, ms).toString(), containsString("  \"actions\": [\n" +
                        "    {\n" +
                        "      \"action\": \"update-node\",\n" +
                        "      \"tree\": \"0: foo [1,11]\",\n" +
                        "      \"label\": \"bar\"\n" +
                        "    }\n" +
                        "  ]\n"));
        assertThat(ActionsIoUtils.toXml(tc1, es, ms).toString(), containsString("<actions>\n" +
                        "  <update-node tree=\"0: foo [1,11]\" label=\"bar\"/>\n" +
                        "</actions>\n"));
    }
}
