package fr.labri.gumtree.gen.jdt;

import java.util.ArrayDeque;
import java.util.Deque;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;

import fr.labri.gumtree.gen.jdt.cd.EntityType;
import fr.labri.gumtree.tree.ITree;
import fr.labri.gumtree.tree.TreeContext;

public abstract class AbstractJdtVisitor  extends ASTVisitor {

    protected TreeContext context = new TreeContext();

    private Deque<ITree> trees = new ArrayDeque<>();

    public TreeContext getTreeContext() {
        return context;
    }

    protected void pushNode(ASTNode n, String label) {
        int type = n.getNodeType();
        String typeName = n.getClass().getSimpleName();
        push(type, typeName, label, n.getStartPosition(), n.getLength());
    }

    protected void pushFakeNode(EntityType n, int startPosition, int length) {
        int type = -n.ordinal(); // Fake types have negative types (but does it matter ?)
        String typeName = n.name();
        push(type, typeName, "", startPosition, length);
    }

    private void push(int type, String typeName, String label, int startPosition, int length) {
        ITree t = context.createTree(type, label, typeName);
        t.setPos(startPosition);
        t.setLength(length);

        if (trees.isEmpty())
            context.setRoot(t);
        else {
            ITree parent = trees.peek();
            t.setParentAndUpdateChildren(parent);
        }

        trees.push(t);
    }

    protected ITree getCurrentParent() {
        return trees.peek();
    }

    protected void popNode() {
        trees.pop();
    }
}
