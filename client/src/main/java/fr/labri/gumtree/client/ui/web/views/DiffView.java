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
	private String srcName;
	
	private File fDst;
	private String dstName;

	private String urlFolder = "";
	
	public void setURLFolder(String urlFolder) {
		this.urlFolder = urlFolder;
	}
	
	public DiffView(File fSrc, File fDst) throws IOException {
		this(fSrc, fDst, fSrc.getName(), fDst.getName());
	}
	
	public DiffView(File fSrc, File fDst, String srcName, String dstName) throws IOException {
		this.fSrc = fSrc;
		this.fDst = fDst;
		this.srcName = srcName;
		this.dstName = dstName;
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
							.h5().content(srcName)
							.pre(class_("pre max-height")).content(diffs.getSrcDiff(), false)
						._div()
						.div(class_("col-lg-6 max-height"))
							.h5().content(dstName)
							.pre(class_("pre max-height")).content(diffs.getDstDiff(), false)
						._div()
					._div()
				._div()
				.macros().javascript("https://code.jquery.com/jquery-1.11.2.min.js")
				.macros().javascript("https://maxcdn.bootstrapcdn.com/bootstrap/3.3.4/js/bootstrap.min.js")
				.macros().javascript("/" + urlFolder  + "res/web/diff.js")
				.macros().stylesheet("https://maxcdn.bootstrapcdn.com/bootstrap/3.3.4/css/bootstrap.min.css")
				.macros().stylesheet("https://maxcdn.bootstrapcdn.com/bootstrap/3.3.4/css/bootstrap-theme.min.css")
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
						.a(class_("btn btn-default btn-xs").id("shortcuts").href("#").add("data-toggle", "popover").add("data-html", "true").add("data-placement", "bottom").add("data-content", "<b>n</b> next<br><b>t</b> top<br><b>b</b> bottom", false).add("data-original-title", "Shortcuts").title("Shortcuts").role("button")).content("Shortcuts")
					._div()
				._div()
			._div();
		}
		
	}

}
