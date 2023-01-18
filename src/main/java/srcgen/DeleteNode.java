package srcgen;

import mytree.Span;
import mytree.TreeWrapper;

import java.util.Objects;

public class DeleteNode extends Action {
    private final Span span;
    public DeleteNode(TreeWrapper tree, Span span) {
        super(tree);
        Objects.requireNonNull(span);
        this.span = span;
    }

    @Override
    public void apply() {
        var node = tree.nodeAtSpan(span);
        node.setDeleted(true);
    }
}
