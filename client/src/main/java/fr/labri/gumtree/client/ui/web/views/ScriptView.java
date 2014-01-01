package fr.labri.gumtree.client.ui.web.views;

import static org.rendersnake.HtmlAttributesFactory.class_;
import static org.rendersnake.HtmlAttributesFactory.lang;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import fr.labri.gumtree.actions.ActionGenerator;
import fr.labri.gumtree.actions.model.Action;
import fr.labri.gumtree.client.TreeGeneratorRegistry;
import fr.labri.gumtree.io.ActionsSerializer;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.MatcherFactories;
import fr.labri.gumtree.tree.Tree;

public class ScriptView implements Renderable {
	
	private File fSrc;
	
	private File fDst;
	
	private List<Action> script;
	
	public ScriptView(File fSrc, File fDst) throws IOException {
		this.fSrc = fSrc;
		this.fDst = fDst;
		Tree src = TreeGeneratorRegistry.getInstance().getTree(fSrc.getAbsolutePath());
		Tree dst = TreeGeneratorRegistry.getInstance().getTree(fDst.getAbsolutePath());
		Matcher matcher = MatcherFactories.newMatcher(src, dst);
		matcher.match();
		ActionGenerator g = new ActionGenerator(src, dst, matcher.getMappings());
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
							.pre().content(ActionsSerializer.toText(this.script))
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
