package com.github.gumtreediff.tree;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TreeMetricsProviderFactory implements MetricProviderFactory<TreeMetricsProviderFactory.TreeMetrics> {
    public static final String ENTER = "enter";
    public static final String LEAVE = "leave";
    public static final int BASE = 33;

    private static int hashFactor(int exponent) {
        return fastExponentiation(BASE, exponent);
    }

    private static int fastExponentiation(int base, int exponent) {
        if (exponent == 0)
            return 1;
        if (exponent == 1)
            return base;
        int result = 1;
        while (exponent > 0) {
            if ((exponent & 1) != 0)
                result *= base;
            exponent >>= 1;
            base *= base;
        }
        return result;
    }

    @Override
    public TreeMetricsProvider computeMetric(TreeContext context) {
        return new TreeMetricsProvider(context.getRoot());
    }

    public static class TreeMetricsProvider implements MetricProvider<TreeMetrics> {
        private Map<ITree, TreeMetrics> metricsForTree;

        public TreeMetricsProvider(ITree root) {
            metricsForTree = new HashMap<>();
            TreeVisitor.visitTree(root, new TreeMetricComputer());
        }

        public TreeMetrics get(ITree tree) {
            return metricsForTree.get(tree);
        }

        private class TreeMetricComputer extends TreeVisitor.InnerNodesAndLeavesVisitor {


            int currentDepth = 0;

            @Override
            public void startInnerNode(ITree tree) {
                currentDepth++;
            }

            @Override
            public void visitLeave(ITree tree) {
                metricsForTree.put(tree, new TreeMetrics(1, 0, leafHash(tree), currentDepth));
            }

            @Override
            public void endInnerNode(ITree tree) {
                currentDepth--;
                int sumSize = 0;
                int maxHeight = 0;
                int currentHash = 0;
                for (ITree child : tree.getChildren()) {
                    TreeMetrics metrics = metricsForTree.get(child);
                    int exponent = 2 * sumSize + 1;
                    currentHash += metrics.hash * hashFactor(exponent);
                    sumSize += metrics.size;
                    if (metrics.height > maxHeight)
                        maxHeight = metrics.height;
                }
                metricsForTree.put(tree, new TreeMetrics(
                        sumSize + 1,
                        maxHeight + 1,
                        innerNodeHash(tree, 2 * sumSize + 1, currentHash),
                        currentDepth));
            }

            private int innerNodeHash(ITree tree, int size, int middleHash) {
                return Objects.hash(tree.getType(), tree.getLabel(), ENTER)
                        + middleHash
                        + Objects.hash(tree.getType(), tree.getLabel(), LEAVE) * hashFactor(size);
            }

            private int leafHash(ITree tree) {
                return innerNodeHash(tree, 1, 0);
            }
        }
    }

    public static class TreeMetrics {
        public final int size;

        public final int height;

        public final int hash;

        public final int depth; //TODO try to remove this redundant metric

        public TreeMetrics(int size, int height, int hash, int depth) {
            this.size = size;
            this.height = height;
            this.hash = hash;
            this.depth = depth;
        }
    }
}
