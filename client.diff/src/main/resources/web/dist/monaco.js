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

function getEditorOptions(text) {
    return {
        value: text,
        readOnly: true,
        language: getLanguage(),
        automaticLayout: true,
        scrollBeyondLastLine: false,
        lineDecorationsWidth: 0,
        glyphMargin: false,
        minimap: {
            enabled: false,
        },
    };
}

function getLanguage() {
    let extension = config.file.split('.').pop().toLowerCase();
    if (extension == "java")
        return "java";
    else if (extension == "js")
        return "javascript";
    else if (extension == "rb")
        return "ruby";
    else if (extension == "css")
        return "css";
    else if (extension == "py")
        return "python";
    else if (extension == "cs")
        return "csharp";
    else if (extension == "r")
        return "r";
    else if (extension == "php")
        return "php";
    else if (extension == "c" || extension == "h" || extension == "cpp")
        return "cpp";
    else
        return undefined;
}

function getEditColor(edit) {
    if (edit == "inserted") return 'green';
    else if (edit == "deleted") return 'red';
    else if (edit == "updated") return 'yellow';
    else if (edit == "moved") return 'blue';
    else return "black";
}

function getDecoration(range, pos, endPos) {
    return {
        range: new monaco.Range(pos.lineNumber, pos.column, endPos.lineNumber, endPos.column),
        options: {
            className: range.kind,
            zIndex: range.index,
            hoverMessage: {
                value: range.tooltip,
            },
            overviewRuler: {
                color: getEditColor(range.kind),
            },
        },
    };
}

require.config({ paths: { 'vs': '/monaco/min/vs' }});
require(['vs/editor/editor.main'], function() {
    Promise.all(
        [
            fetch(config.left.url)
                .then(result => result.text())
                .then(text => monaco.editor.create(document.getElementById('left-container'), getEditorOptions(text))),
            fetch(config.right.url)
                .then(result => result.text())
                .then(text => monaco.editor.create(document.getElementById('right-container'), getEditorOptions(text)))
        ]
    ).then(([leftEditor, rightEditor]) => {
        config.mappings = config.mappings.map(mapping =>
            [
                monaco.Range.fromPositions(leftEditor.getModel().getPositionAt(mapping[0]), leftEditor.getModel().getPositionAt(mapping[1])),
                monaco.Range.fromPositions(rightEditor.getModel().getPositionAt(mapping[2]), rightEditor.getModel().getPositionAt(mapping[3])),
            ]);

        const leftDecorations = config.left.ranges.map(range => getDecoration(
             range,
             leftEditor.getModel().getPositionAt(range.from),
             leftEditor.getModel().getPositionAt(range.to)
        ));
        leftEditor.deltaDecorations([], leftDecorations);

        leftEditor.onMouseDown((event) => {
            const allDecorations = leftEditor.getModel().getDecorationsInRange(event.target.range, leftEditor.id, true)
                .filter(decoration => decoration.options.className == "updated" || decoration.options.className == "moved");
            if (allDecorations.length >= 1) {
                let activatedRange = allDecorations[0].range;
                if (allDecorations.length > 1)  {
                    for (let i = 1; i < allDecorations.length; i = i + 1) {
                        const candidateRange = allDecorations[i].range;
                        if (activatedRange.containsRange(candidateRange))
                            activatedRange = candidateRange;
                    }
                }
                const mapping = config.mappings.find(mapping => mapping[0].equalsRange(activatedRange))
                rightEditor.revealRangeInCenter(mapping[1]);
            }
        });

        const rightDecorations = config.right.ranges.map(range => getDecoration(
            range,
            rightEditor.getModel().getPositionAt(range.from),
            rightEditor.getModel().getPositionAt(range.to)
        ));
        rightEditor.deltaDecorations([], rightDecorations);

        rightEditor.onMouseDown((event) => {
            const allDecorations = rightEditor.getModel().getDecorationsInRange(event.target.range, rightEditor.id, true)
                .filter(decoration => decoration.options.className == "updated" || decoration.options.className == "moved");
            if (allDecorations.length >= 1) {
                let activatedRange = allDecorations[0].range;
                if (allDecorations.length > 1)  {
                    for (let i = 1; i < allDecorations.length; i = i + 1) {
                        const candidateRange = allDecorations[i].range;
                        if (activatedRange.containsRange(candidateRange)) activatedRange = candidateRange;
                    }
                }
                const mapping = config.mappings.find(mapping => mapping[1].equalsRange(activatedRange))
                leftEditor.revealRangeInCenter(mapping[0]);
            }
        });
    });
});