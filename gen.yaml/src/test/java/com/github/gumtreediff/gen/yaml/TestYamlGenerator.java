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
 * Copyright 2023 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.gen.yaml;

import java.io.IOException;

import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.tree.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.github.gumtreediff.tree.TypeSet.type;
import static org.junit.jupiter.api.Assertions.*;

public class TestYamlGenerator {
    @Test
    public void testSimpleSyntax() throws IOException {
        String input = "name: Build, Test and Deploy GumTree\n"
                + "\n"
                + "on:\n"
                + "  push:\n"
                +  "    branches: [ main ]\n"
                + "    tags:\n"
                + "      - 'v*'\n"
                + "  pull_request:\n"
                + "    branches: [ main ]\n"
                + "  schedule:\n"
                + "    - cron: '59 23 * * SUN'\n"
                + "  workflow_dispatch:\n"
                + "\n"
                + "jobs:\n"
                + "  build-test-deploy:\n"
                + "    runs-on: ubuntu-latest\n"
                + "    container: gumtreediff/gumtree:latest\n"
                + "    environment: MavenCentral\n"
                + "    if: \"!(contains(github.event.head_commit.message, '[no ci]') "
                + "|| startsWith(github.event.head_commit.message, 'doc'))\"\n"
                + "    steps:\n"
                + "      - name: checkout gumtree\n"
                + "        uses: actions/checkout@v2\n"
                + "        with:\n"
                + "          submodules: recursive\n"
                + "      - name: retrieve gumtree version\n"
                + "        id: version\n"
                + "        run: echo \"::set-output name=version::$(cat build.gradle "
                + "| grep \"projectsVersion =\" | cut -f 2 -d \"'\")\"\n"
                + "        shell: bash";
        Tree t = new YamlTreeGenerator().generateFrom().string(input).getRoot();
        assertEquals(83, t.getMetrics().size);
    }

    @Test
    public void testSyntaxError() throws IOException {
        String input = "- name: Checkout code\n"
                + "    uses: actions/checkout@v2";
        Assertions.assertThrows(SyntaxException.class,
                () -> new YamlTreeGenerator().generateFrom().string(input));
    }
}
