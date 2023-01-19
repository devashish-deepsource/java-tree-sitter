package srcgen;

import mytree.MergeNode;
import mytree.MyNode;
import mytree.Span;
import mytree.TreeWrapper;

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

    @Override
    public void apply() {
        var refNode = tree.nodeAtSpan(span);
        var parent = refNode.parent();
        int indexOfRefNode = parent.children().indexOf(refNode);

        if (nodeToInsert instanceof MergeNode mergeNode && !shouldInsertBefore) {
            // Fixme: This is waiting to blow up. What if refNode is the last child of this parent.
            //  Btw, the reason we are trying to find the span of the next sibling is because eventually what we'll do to replace
            //   the spans of the partial tree node with the "correct" one is just assign the span of the node that it is replacing in the main-tree.
            //   If we're inserting before an existing node in the main tree, then we replace the merge node span with this node's
            //   span. If we append after an existing node in the main tree,m then we replace the merge node span with the span of
            //   the node next (as in siblings)
            Span nextSiblingSpan = parent.children().get(indexOfRefNode + 1).span();
            mergeNode.setOffset(nextSiblingSpan);
        }
        Objects.requireNonNull(parent);
        nodeToInsert.setParent(parent);
        int insertPos = shouldInsertBefore ? indexOfRefNode : indexOfRefNode + 1;
        parent.children().add(insertPos, nodeToInsert);
    }
}
