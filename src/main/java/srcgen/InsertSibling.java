package srcgen;

import mytree.MyNode;
import mytree.Span;
import mytree.TreeWrapper;

import java.util.List;
import java.util.Objects;

public class InsertSibling extends Action {
    private final Span span;
    private final MyNode nodeToInsert;
    private final boolean shouldInsertBefore;
    public InsertSibling(TreeWrapper tree, Span refSpan, MyNode nodeToInsert, boolean shouldInsertBefore) {
        super(tree);
        Objects.requireNonNull(refSpan);
        Objects.requireNonNull(nodeToInsert);
        this.span = refSpan;
        this.nodeToInsert = nodeToInsert;
        this.shouldInsertBefore = shouldInsertBefore;
    }

    public MyNode getWhitespaceNode(List<MyNode> childList, int refNodeIndex) {
        // Prevent underflow.
        if (refNodeIndex == 0)
            return null;
        MyNode prevNode = childList.get(refNodeIndex - 1);
        boolean isWhitespace = prevNode.getInternalNode().getType().equals("ws");
        return isWhitespace ? prevNode : null;
    }

    private MyNode getNewlineNode(List<MyNode> childList, int refNodeIndex) {
        for (int i = refNodeIndex - 1; i >= 0; i--) {
            var thisNode = childList.get(i);
            var type = thisNode.getInternalNode().getType();
            if (type.equals("newline"))
                return thisNode;
            if (!type.equals("ws"))
                return  null;
        }
        return null;
    }

    @Override
    public void apply() {
        var refNode = tree.nodeAtSpan(span);
        var parent = refNode.parent();
        int indexOfRefNode = parent.children().indexOf(refNode);
        Objects.requireNonNull(parent);
        nodeToInsert.setParent(parent);
        int insertPos = shouldInsertBefore ? indexOfRefNode : indexOfRefNode + 1;

        MyNode wsNode = getWhitespaceNode(parent.children(), indexOfRefNode);
        if (wsNode != null) {
            // Add the whitespace right before the refNode.
            parent.children().add(insertPos, wsNode);
        }

        MyNode nlNode = getNewlineNode(parent.children(), indexOfRefNode);
        if (nlNode != null) {
            // Add the newline right before the refNode.
            parent.children().add(insertPos, nlNode);
        }

        parent.children().add(insertPos, nodeToInsert);
    }
}
