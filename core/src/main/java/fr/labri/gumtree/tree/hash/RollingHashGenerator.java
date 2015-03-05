package fr.labri.gumtree.tree.hash;

import java.util.HashMap;
import java.util.Map;

import fr.labri.gumtree.tree.ITree;
import static fr.labri.gumtree.tree.hash.HashUtils.*;

public abstract class RollingHashGenerator implements HashGenerator {

	public void hash(ITree t) {
		for (ITree n: t.postOrder())
			if (n.isLeaf())
				n.setHash(leafHash(n));
			else
				n.setHash(innerNodeHash(n));
	}
	
	public abstract int hashFunction(String s);
	
	public int leafHash(ITree t) {
		return BASE * hashFunction(t.inSeed()) + hashFunction(t.outSeed());
	}

	public int innerNodeHash(ITree t) {
		int size = t.getSize() * 2 - 1;
		int hash = hashFunction(t.inSeed()) * fpow(BASE, size);
		
		for (ITree c: t.getChildren()) {
			size = size - c.getSize() * 2;
			hash += c.getHash() * fpow(BASE, size);
		}
		
		hash += hashFunction(t.outSeed());
		return hash;
	}

	public static class JavaRollingHashGenerator extends RollingHashGenerator {

		@Override
		public int hashFunction(String s) {
			return s.hashCode();
		}
		
	}

	public static class Md5RollingHashGenerator extends RollingHashGenerator {
		
		@Override
		public int hashFunction(String s) {
			return md5(s);
		}

	}

	public static class RandomRollingHashGenerator extends RollingHashGenerator {

		private final static Map<String, Integer> digests = new HashMap<>();
		
		@Override
		public int hashFunction(String s) {
			return rdmHash(s);
		}

		public static int rdmHash(String s) {
			if (!digests.containsKey(s)) {
				int digest = (int) (Math.random() * (Integer.MAX_VALUE - 1));
				digests.put(s, digest);
				return digest;
			} else return digests.get(s);
		}

	}

}
