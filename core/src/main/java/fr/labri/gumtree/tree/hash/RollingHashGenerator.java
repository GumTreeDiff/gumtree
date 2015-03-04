package fr.labri.gumtree.tree.hash;

import java.util.HashMap;
import java.util.Map;

import fr.labri.gumtree.tree.ITree;

import static fr.labri.gumtree.tree.hash.HashUtils.*;

public abstract class RollingHashGenerator implements HashGenerator {

	public void hash(ITree t) {
		for (ITree n: t.postOrder())
			if (n.isLeaf())
				n.setDigest(leafHash(n));
			else
				n.setDigest(innerNodeHash(n));
	}

	public abstract int leafHash(ITree t);

	public int innerNodeHash(ITree t) {
		int digest = 0;
		for(ITree c : t.getChildren())
			digest = BASE * digest + c.getDigest() * (int) fpow(BASE, c.getSize());
		return BASE * digest + leafHash(t);
	}

	public static class JavaRollingHashGenerator extends RollingHashGenerator {

		@Override
		public int leafHash(ITree t) {
			return t.toDigestString().hashCode();
		}
		
	}

	public static class Md5RollingHashGenerator extends RollingHashGenerator {
		
		@Override
		public int leafHash(ITree t) {
			return md5(t.toDigestString());
		}

	}

	public static class RandomRollingHashGenerator extends RollingHashGenerator {

		private final static Map<String, Integer> digests = new HashMap<>();
		
		@Override
		public int leafHash(ITree t) {
			return rdmDigest(t.toDigestString());
		}

		public static int rdmDigest(String s) {
			if (!digests.containsKey(s)) {
				int digest = (int) (Math.random() * (Integer.MAX_VALUE - 1));
				digests.put(s, digest);
				return digest;
			} else return digests.get(s);
		}

	}

}
