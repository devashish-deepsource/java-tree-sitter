package mytree;

import java.util.List;

public class MergeNode extends MyNode {
    private final boolean affectsRow;
    private final MyNode myNode;
    private final Span offset;

    public MergeNode(MyNode node, MyNode parent, String refString, boolean affectsRow, Span offset) {
        super(node.getInternalNode(), parent, refString);
        this.myNode = node;
        this.children = node.children;
        this.affectsRow = affectsRow;
        this.offset = offset;
    }

    @Override
    public List<MyNode> children() {
        return myNode.children;
    }

    public Span offset() {
        return offset;
    }

    // If merging this node in the main tree changes the number of lines that we are to see in regenerated source,
    // then we classify this node as one that affect rows. If we observe change in only the line length, then we
    // classify this node that only affects columns. Inserting a node in the tree can only produce one of these two
    // kinds of merge nodes.
    public boolean affectsRow() {
        return affectsRow;
    }

    public MyNode getNode() {
        return myNode;
    }
}
