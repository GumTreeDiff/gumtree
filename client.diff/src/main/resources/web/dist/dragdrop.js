document.addEventListener("DOMContentLoaded", function () {
    var dragSource = null;
    var rows = document.querySelectorAll("tr.draggable-file");

    rows.forEach(function (row) {
        row.style.cursor = "grab";

        row.addEventListener("dragstart", function (e) {
            dragSource = {
                path: row.getAttribute("data-path"),
                side: row.getAttribute("data-side")
            };
            e.dataTransfer.effectAllowed = "move";
            e.dataTransfer.setData("text/plain", "");
            row.style.opacity = "0.5";
        });

        row.addEventListener("dragend", function () {
            dragSource = null;
            row.style.opacity = "";
            document.querySelectorAll("tr.drag-over").forEach(function (el) {
                el.classList.remove("drag-over");
            });
        });

        row.addEventListener("dragover", function (e) {
            if (!dragSource || dragSource.side === row.getAttribute("data-side")) return;
            e.preventDefault();
            e.dataTransfer.dropEffect = "move";
        });

        row.addEventListener("dragenter", function (e) {
            if (!dragSource || dragSource.side === row.getAttribute("data-side")) return;
            e.preventDefault();
            row.classList.add("drag-over");
        });

        row.addEventListener("dragleave", function (e) {
            if (e.relatedTarget && row.contains(e.relatedTarget)) return;
            row.classList.remove("drag-over");
        });

        row.addEventListener("drop", function (e) {
            e.preventDefault();
            e.stopPropagation();
            row.classList.remove("drag-over");

            if (!dragSource) return;

            var dropSide = row.getAttribute("data-side");
            var dropPath = row.getAttribute("data-path");

            if (dragSource.side === dropSide) return;

            var srcPath, dstPath;
            if (dragSource.side === "deleted") {
                srcPath = dragSource.path;
                dstPath = dropPath;
            } else {
                srcPath = dropPath;
                dstPath = dragSource.path;
            }

            dragSource = null;

            var form = document.createElement("form");
            form.method = "POST";
            form.action = "/pair-files?src=" + encodeURIComponent(srcPath) + "&dst=" + encodeURIComponent(dstPath);
            document.body.appendChild(form);
            form.submit();
        });
    });
});

function unpairFiles(id) {
    var form = document.createElement("form");
    form.method = "POST";
    form.action = "/unpair-files?id=" + id;
    document.body.appendChild(form);
    form.submit();
}
