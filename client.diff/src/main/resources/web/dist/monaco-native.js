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

require.config({ paths: { 'vs': '/monaco/min/vs' }});
require(['vs/editor/editor.main'], function() {
    Promise.all([
        fetch(config.left.url)
            .then(result => result.text()),
        fetch(config.right.url)
            .then(result => result.text())
    ])
        .then(([left, right]) => {
            var originalModel = monaco.editor.createModel(left);
            var modifiedModel = monaco.editor.createModel(right);

            var diffEditor = monaco.editor.createDiffEditor(document.getElementById("container"));
            diffEditor.setModel({
            	original: originalModel,
            	modified: modifiedModel
            });
        });
});