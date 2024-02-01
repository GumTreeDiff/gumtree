const doc = new Mergely('#compare', { cmsettings : { readOnly: true, lineNumbers: true, lineWrapping: true }});
doc.once('updated', () => {
    Promise.all([
        fetch(lhs_url).then(result => result.text()),
        fetch(rhs_url).then(result => result.text())
    ]).then(([lhs, rhs]) => {
        doc.lhs(lhs);
        doc.rhs(rhs);
        doc.once('updated', () => {
            doc.scrollToDiff('next');
        });
    });
});