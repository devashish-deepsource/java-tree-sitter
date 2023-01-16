package mytree;

import ai.serenade.treesitter.Node;

import java.util.ArrayList;
import java.util.List;

public class MyNode {
    private Node internalNode;
    private MyNode parent;
    private final List<MyNode> children = new ArrayList<>();
    private Span span;

    private boolean isDelete;

    public boolean isDeleted() {
        return isDelete;
    }

    public MyNode(Node internalNode, MyNode parent) {
        this.parent = parent;
        this.internalNode = internalNode;
        this.span = new Span(internalNode.getRange());
    }

    public MyNode(Node internalNode) {
        this(internalNode, null);
    }

    public void setDeleted(boolean deleted) {
        isDelete = deleted;
    }

    public void appendChild(MyNode child) {
        children.add(child);
    }

    public MyNode parent() {
        return parent;
    }

    public List<MyNode> children() {
        return children;
    }

    public boolean isLeaf() {
        return children.size() == 0;
    }

    public Node getInternalNode() {
        return internalNode;
    }

    @Override
    public String toString() {
        String leafOrNoLeaf = isLeaf() ? " (Leaf)" : "";
        return String.format("Type: %s, Range: %s%s", internalNode.getType(), internalNode.getRange(), leafOrNoLeaf);
    }

    public void resetSpan() {
        span.reset();
    }

    public Span span() {
        return span;
    }
}


