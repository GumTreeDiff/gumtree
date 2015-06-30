package com.github.gumtreediff.tree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class Tree extends AbstractTree implements ITree {

    // Type of the token
    int type;

    // Label of the token
    String label;

    // Begin position of the tree in terms of absolute character index
    int pos;
    int length;
    // End position

    // Begin position in terms of line and column start and end
    private int[] lcPosStart;
    private int[] lcPosEnd;
    // End position

    // Needed for RTED :(
    private Object tmpData;

    private AssociationMap metadata;

    Tree(int type, String label) {
        this.type = type;
        this.label = (label == null) ? NO_LABEL : label.intern();
        this.id = NO_ID;
        this.depth = NO_VALUE;
        this.hash = NO_VALUE;
        this.height = NO_VALUE;
        this.depth = NO_VALUE;
        this.size = NO_VALUE;
        this.pos = NO_VALUE;
        this.length = NO_VALUE;
        this.matched = false;
        this.children = new ArrayList<>();
    }

    // Only used for cloning ...
    private Tree(Tree other) {
        this.type = other.type;
        this.label = other.getLabel();

        this.id = other.getId();
        this.matched = other.isMatched();
        this.pos = other.getPos();
        this.length = other.getLength();
        this.height = other.getHeight();
        this.size = other.getSize();
        this.depth = other.getDepth();
        this.hash = other.getHash();
        this.depth = other.getDepth();
        this.tmpData = other.getTmpData();
        this.children = new ArrayList<>();
        this.metadata = other.metadata;
    }

    @Override
    public void addChild(ITree t) {
        children.add(t);
        t.setParent(this);
    }

    @Override
    public Tree deepCopy() {
        Tree copy = new Tree(this);
        for (ITree child : getChildren())
            copy.addChild(child.deepCopy());
        return copy;
    }

    @Override
    public List<ITree> getChildren() {
        return children;
    }

    @Override
    public int getEndPos() {
        return pos + length;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public int[] getLcPosEnd() {
        return lcPosEnd;
    }

    @Override
    public int[] getLcPosStart() {
        return lcPosStart;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public ITree getParent() {
        return parent;
    }

    @Override
    public int getPos() {
        return pos;
    }

    @Override
    public Object getTmpData() {
        return tmpData;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public void setChildren(List<ITree> children) {
        this.children = children;
        for (ITree c : children)
            c.setParent(this);
    }

    @Override
    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public void setLcPosEnd(int[] lcPosEnd) {
        this.lcPosEnd = lcPosEnd;
    }

    @Override
    public void setLcPosStart(int[] lcPosStart) {
        this.lcPosStart = lcPosStart;
    }

    @Override
    public void setLength(int length) {
        this.length = length;
    }

    @Override
    public void setParent(ITree parent) {
        this.parent = parent;
    }

    @Override
    public void setParentAndUpdateChildren(ITree parent) {
        if (this.parent != null) this.parent.getChildren().remove(this);
        this.parent = parent;
        if (this.parent != null) parent.getChildren().add(this);
    }

    @Override
    public void setPos(int pos) {
        this.pos = pos;
    }

    @Override
    public void setTmpData(Object tmpData) {
        this.tmpData = tmpData;
    }

    @Override
    public void setType(int type) {
        this.type = type;
    }

    @Override
    public Object getMetadata(String key) {
        if (metadata == null)
            return null;
        return metadata.get(key);
    }

    @Override
    public Object setMetadata(String key, Object value) {
        if (value == null) {
            if (metadata == null)
                return null;
            else
                return metadata.remove(key);
        }
        if (metadata == null)
            metadata = new AssociationMap();
        return metadata.set(key, value);
    }

    @Override
    public Iterator<Entry<String, Object>> getMetadata() {
        if (metadata == null)
            return new EmptyEntryIterator();
        return metadata.iterator();
    }
}
