package fr.labri.gumtree.client.ui.web.views;

import static org.rendersnake.HtmlAttributesFactory.*;

import java.io.IOException;

import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

public class BootstrapHeader implements Renderable {
	
	private String urlFolder;

	public BootstrapHeader() {
		this("");
	}
	
	public BootstrapHeader(String urlFolder) {
		this.urlFolder = urlFolder;
	}

	@Override
	public void renderOn(HtmlCanvas html) throws IOException {
		html
		.head()
			.meta(charset("utf8"))
			.meta(name("viewport").content("width=device-width, initial-scale=1.0"))
			.title().content("GumTree")
			.macros().stylesheet("https://maxcdn.bootstrapcdn.com/bootstrap/3.3.4/css/bootstrap.min.css")
			.macros().stylesheet("/" + urlFolder +  "/res/web/gumtree.css")
		._head();
	}

}
