package fr.labri.gumtree.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.rendersnake.HtmlCanvas;

import fr.labri.gumtree.client.ui.web.views.DiffView;

public class StringHtmlDiff {
	
	public static void main(String args[]) throws Exception {
		
		Path file1 = Files.createTempFile("", ".java");
		Path file2 = Files.createTempFile("", ".java");
		Files.write(file1, "public class A{public void m()}".getBytes(), StandardOpenOption.WRITE);
		Files.write(file2, "public class B{public void m()}".getBytes(), StandardOpenOption.WRITE);
		
		DiffView diffView = new DiffView(file1.toFile(), file2.toFile());
		HtmlCanvas html = new HtmlCanvas();
		diffView.renderOn(html);
		System.out.println(html.toHtml());
	}
}
