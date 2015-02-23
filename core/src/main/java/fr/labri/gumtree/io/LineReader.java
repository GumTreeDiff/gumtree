package fr.labri.gumtree.io;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;

public class LineReader extends Reader {
	private Reader reader;
	int currentPos = 0;
	
	ArrayList<Integer> lines = new ArrayList<>();
	
	public LineReader(Reader parent) {
		reader = parent;
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		reader.read(cbuf, off, len);
		for (int i = 0; i < len; i ++)
			if (cbuf[off + i] == '\n')
				lines.add(currentPos + i);
				
		currentPos += len;
		return 0;
	}

	public int positionFor(int line, int column) {
		return lines.get(line) + column;
	}

//	public int[] positionFor(int offset) {
//		Arrays.binarySearch(lines., null, null)
//	}
	
	@Override
	public void close() throws IOException {
	}
}
