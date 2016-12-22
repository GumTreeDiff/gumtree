$(document).ready(function () {
    $('#compare').mergely({
        width: 'auto',
        cmsettings: { readOnly: true, lineNumbers: true },
    });
    //var lhs_url = 'lhs.txt';
    //var rhs_url = 'rhs.txt'

    $.get(lhs_url, function(data) {
        $('#compare').mergely('lhs', data);
    });

    $.get(rhs_url, function(data) {
        $('#compare').mergely('rhs', data);
    });
});