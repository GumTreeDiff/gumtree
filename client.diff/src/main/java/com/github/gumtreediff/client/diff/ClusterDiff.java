package com.github.gumtreediff.client.diff;

import com.github.gumtreediff.actions.ActionClusterFinder;
import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.client.Register;
import com.github.gumtreediff.matchers.Matcher;

import java.util.List;
import java.util.Set;

@Register(name = "cluster", description = "Extract action clusters",
        options = AbstractDiffClient.Options.class)
public class ClusterDiff extends AbstractDiffClient<AbstractDiffClient.Options> {

    public ClusterDiff(String[] args) {
        super(args);
    }

    @Override
    public void run() {
        Matcher m = matchTrees();
        ActionGenerator g = new ActionGenerator(getSrcTreeContext().getRoot(),
                getDstTreeContext().getRoot(), m.getMappings());
        g.generate();
        List<Action> actions = g.getActions();
        ActionClusterFinder f = new ActionClusterFinder(getSrcTreeContext(), getDstTreeContext(), actions);
        for(Set<Action> cluster: f.getClusters()) {
            System.out.println("New cluster:");
            System.out.println(f.getClusterLabel(cluster));
            System.out.println("------------");
            for (Action a: cluster)
                System.out.println(a.format(getSrcTreeContext()));
            System.out.println("");
        }
    }

    @Override
    protected Options newOptions() {
        return new Options();
    }
}
