package com.github.gumtreediff.gen.treesitterng;

import org.treesitter.TSNode;

import java.util.ArrayList;

public class MockTypeOnlyTreeSitterNode extends TSNode {
    private static final MockTypeOnlyTreeSitterNode NULL_NODE = new MockTypeOnlyTreeSitterNode();
    private String type = null;

    private MockTypeOnlyTreeSitterNode parent = NULL_NODE;

    private ArrayList<MockTypeOnlyTreeSitterNode> children = new ArrayList<>();

    public void setType(String type) {
        this.type = type;
    }

    public void setParent(MockTypeOnlyTreeSitterNode parent) {
        this.parent = parent;
    }

    public void setChildren(ArrayList<MockTypeOnlyTreeSitterNode> children) {
        children.forEach((child) -> child.setParent(this));
        this.children = children;
    }

    public void addChild(MockTypeOnlyTreeSitterNode child) {
        child.setParent(this);
        children.add(child);
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public MockTypeOnlyTreeSitterNode getParent() {
        return parent;
    }

    @Override
    public TSNode getChild(int index) {
        return children.get(index);
    }

    @Override
    public int getChildCount() {
        return children.size();
    }

    @Override
    public boolean isNull() {
        return type == null;
    }

    @Override
    public String toString() {
        return "MockTypeOnlyTreeSitterNode{" +
                "type='" + type + '\'' +
                '}';
    }
}
