$(document).ready(function () {
    $('#compare').mergely({
        editor_width: 'calc(50% - 25px)',
        editor_height: 'calc(100% - 25px)',
        cmsettings: {
            readOnly: true,
            lineNumbers: true,
            lineWrapping: true
        }
    });

    $.get(lhs_url, function(data) {
        $('#compare').mergely('lhs', data);
    });

    $.get(rhs_url, function(data) {
        $('#compare').mergely('rhs', data);
    });
});