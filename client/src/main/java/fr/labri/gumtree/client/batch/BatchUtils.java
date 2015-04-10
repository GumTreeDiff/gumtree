package fr.labri.gumtree.client.batch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import fr.labri.gumtree.tree.Pair;

public class BatchUtils {
	
	private BatchUtils() {};
	
	public static Set<Pair<File, File>> getFilePairs(File dir) {
		Set<Pair<File, File>> result = new HashSet<>();
		
		if (!dir.isDirectory())
			return result;
		
		for(File f: dir.listFiles())
			if (f.isFile() && f.getName().contains("_v0")) {
				File dst = new File(f.getAbsolutePath().replace("_v0.", "_v1."));
				result.add(new Pair<File, File>(f, dst));
			}
				
		return result;
	}
	
	public static void ensureFolder(String folder) {
		File f = new File(folder);
		if (f.exists() && !f.isDirectory()) f.delete();
		f.mkdirs();
	}
	
	public static void copyResource(String url, String dest) {
		try {
			InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(url);
			FileOutputStream out = new FileOutputStream(dest);
			int read = 0;
			byte[] bytes = new byte[1024];
			while ((read = in.read(bytes)) != -1)
				out.write(bytes, 0, read);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String folderify(String s) {
		return s.replaceAll("[^A-Za-z0-9-_]", "_");
	}
	

}
