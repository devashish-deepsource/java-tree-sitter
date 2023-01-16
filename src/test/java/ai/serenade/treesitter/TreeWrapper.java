package ai.serenade.treesitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class MyNode {
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

public class TreeWrapper {
    private final MyNode root;

    public TreeWrapper(Tree sitterTree) {
        var rootNode = sitterTree.getRootNode();
        root = new MyNode(rootNode, null);
        buildTree(root, rootNode);
    }

    private MyNode lookupNodeBySpan(MyNode current, Span span) {
        if (current.span().equals(span))
            return current;

        if (current.span().contains(span)) {
            var children = current.children();
            for (var child : children) {
                if (child.span().contains(span)) {
                    return lookupNodeBySpan(child, span);
                }
            }
        }

        throw new RuntimeException("Shouldn't happen");
    }

    public MyNode nodeAtSpan(Span span) {
        var root = root();
        return lookupNodeBySpan(root, span);
    }

    public MyNode root() {
        return root;
    }

    private void buildTree(MyNode wrapperParent, Node nodeParent) {
        if (nodeParent.isNull())
            return;
        var childCount = nodeParent.getChildCount();
        for (var i = 0; i < childCount; i++) {
            var currentChild = nodeParent.getChild(i);
            MyNode childNode = new MyNode(currentChild, wrapperParent);
            wrapperParent.appendChild(childNode);
            buildTree(childNode, currentChild);
        }
    }

    private String toStringHelper(MyNode current, StringBuilder builder) {
        builder.append(current.toString());
        builder.append("\n");
        for (var child : current.children())
            toStringHelper(child, builder);
        return builder.toString();
    }

    @Override
    public String toString() {
        return toStringHelper(root, new StringBuilder());
    }

    public String generateSource(String refSourcePath) {
        try {
            var sourceGenerator = new SourceGenerator(refSourcePath);
            return sourceGenerator.generate(this);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }
}
