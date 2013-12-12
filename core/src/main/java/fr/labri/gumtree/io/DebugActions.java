package fr.labri.gumtree.io;

import java.util.List;

import fr.labri.gumtree.actions.model.Action;
import fr.labri.gumtree.actions.model.Move;
import fr.labri.gumtree.tree.Tree;

public final class DebugActions {
	
	private DebugActions() {
	}
	
	public static void debugNestedMoves(List<Action> actions) {
		for (Action a: actions) {
			if (a instanceof Move) {
				for (Tree c: a.getNode().getDescendants()) {
					for (Action aa: actions) {
						if (a != aa && aa instanceof Move && aa.getNode() == c) {
							System.out.println("Nested move on " + a.getNode().toString() + " by " + c.toString());
						}
					}
				}
			}
		}
	}

}
