package mytree;

import ai.serenade.treesitter.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MyNode {
    private Node internalNode;
    private MyNode parent;
    List<MyNode> children = new ArrayList<>();
    private Span span;

    private boolean isDeleted;

    private final String refString;

    public boolean isDeleted() {
        return isDeleted;
    }

    public MyNode(Node internalNode, MyNode parent, String refString) {
        this.parent = parent;
        this.refString = refString;
        this.internalNode = internalNode;
        this.span = new Span(internalNode.getRange());
    }

    private void setSpanRecursiveHelper(MyNode node, Span newSpan) {
        for (var child : node.children) {
            child.span = newSpan;
            child.setSpanRecursiveHelper(child, newSpan);
        }
    }

    public void setSpanRecursive(Span span) {
        setSpanRecursiveHelper(this, span);
    }

    public String refString() {
        return refString;
    }

    public MyNode(Node internalNode, String refString) {
        this(internalNode, null, refString);
    }

    private void setDeletedHelper(MyNode parent, boolean deleted) {
        parent.isDeleted = deleted;
        if (parent.isLeaf())
            return;
        for (var child : parent.children)
            setDeletedHelper(child, deleted);
    }

    public void setDeleted(boolean deleted) {
        setDeletedHelper(this, deleted);
    }

    public void appendChild(MyNode child) {
        children.add(child);
    }

    public MyNode parent() {
        return parent;
    }

    public void setParent(MyNode parent) {
        this.parent = parent;
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
        String position = String.format("[(%d, %d), (%d, %d)]", span.startRow(), span.startCol(), span.endRow(), span.endCol());
        return String.format("Type: %s, Range: %s%s", internalNode.getType(), position, leafOrNoLeaf);
    }

    public void resetSpan() {
        span.reset();
    }

    public Span span() {
        return span;
    }
}


