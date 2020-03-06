$(document).ready(function () {
    $('#mergely').mergely({
        cmsettings: {
            readOnly: true,
            lineNumbers: true,
            lineWrapping: true
        }
    });

    $.get(lhs_url, function(data) {
        $('#mergely').mergely('lhs', data);
    });

    $.get(rhs_url, function(data) {
        $('#mergely').mergely('rhs', data);
    });
});