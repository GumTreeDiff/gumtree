package fr.labri.gumtree.tree.hash;

import fr.labri.gumtree.tree.ITree;

import static fr.labri.gumtree.tree.hash.HashUtils.*;

public abstract class StaticHashGenerator implements HashGenerator {

	public void hash(ITree t) {
		for (ITree n: t.postOrder())
			n.setDigest(nodeHash(n));
	}

	public abstract int nodeHash(ITree t);


	public static class StdHashGenerator extends StaticHashGenerator {
		
		@Override
		public int nodeHash(ITree t) {
			return t.toDigestTreeString().hashCode();
		}
		
	}

	public static class Md5HashGenerator extends StaticHashGenerator {
		
		@Override
		public int nodeHash(ITree t) {
			return md5(t.toDigestTreeString());
		}
		
	}

}
