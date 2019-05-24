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
 * Copyright 2017 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.client.diff;

import com.github.gumtreediff.actions.ActionClusterFinder;
import com.github.gumtreediff.actions.ChawatheScriptGenerator;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.client.Register;
import com.github.gumtreediff.matchers.MappingStore;

import java.util.List;
import java.util.Set;

@Register(name = "cluster", description = "Extract action clusters",
        options = AbstractDiffClient.Options.class)
public class ClusterDiff extends AbstractDiffClient<AbstractDiffClient.Options> {

    public ClusterDiff(String[] args) {
        super(args);
    }

    @Override
    public void run() {
        MappingStore ms = matchTrees();
        EditScript actions = new ChawatheScriptGenerator().computeActions(ms);
        ActionClusterFinder f = new ActionClusterFinder(getSrcTreeContext(), getDstTreeContext(), actions);
        for (Set<Action> cluster: f.getClusters()) {
            System.out.println("New cluster:");
            System.out.println(f.getClusterLabel(cluster));
            System.out.println("------------");
            for (Action a: cluster)
                System.out.println(a.toString());
            System.out.println();
        }
    }

    @Override
    protected Options newOptions() {
        return new Options();
    }
}
