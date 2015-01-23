package fr.labri.gumtree.client.ui.web.views;

import static org.rendersnake.HtmlAttributesFactory.class_;
import static org.rendersnake.HtmlAttributesFactory.lang;

import java.io.File;
import java.io.IOException;

import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import fr.labri.gumtree.client.TreeGeneratorRegistry;
import fr.labri.gumtree.client.ui.web.HtmlDiffs;
import fr.labri.gumtree.matchers.Matcher;
import fr.labri.gumtree.matchers.MatcherFactories;
import fr.labri.gumtree.tree.Tree;

public class DiffView implements Renderable {
	
	private HtmlDiffs diffs;
	
	private File fSrc;
	
	private File fDst;
	
	public DiffView(File fSrc, File fDst) throws IOException {
		this.fSrc = fSrc;
		this.fDst = fDst;
		Tree src = TreeGeneratorRegistry.getInstance().getTree(fSrc.getAbsolutePath());
		Tree dst = TreeGeneratorRegistry.getInstance().getTree(fDst.getAbsolutePath());
		Matcher matcher = MatcherFactories.newMatcher(src, dst);
		matcher.match();
		diffs = new HtmlDiffs(fSrc, fDst, src, dst, matcher);
		diffs.produce();
	}

	@Override
	public void renderOn(HtmlCanvas html) throws IOException {
		html
		.render(DocType.HTML5)
		.html(lang("en"))
			.render(new BootstrapHeader())
			.body()
				.div(class_("container-fluid"))
					.div(class_("row"))
						.render(new MenuBar())
					._div()
					.div(class_("row"))
						.div(class_("col-lg-6 max-height"))
							.h5().content(fSrc.getName())
							.pre(class_("pre max-height")).content(diffs.getSrcDiff(), false)
						._div()
						.div(class_("col-lg-6 max-height"))
							.h5().content(fDst.getName())
							.pre(class_("pre max-height")).content(diffs.getDstDiff(), false)
						._div()
					._div()
				._div()
				.macros().javascript("res/web/jquery.min.js")
				.macros().javascript("res/web/bootstrap.min.js")
				.macros().javascript("res/web/diff.js")
			._body()
		._html();
	}
	
	public static class MenuBar implements Renderable {

		@Override
		public void renderOn(HtmlCanvas html) throws IOException {
			html
			.div(class_("col-lg-12"))
				.div(class_("btn-toolbar pull-right"))
					.div(class_("btn-group"))
					    .a(class_("btn btn-default btn-xs").id("legend").href("#").add("data-toggle", "popover").add("data-html", "true").add("data-placement", "bottom").add("data-content", "<span class=&quot;del&quot;>&nbsp;&nbsp;</span> deleted<br><span class=&quot;add&quot;>&nbsp;&nbsp;</span> added<br><span class=&quot;mv&quot;>&nbsp;&nbsp;</span> moved<br><span class=&quot;upd&quot;>&nbsp;&nbsp;</span> updated<br>", false).add("data-original-title", "Legend").title("Legend").role("button")).content("Legend")
						.a(class_("btn btn-default btn-xs").id("shortcuts").href("#").add("data-toggle", "popover").add("data-html", "true").add("data-placement", "bottom").add("data-content", "<b>q</b> quit<br><b>l</b> list<br><b>n</b> next<br><b>t</b> top<br><b>b</b> bottom", false).add("data-original-title", "Shortcuts").title("Shortcuts").role("button")).content("Shortcuts")
					._div()
					.div(class_("btn-group"))
						.a(class_("btn btn-default btn-xs btn-danger").href("/quit")).content("Quit")
					._div()
				._div()
			._div();
		}
		
	}

}
