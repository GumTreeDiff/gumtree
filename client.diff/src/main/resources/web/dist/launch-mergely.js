const doc = new Mergely('#compare', { cmsettings : { readOnly: true, lineNumbers: true, lineWrapping: true }});
doc.once('updated', () => {
    Promise.all([fetch(lhs_url), fetch(rhs_url)]).then((values) => {
        Promise.all([values[0].text(), values[1].text()]).then((values) => {
            doc.lhs(values[0]);
            doc.rhs(values[1]);

            doc.once('updated', () => {
                doc.scrollToDiff('next');
            });
        });
    });
});