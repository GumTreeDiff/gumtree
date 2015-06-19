package fr.labri.gumtree.matchers;

import fr.labri.gumtree.matchers.heuristic.XyBottomUpMatcher;
import fr.labri.gumtree.matchers.heuristic.cd.ChangeDistillerBottomUpMatcher;
import fr.labri.gumtree.matchers.heuristic.cd.ChangeDistillerLeavesMatcher;
import fr.labri.gumtree.matchers.heuristic.gt.CliqueSubtreeMatcher;
import fr.labri.gumtree.matchers.heuristic.gt.CompleteBottomUpMatcher;
import fr.labri.gumtree.matchers.heuristic.gt.GreedyBottomUpMatcher;
import fr.labri.gumtree.matchers.heuristic.gt.GreedySubtreeMatcher;
import fr.labri.gumtree.tree.ITree;

public class CompositeMatchers {
    @Register(id = "gumtree", defaultMatcher = true)
    public static class ClassicGumtree extends CompositeMatcher {

        public ClassicGumtree(ITree src, ITree dst, MappingStore store) {
            super(src, dst, store, new Matcher[]{
                    new GreedySubtreeMatcher(src, dst, store),
                    new GreedyBottomUpMatcher(src, dst, store)
            });
        }
    }

    @Register(id = "gumtree-complete")
    public static class CompleteGumtreeMatche extends CompositeMatcher {

        public CompleteGumtreeMatche(ITree src, ITree dst, MappingStore store) {
            super(src, dst, store, new Matcher[]{
                    new CliqueSubtreeMatcher(src, dst, store),
                    new CompleteBottomUpMatcher(src, dst, store)
            });
        }
    }

    @Register(id = "change-distiller", defaultMatcher = true)
    public class ChangeDistiller extends CompositeMatcher {

        public ChangeDistiller(ITree src, ITree dst, MappingStore store) {
            super(src, dst, store, new Matcher[]{
                    new ChangeDistillerLeavesMatcher(src, dst, store),
                    new ChangeDistillerBottomUpMatcher(src, dst, store)
            });
        }
    }

    @Register(id = "xy", defaultMatcher = true)
    public static class XyMatcher extends CompositeMatcher {

        public XyMatcher(ITree src, ITree dst, MappingStore store) {
            super(src, dst, store, new Matcher[]{
                    new GreedySubtreeMatcher(src, dst, store),
                    new XyBottomUpMatcher(src, dst, store)
            });
        }
    }
}