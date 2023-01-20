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

    public MyNode getWhitespaceNodeBeforeRef(List<MyNode> childList, int refNodeIndex) {
        // Prevent underflow.
        if (refNodeIndex == 0)
            return null;
        MyNode prevNode = childList.get(refNodeIndex - 1);
        boolean isWhitespace = prevNode.getInternalNode().getType().equals("ws");
        return isWhitespace ? prevNode : null;
    }

    private MyNode getNewlineNodeBeforeRef(List<MyNode> childList, int refNodeIndex) {
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

    private MyNode getWhitespaceNodeAfterRef(List<MyNode> childList, int refNodeIndex) {
        // Prevent overflow.
        if (refNodeIndex == childList.size() - 1)
            return null;
        MyNode nextNode = childList.get(refNodeIndex + 1);
        boolean isWhitespace = nextNode.getInternalNode().getType().equals("ws");
        return isWhitespace ? nextNode : null;
    }

    private MyNode getNewlineNodeAfterRef(List<MyNode> childList, int refNodeIndex) {
        for (int i = refNodeIndex + 1; i < childList.size(); i++) {
            var thisNode = childList.get(i);
            var type = thisNode.getInternalNode().getType();
            if (type.equals("newline"))
                return thisNode;
            if (!type.equals("ws"))
                return null;
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

        if (shouldInsertBefore) {
            MyNode wsNode = getWhitespaceNodeBeforeRef(parent.children(), indexOfRefNode);
            if (wsNode != null) {
                // Add the whitespace right before the refNode.
                parent.children().add(insertPos, wsNode);
            }

            MyNode nlNode = getNewlineNodeBeforeRef(parent.children(), indexOfRefNode);
            if (nlNode != null) {
                // Add the newline right before the refNode.
                parent.children().add(insertPos, nlNode);
            }
        } else {
            MyNode nlNode = getNewlineNodeAfterRef(parent.children(), indexOfRefNode);
            if (nlNode != null) {
                // Add the newline right before the refNode.
                parent.children().add(insertPos, nlNode);
                insertPos++;
            }

            // This is to fix the indent. Ideally, we should also look for whitespaces that come after the ref node on the same line.
            MyNode wsNode = getWhitespaceNodeBeforeRef(parent.children(), indexOfRefNode);
            if (wsNode != null) {
                // Add the whitespace right before the refNode.
                parent.children().add(insertPos, wsNode);
                insertPos++;
            }
        }

        parent.children().add(insertPos, nodeToInsert);
    }
}
