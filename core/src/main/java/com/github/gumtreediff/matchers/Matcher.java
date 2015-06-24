package com.github.gumtreediff.matchers;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.ITree;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public abstract class Matcher {

    public static final Logger LOGGER = Logger.getLogger("com.github.gumtreediff.matchers");

    protected final ITree src;

    protected final ITree dst;

    protected final MappingStore mappings;

    public Matcher(ITree src, ITree dst, MappingStore store) {
        this.src = src;
        this.dst = dst;
        this.mappings = store;
    }

    public abstract void match();

    public MappingStore getMappings() {
        return mappings;
    }

    public Set<Mapping> getMappingSet() {
        return mappings.asSet();
    }

    public ITree getSrc() {
        return src;
    }

    public ITree getDst() {
        return dst;
    }

    protected void addMapping(ITree src, ITree dst) {
        src.setMatched(true);
        dst.setMatched(true);
        mappings.link(src, dst);
    }

    protected void addFullMapping(ITree src, ITree dst) {
        List<ITree> csrcs = src.getTrees();
        List<ITree> cdsts = dst.getTrees();
        for (int i = 0; i < csrcs.size(); i++) {
            ITree csrc = csrcs.get(i);
            ITree cdst = cdsts.get(i);
            addMapping(csrc, cdst);
        }
    }

    protected double chawatheSimilarity(ITree src, ITree dst) {
        int max = Math.max(src.getDescendants().size(), dst.getDescendants().size());
        return (double) numberOfCommonDescendants(src, dst) / (double) max;
    }

    protected double diceSimilarity(ITree src, ITree dst) {
        double c = (double) numberOfCommonDescendants(src, dst);
        return (2D * c) / ((double) src.getDescendants().size() + (double) dst.getDescendants().size());
    }

    protected double jaccardSimilarity(ITree src, ITree dst) {
        double num = (double) numberOfCommonDescendants(src, dst);
        double den = (double) src.getDescendants().size() + (double) dst.getDescendants().size() - num;
        return num / den;
    }

    protected int numberOfCommonDescendants(ITree src, ITree dst) {
        Set<ITree> dstDescs = new HashSet<>(dst.getDescendants());
        int common = 0;

        for (ITree t : src.getDescendants()) {
            ITree m = mappings.getDst(t);
            if (m != null && dstDescs.contains(m))
                common++;
        }

        return common;
    }

    protected void clean() {
        for (ITree t : src.getTrees())
            if (!mappings.hasSrc(t))
                t.setMatched(false);
        for (ITree t : dst.getTrees())
            if (!mappings.hasDst(t))
                t.setMatched(false);
    }
}
