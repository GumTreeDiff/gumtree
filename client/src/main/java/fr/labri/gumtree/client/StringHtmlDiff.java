package fr.labri.gumtree.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.rendersnake.HtmlCanvas;

import fr.labri.gumtree.client.ui.web.views.DiffView;

public class StringHtmlDiff {
	
	public String getHtmlOfDiff(String urlFolder, String aContent, String bContent, String aName, String bName) throws Exception {
		
		Path file1 = Files.createTempFile("A", ".java");
		Path file2 = Files.createTempFile("B", ".java");
		Files.write(file1, aContent.getBytes(), StandardOpenOption.WRITE);
		Files.write(file2, bContent.getBytes(), StandardOpenOption.WRITE);
		
		DiffView diffView = new DiffView(file1.toFile(), file2.toFile(), aName, bName);
		diffView.setURLFolder(urlFolder);
		HtmlCanvas html = new HtmlCanvas();
		diffView.renderOn(html);
		return html.toHtml();
	}
}
