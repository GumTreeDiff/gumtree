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
 * Copyright 2024 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.matchers;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import com.github.gumtreediff.utils.Registry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AutoMatchers {

    @Register(id = "gumtree-simple-auto-st", priority = Registry.Priority.HIGH)
    public static class SimpleGumtreeAuto implements Matcher {
        @Override
        public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
            MappingStore bestMappings = null;
            int bestSize = Integer.MAX_VALUE;
            for (GumtreeProperties prop: propertiesForSimple()) {
                Matcher matcher = new CompositeMatchers.SimpleGumtreeStable();
                matcher.configure(prop);
                mappings = matcher.match(src, dst);
                EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator();
                EditScript s = editScriptGenerator.computeActions(mappings);
                if (s.size() < bestSize) {
                    bestSize = s.size();
                    bestMappings = mappings;
                }
            }
            return bestMappings;
        }
    }

    @Register(id = "gumtree-simple-auto", priority = Registry.Priority.HIGH)
    public static class SimpleGumtreeAutoMt implements Matcher {
        @Override
        public MappingStore match(Tree src, Tree dst, MappingStore mappings) {
            return propertiesForSimple().parallelStream().map(props -> {
                Matcher matcher = new CompositeMatchers.SimpleGumtreeStable();
                matcher.configure(props);
                MappingStore curMappings = matcher.match(src, dst);
                EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator();
                EditScript script = editScriptGenerator.computeActions(curMappings);
                return new Pair<>(curMappings, script.size());
            }).min(Comparator.comparingInt(pair -> pair.second)).get().first;
        }
    }

    private static List<GumtreeProperties> propertiesForSimple() {
        ArrayList<GumtreeProperties> properties = new ArrayList<>();
        for (double minSim = 0.1; minSim <= 0.9; minSim += 0.2) {
            for (int minPrio = 5; minPrio >= 1; minPrio -= 1) {
                GumtreeProperties prop = new GumtreeProperties();
                prop.put(ConfigurationOptions.st_minprio, minPrio);
                prop.put(ConfigurationOptions.bu_minsim, minSim);
                properties.add(prop);
            }
        }
        return properties;
    }

}
