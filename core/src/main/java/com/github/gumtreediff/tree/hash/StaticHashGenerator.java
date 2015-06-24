package com.github.gumtreediff.tree.hash;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.ITree;

import static com.github.gumtreediff.tree.hash.HashUtils.*;

public abstract class StaticHashGenerator implements HashGenerator {

    public void hash(ITree t) {
        for (ITree n: t.postOrder())
            n.setHash(nodeHash(n));
    }

    public abstract int nodeHash(ITree t);

    public static class StdHashGenerator extends StaticHashGenerator {

        @Override
        public int nodeHash(ITree t) {
            return t.toStaticHashString().hashCode();
        }

    }

    public static class Md5HashGenerator extends StaticHashGenerator {

        @Override
        public int nodeHash(ITree t) {
            return HashUtils.md5(t.toStaticHashString());
        }

    }

}
