package fr.labri.gumtree.io;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import fr.labri.gumtree.actions.Action;

public final class SerializeEditScript {
	
	private SerializeEditScript() {
	}

	public static String toText(List<Action> script) {
		StringWriter w = new StringWriter();
		for (Action a: script) w.append(a.toString() + "\n");
		String result = w.toString();
		try {
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public static void toText(List<Action> script, String file) {
		try {
			FileWriter w = new FileWriter(file);
			for (Action a : script) w.append(a.toString() + "\n");
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
