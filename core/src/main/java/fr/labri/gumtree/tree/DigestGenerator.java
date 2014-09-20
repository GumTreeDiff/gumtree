package fr.labri.gumtree.tree;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public abstract class DigestGenerator {

	public abstract void computeDigest(ITree tree);

	public static int byteArrayToInt(byte[] b) {
		return   b[3] & 0xFF | (b[2] & 0xFF) << 8 | (b[1] & 0xFF) << 16 | (b[0] & 0xFF) << 24;
	}

	public static int fpow(int a, int b){
		int result = 1;
		while (b > 0) {
			if ((b&1) != 0) result *= a;
			b >>= 1;
			a *= a;
		}
		return result;
	}

	public static int md5digest(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(s.getBytes());
			return byteArrayToInt(digest);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return ITree.NO_VALUE;
	}

	public static class StdHashGenerator extends DigestGenerator {
		@Override
		public void computeDigest(ITree tree) {
			for (ITree t: tree.getTrees()) t.setDigest(t.toDigestTreeString().hashCode());
		}
	}

	public static class Md5HashGenerator extends DigestGenerator {
		@Override
		public void computeDigest(ITree tree) {
			for (ITree t: tree.getTrees()) t.setDigest(md5digest(t.toDigestTreeString()));
		}
	}
	
	public static class RollingStdHashGenerator extends DigestGenerator {
		private final static int B = 33;

		@Override
		public void computeDigest(ITree tree) {
			for(ITree t: tree.postOrder()) {
				if (t.isLeaf()) t.setDigest(t.toDigestString().hashCode());
				else {
					int digest = 0;
					int n = t.getChildren().size();
					for (int i = 0; i < n; i ++) {
						ITree child = t.getChildren().get(i);
						digest = B * digest + t.getChildren().get(i).getDigest() * (int) fpow(B, child.getSize());
					}
					digest = B * digest + t.toDigestString().hashCode();
					t.setDigest(digest);
				}
			}
		}
	}

	public static class RollingMd5HashGenerator extends DigestGenerator {
		private final static int B = 33;

		@Override
		public void computeDigest(ITree tree) {
			for(ITree t: tree.postOrder()) {
				if (t.isLeaf()) t.setDigest(md5digest(t.toDigestString()));
				else {
					int digest = 0;
					int n = t.getChildren().size();
					for (int i = 0; i < n; i ++) {
						ITree child = t.getChildren().get(i);
						digest = B * digest + t.getChildren().get(i).getDigest() * (int) fpow(B, child.getSize());
					}
					digest = B * digest + md5digest(t.toDigestString());
					t.setDigest(digest);
				}
			}
		}
	}
	
	public static class RollingRdmHashGenerator extends DigestGenerator {
		private final static int B = 33;
		
		private final static Map<String, Integer> digests = new HashMap<>();

		@Override
		public void computeDigest(ITree tree) {
			for(ITree t: tree.postOrder()) {
				if (t.isLeaf()) t.setDigest(rdmDigest(t.toDigestString()));
				else {
					int digest = 0;
					int n = t.getChildren().size();
					for (int i = 0; i < n; i ++) {
						ITree child = t.getChildren().get(i);
						digest = B * digest + t.getChildren().get(i).getDigest() * (int) fpow(B, child.getSize());
					}
					digest = B * digest + rdmDigest(t.toDigestString());
					t.setDigest(digest);
				}
			}
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
