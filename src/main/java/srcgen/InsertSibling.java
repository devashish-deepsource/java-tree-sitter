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
        Objects.requireNonNull(parent);
        nodeToInsert.setParent(parent);
        int insertPos = shouldInsertBefore ? indexOfRefNode : indexOfRefNode + 1;
        parent.children().add(insertPos, nodeToInsert);
    }
}
