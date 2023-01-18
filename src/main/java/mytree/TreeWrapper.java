package mytree;

import ai.serenade.treesitter.Node;
import ai.serenade.treesitter.Tree;
import srcgen.SourceGenerator;

import java.io.IOException;

public class TreeWrapper {
    private final MyNode root;

    public TreeWrapper(Tree sitterTree, String commonSource) {
        var rootNode = sitterTree.getRootNode();
        root = new MyNode(rootNode, commonSource);
        buildTree(root, rootNode, commonSource);
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

    private void buildTree(MyNode wrapperParent, Node nodeParent, String refString) {
        if (nodeParent.isNull())
            return;
        var childCount = nodeParent.getChildCount();
        for (var i = 0; i < childCount; i++) {
            var currentChild = nodeParent.getChild(i);
            MyNode childNode = new MyNode(currentChild, wrapperParent, refString);
            wrapperParent.appendChild(childNode);
            buildTree(childNode, currentChild, refString);
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

    public String generateSource() {
        try {
            var sourceGenerator = new SourceGenerator(this.root);
            return sourceGenerator.generate();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }
}
