package fr.labri.gumtree.client.batch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FileUtils {

	private FileUtils() {
	}

	public static String getExtension(String path) {
		return path.substring(path.lastIndexOf(".") + 1).toLowerCase();
	}

	public static void deleteFolder(String path) {
		deleteFolder(new File(path));
	}

	public static void deleteFolder(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) deleteFolder(new File(dir, children[i]));
		}
		dir.delete();
	}

	public static String createTmpFolder(String prefix, String path) {
		try {
			File folder = null;
			if ("".equals(path)) {
				File fTmpPath = new File(path);
				if (fTmpPath.exists() && fTmpPath.canWrite()) folder = File.createTempFile(prefix, "", fTmpPath);
				else folder = File.createTempFile(prefix, "");
			} else folder = File.createTempFile(prefix, "");
			folder.delete();
			folder.mkdir();
			return folder.getAbsolutePath();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static void listAllFiles(File path, List<String> allFiles, String extension) {
		if (path.isDirectory()) {
			File[] list = path.listFiles();
			if (list != null) for (int i = 0; i < list.length; i++) listAllFiles(list[i], allFiles, extension);
		} else {
			String currentFilePath = path.getAbsolutePath();
			if (currentFilePath.toLowerCase().endsWith(extension)) allFiles.add(currentFilePath);
		}
	}
	
	public static List<File> listAllFiles(File path) {
		List<File> files = new ArrayList<>();
		listAllFiles(path, files);
		Collections.sort(files);
		return files;
	}
	
	public static void listAllFiles(File path, List<File> allFiles) {
		if (path.isDirectory()) {
			File[] list = path.listFiles();
			if (list != null) for (int i = 0; i < list.length; i++) listAllFiles(list[i], allFiles);
		} else allFiles.add(path);
	}

}
