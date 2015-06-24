package com.github.gumtreediff.client.diff.ui.web.views;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.gen.Generators;
import com.github.gumtreediff.io.ActionsIoUtils;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.TreeContext;
import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.rendersnake.HtmlAttributesFactory.class_;
import static org.rendersnake.HtmlAttributesFactory.lang;

public class ScriptView implements Renderable {

    private File fSrc;

    private File fDst;

    private List<Action> script;

    public ScriptView(File fSrc, File fDst) throws IOException {
        this.fSrc = fSrc;
        this.fDst = fDst;
        TreeContext src = Generators.getInstance().getTree(fSrc.getAbsolutePath());
        TreeContext dst = Generators.getInstance().getTree(fDst.getAbsolutePath());
        Matcher matcher = Matchers.getInstance().getMatcher(src.getRoot(), dst.getRoot());
        matcher.match();
        ActionGenerator g = new ActionGenerator(src.getRoot(), dst.getRoot(), matcher.getMappings());
        g.generate();
        this.script = g.getActions();
    }

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        html
                .render(DocType.HTML5)
                .html(lang("en"))
                .render(new BootstrapHeader())
                .body()
                .div(class_("container"))
                .div(class_("row"))
                .div(class_("col-lg-12"))
                .h3()
                .write("Script ")
                .small().content(String.format("%s -> %s", fSrc.getName(), fDst.getName()))
                ._h3()
                .pre().content(ActionsIoUtils.toText(this.script))
                ._div()
                ._div()
                ._div()
                .macros().javascript("res/web/jquery.min.js")
                .macros().javascript("res/web/bootstrap.min.js")
                .macros().javascript("res/web/script.js")
                ._body()
                ._html();
    }

}
