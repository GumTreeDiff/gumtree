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

package com.github.gumtreediff.client.diff.webdiff;

import com.github.gumtreediff.gen.TreeGenerators;
import com.github.gumtreediff.utils.Pair;
import com.github.gumtreediff.io.DirectoryComparator;

import j2html.tags.Tag;
import j2html.tags.specialized.HtmlTag;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static j2html.TagCreator.*;

public class DirectoryDiffView {

    public static HtmlTag build(DirectoryComparator comparator) {
        return html(
            Header.build(),
            body(
                div(
                    div(MenuBar.build()).withClass("row"),
                    div(
                        div(
                            div(
                                div(
                                    h4(
                                        join("Modified files ",
                                                span("" + comparator.getModifiedFiles().size()).withClasses("badge", "badge-secondary"))
                                    ).withClasses("card-title", "mb-0")
                                ).withClass("card-header"),
                                iff(comparator.getModifiedFiles().size() > 0, ModifiedFiles.build(comparator.getModifiedFiles(), comparator))
                            ).withClass("card")
                        ).withClass("col")
                    ).withClasses("row", "mt-3", "mb-3"),
                    div(
                        div(
                            div(
                                div(
                                    h4(
                                        join("Deleted files ",
                                                span("" + comparator.getDeletedFiles().size()).withClasses("badge", "badge-secondary"))
                                    ).withClasses("card-title", "mb-0")
                                ).withClasses("card-header", "bg-danger"),
                                iff(comparator.getDeletedFiles().size() > 0,
                                        DeletedFiles.build(comparator.getDeletedFiles(), comparator.getSrc()))
                            ).withClass("card")
                        ).withClass("col"),
                        div(
                            div(
                                div(
                                    h4(
                                        join("Added files ",
                                                span("" + comparator.getAddedFiles().size()).withClasses("badge", "badge-secondary"))
                                    ).withClasses("card-title", "mb-0")
                                ).withClasses("card-header", "bg-success"),
                                    iff(comparator.getAddedFiles().size() > 0,
                                            AddedFiles.build(comparator.getAddedFiles(), comparator.getDst()))
                            ).withClass("card")
                        ).withClass("col")
                    ).withClasses("row", "mb-3")
                ).withClass("container-fluid")
            )).withLang("en");
    }

    private class ModifiedFiles {

        public static Tag build(List<Pair<File, File>> files, DirectoryComparator comparator) {
            return table(
                tbody(
                    each(files, (id, file) -> {
                        String srcRelPath = comparator.getSrc().toAbsolutePath()
                                .relativize(file.first.toPath().toAbsolutePath()).toString();
                        String dstRelPath = comparator.getDst().toAbsolutePath()
                                .relativize(file.second.toPath().toAbsolutePath()).toString();
                        boolean isRenamed = !srcRelPath.equals(dstRelPath);
                        return tr(
                            td(
                                isRenamed
                                    ? join(text(srcRelPath), rawHtml(" &rarr; "), text(dstRelPath))
                                    : text(srcRelPath)
                            ),
                            td(
                                div(
                                    div(
                                        iff(TreeGenerators.getInstance().hasGeneratorForFile(file.first.getAbsolutePath()), join(
                                                a("monaco").withHref("/monaco-diff/" + id).withClasses("btn", "btn-primary", "btn-sm"),
                                                a("classic").withHref("/vanilla-diff/" + id).withClasses("btn", "btn-primary", "btn-sm")
                                            )
                                        ),
                                        a("monaco-native").withHref("/monaco-native-diff/" + id).withClasses("btn", "btn-primary", "btn-sm"),
                                        a("mergely").withHref("/mergely-diff/" + id).withClasses("btn", "btn-primary", "btn-sm"),
                                        a("raw").withHref("/raw-diff/" + id).withClasses("btn", "btn-primary", "btn-sm"),
                                        iff(isRenamed,
                                            button("unpair").withClasses("btn", "btn-warning", "btn-sm", "ms-2")
                                                    .attr("onclick", "unpairFiles(" + id + ")"))
                                    ).withClass("btn-group")
                                ).withClasses("btn-toolbar", "justify-content-end")
                            )
                        );
                    })
                )
            ).withClasses("table", "card-table", "table-striped", "table-condensed", "mb-0");
        }
    }

    private static class DeletedFiles {

        public static Tag build(Set<File> files, Path root) {
            return table(
                tbody(
                    each(files, file -> {
                        String relPath = root.relativize(file.toPath()).toString();
                        return tr(
                            td(relPath)
                        ).attr("draggable", "true")
                         .attr("data-path", relPath)
                         .attr("data-side", "deleted")
                         .withClass("draggable-file");
                    })
                )
            ).withClasses("table", "card-table", "table-striped", "table-condensed", "mb-0");
        }
    }

    private static class AddedFiles {

        public static Tag build(Set<File> files, Path root) {
            return table(
                tbody(
                    each(files, file -> {
                        String relPath = root.relativize(file.toPath()).toString();
                        return tr(
                            td(relPath)
                        ).attr("draggable", "true")
                         .attr("data-path", relPath)
                         .attr("data-side", "added")
                         .withClass("draggable-file");
                    })
                )
            ).withClasses("table", "card-table", "table-striped", "table-condensed", "mb-0");
        }
    }

    private static class Header {

        public static Tag build() {
            return head(
                meta().withCharset("utf8"),
                meta().withName("viewport").withContent("width=device-width, initial-scale=1.0"),
                title("GumTree"),
                link().withRel("stylesheet").withType("text/css").withHref(WebDiff.BOOTSTRAP_CSS_URL),
                script().withType("text/javascript").withSrc(WebDiff.BOOTSTRAP_JS_URL),
                script().withType("text/javascript").withSrc("/dist/shortcuts.js"),
                script().withType("text/javascript").withSrc("/dist/dragdrop.js").attr("defer", "defer"),
                rawHtml("<style>"
                        + "tr.draggable-file { transition: background-color 0.15s; }"
                        + "tr.drag-over { background-color: #ffc107 !important; }"
                        + "</style>")
            );
        }
    }

    private static class MenuBar  {

        public static Tag build() {
            return div(
                    div(
                        div(
                            a("Quit").withHref("/quit").withClasses("btn", "btn-default", "btn-sm", "btn-danger")
                        ).withClass("btn-group")
                    ).withClasses("btn-toolbar", "justify-content-end")
            ).withClass("col");
        }
    }
}
