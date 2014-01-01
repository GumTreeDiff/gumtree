package fr.labri.gumtree.client.ui.web.views;

import static org.rendersnake.HtmlAttributesFactory.*;

import java.io.IOException;

import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

public class BootstrapHeader implements Renderable {

	@Override
	public void renderOn(HtmlCanvas html) throws IOException {
		html
		.head()
			.meta(charset("utf8"))
			.meta(name("viewport").content("width=device-width, initial-scale=1.0"))
			.title().content("GumTree")
			.macros().stylesheet("res/web/bootstrap.min.css")
			.macros().stylesheet("res/web/gumtree.css")
		._head();
	}

}
