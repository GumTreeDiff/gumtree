package fr.labri.gumtree.io;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.labri.gumtree.tree.Pair;

public class DirectoryComparator {

	private Path src;

	public Path getSrc() {
		return src;
	}

	public Path getDst() {
		return dst;
	}

	private Path dst;
	
	private List<Pair<File, File>> modifiedFiles;

	private Set<File> deletedFiles;

	private  Set<File> addedFiles;
	
	private boolean dirMode = true;

	public DirectoryComparator(String src, String dst) {
		modifiedFiles = new ArrayList<>();
		addedFiles = new HashSet<>();
		deletedFiles =  new HashSet<>();
		this.src = Paths.get(src);
		this.dst = Paths.get(dst);
		if (!Files.exists(this.src) || !Files.exists(this.dst))
			throw new RuntimeException();
		else {
			if (!Files.isDirectory(this.src) && !Files.isDirectory(this.dst)) {
				this.modifiedFiles.add(new Pair<File, File>(this.src.toFile(), this.dst.toFile()));
				this.src = this.src.getParent();
				this.dst = this.dst.getParent();
				this.dirMode = false;
			} else if (!(Files.isDirectory(this.src) && Files.isDirectory(this.dst))) {
				throw new RuntimeException();
			}
				
		}
	}

	public void compare() {
		if (!dirMode) return;
		AllFilesVisitor vSrc = new AllFilesVisitor(src);
		AllFilesVisitor vDst = new AllFilesVisitor(dst);
		try {
			Files.walkFileTree(src, vSrc);
			Files.walkFileTree(dst, vDst);

			Set<String> addedFiles = new HashSet<>();
			addedFiles.addAll(vDst.files);
			addedFiles.removeAll(vSrc.files);
			for (String file : addedFiles) this.addedFiles.add(toDstFile(file));

			Set<String> deletedFiles = new HashSet<>();
			deletedFiles.addAll(vSrc.files);
			deletedFiles.removeAll(vDst.files);
			for (String file : deletedFiles) this.deletedFiles.add(toSrcFile(file));

			Set<String> commonFiles = new HashSet<>();
			commonFiles.addAll(vSrc.files);
			commonFiles.retainAll(vDst.files);
			
			for (String file : commonFiles) 
				if (hasChanged(file, file))
					modifiedFiles.add(new Pair<File, File>(toSrcFile(file), toDstFile(file)));
		} catch (IOException e) {

		}
	}
	
	public boolean isDirMode() {
		return dirMode;
	}
	
	public List<Pair<File, File>> getModifiedFiles() {
		return modifiedFiles;
	}

	public Set<File> getDeletedFiles() {
		return deletedFiles;
	}

	public Set<File> getAddedFiles() {
		return addedFiles;
	}

	private File toSrcFile(String s) {
		return new File(src.toFile(), s);
	}
	
	private File toDstFile(String s) {
		return new File(dst.toFile(), s);
	}

	public boolean hasChanged(String s1, String s2) throws IOException {
		File f1 = toSrcFile(s1);
		File f2 = toDstFile(s2);
		long l1 = Files.size(f1.toPath());
		long l2 = Files.size(f2.toPath());
		if (l1 != l2) return true;
		else {
			FileInputStream fis1 = new FileInputStream(f1);
			DataInputStream dis1 = new DataInputStream(fis1);
			FileInputStream fis2 = new FileInputStream(f2);
			DataInputStream dis2 = new DataInputStream(fis2);

			int c1, c2;
			while ((c1 = dis1.read()) != -1) {
				c2 = dis2.read();
				if (c1 != c2) {
					dis1.close();
					dis2.close();
					return true;
				}
			}

			dis1.close();
			dis2.close();

			return false;
		}
	}

	public static class AllFilesVisitor extends SimpleFileVisitor<Path> {

		private Set<String> files = new HashSet<>();
		
		private Path root;
		
		public AllFilesVisitor(Path root) {
			this.root = root;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (!file.getFileName().startsWith("."))
				files.add(root.relativize(file).toString());
			return FileVisitResult.CONTINUE;
		}
		
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
			return (dir.getFileName().toString().startsWith(".")) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
		}

	}

}
