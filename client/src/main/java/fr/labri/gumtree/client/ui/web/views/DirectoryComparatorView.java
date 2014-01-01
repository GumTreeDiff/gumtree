package fr.labri.gumtree.client.ui.web.views;

import static org.rendersnake.HtmlAttributesFactory.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import fr.labri.gumtree.io.DirectoryComparator;
import fr.labri.gumtree.tree.Pair;

public class DirectoryComparatorView implements Renderable {
	
	private DirectoryComparator comparator;
	
	public DirectoryComparatorView(DirectoryComparator comparator) throws IOException {
		this.comparator = comparator;
	}

	@Override
	public void renderOn(HtmlCanvas html) throws IOException {
		html
		.render(DocType.HTML5)
		.html(lang("en"))
			.render(new BootstrapHeader())
			.body()
				.div(class_("container"))
					.render(new ModifiedFiles("Modified", comparator.getModifiedFiles()))
					.div(class_("row"))
						.render_if(new UnmodifiedFiles("Added", comparator.getAddedFiles()), comparator.getAddedFiles().size() > 0)
						.render_if(new UnmodifiedFiles("Deleted", comparator.getDeletedFiles()), comparator.getDeletedFiles().size() > 0)
					._div()
				._div()
				.macros().javascript("res/web/jquery.min.js")
				.macros().javascript("res/web/bootstrap.min.js")
				.macros().javascript("res/web/list.js")
			._body()
		._html();
	}
	
	public static class ModifiedFiles implements Renderable {

		private String title;
		
		private List<Pair<File, File>> files;
		
		public ModifiedFiles(String title, List<Pair<File, File>> files) {
			this.title = title;
			this.files = files;
		}
		
		@Override
		public void renderOn(HtmlCanvas html) throws IOException {
			HtmlCanvas tbody = html
			.div(class_("row"))
				.div(class_("col-lg-12"))
					.h3().content(title)
					.table(class_("table table-striped table-condensed"))
						.thead()
							.tr()
								.th().content("Source file")
								.th().content("Destination file")
								.th().content("Action")
							._tr()
						._thead()
						.tbody();
			int id = 0;
			for (Pair<File, File> file : files) {
				tbody
					.tr()
						.td().content(file.getFirst().getName())
						.td().content(file.getSecond().getName())
						.td()
							.a(class_("btn btn-primary btn-xs").href("/diff?id=" + id)).content("diff")
							.write(" ")
							.a(class_("btn btn-primary btn-xs").href("/script?id=" + id)).content("script")
						._td()
					._tr();
				id++;
			}
			tbody
				._tbody()
				._table()
				._div()
				._div();
		}
	}
	
	public static class UnmodifiedFiles implements Renderable {

		private String title;
		
		private Set<File> files;
		
		public UnmodifiedFiles(String title, Set<File> files) {
			this.title = title;
			this.files = files;
		}
		
		@Override
		public void renderOn(HtmlCanvas html) throws IOException {
			HtmlCanvas tbody = html
				.div(class_("col-lg-6"))
					.h3().content(title)
					.table(class_("table table-striped table-condensed"))
						.thead()
							.tr()
								.th().content("File")
							._tr()
						._thead()
						.tbody();
			for (File file : files) {
				tbody
					.tr()
						.td().content(file.getName())
					._tr();
			}
			tbody
				._tbody()
				._table()
				._div();
		}
	}

}
