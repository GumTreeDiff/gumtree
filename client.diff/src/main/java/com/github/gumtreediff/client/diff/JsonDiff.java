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

package com.github.gumtreediff.client.diff;

import com.github.gumtreediff.actions.ChawatheScriptGenerator;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.client.Register;
import com.github.gumtreediff.io.ActionsIoUtils;
import com.github.gumtreediff.matchers.MappingStore;

import java.util.List;

@Register(name = "jsondiff", description = "Dump actions in the JSON format",
        options = AbstractDiffClient.Options.class)
public class JsonDiff extends AbstractDiffClient<AbstractDiffClient.Options> {

    public JsonDiff(String[] args) {
        super(args);
    }

    @Override
    protected Options newOptions() {
        return new Options();
    }

    @Override
    public void run() {
        MappingStore ms = matchTrees();
        EditScript actions = new ChawatheScriptGenerator().computeActions(ms);
        try {
            ActionsIoUtils.toJson(getSrcTreeContext(), actions, ms).writeTo(System.out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
