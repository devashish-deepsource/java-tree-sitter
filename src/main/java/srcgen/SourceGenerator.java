package srcgen;

import mytree.MergeNode;
import mytree.MyNode;
import mytree.Span;
import mytree.TreeWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class SourceGenerator {
    private final List<String> commonSourceLines;
    private final MyNode node;
    record SourceSegment(MyNode node, String string, Span span) {
    }

    private final Comparator<SourceSegment> sourceSegmentSorter = (seg1, seg2) -> {
        var rowDiff = seg1.span.startRow() - seg2.span.startRow();
        var colDiff = seg1.span.startCol() - seg2.span.startCol();
        return rowDiff == 0 ? colDiff : rowDiff;
    };

    public SourceGenerator(MyNode root) throws IOException {
        this.node = root;
        commonSourceLines = Arrays.stream(root.refString().split("\n")).toList();
    }

    private String getLeafSource(MyNode node) {
        if (!node.isLeaf())
            throw new RuntimeException("Can't get source for non-leaf nodes!");

        var span = node.span();
        // TODO: Document consume and also clarify if it is required at all.
        // Consume this span in the parent.
        node.parent().span().consume(span);

        boolean endsOnTheSameLine = span.startRow() == span.endRow();
        var startLine = commonSourceLines.get(span.startRow());
        if (endsOnTheSameLine)
            return startLine.substring(span.startCol(), span.endCol());

        var builder = new StringBuilder("\n");
        // If the segment does not end on the same line, then it probably consumes this line completely.
        String startSegment = startLine.substring(span.startCol());
        builder.append(startSegment);
        for (int i = span.startRow() + 1; i < span.endRow(); i++) {
            var line = commonSourceLines.get(i);
            builder.append(line).append("\n");
        }

        try {
            // It's unlikely that a leaf node expands more than one line, but if it does, we're handling it here.
            var endLine = commonSourceLines.get(span.endRow());
            String endSegment = endLine.substring(0, span.endCol());
            builder.append(endSegment);
        } catch (ArrayIndexOutOfBoundsException e) {
            // Fixme: if the file has a new line at the end and we're splitting by "\n", we will only have 20 elements
            //  in the array of lines. The OOB happens cause we're trying to query the 21st line.
            System.out.println(e);
        }

        return builder.toString();
    }

    private void fixMergeNodeSpans(List<SourceSegment> sourceSegments) {
        for (var seg : sourceSegments) {
            if (seg.node instanceof MergeNode mergeNode) {
                mergeNode.setSpanRecursive(mergeNode.offset());
                mergeNode.getNode().setSpanRecursive(mergeNode.offset());
            }
        }
    }

    private String sourceSegmentsToSource(List<SourceSegment> sourceSegments) {
        fixMergeNodeSpans(sourceSegments);
        sourceSegments.sort(sourceSegmentSorter);
        var builder = new StringBuilder();
        for (var srcSegment : sourceSegments) {
            if (!srcSegment.node.isDeleted()) {
                builder.append(srcSegment.string);
            }
        }
        return builder.toString();
    }

    private String getMergeString(MergeNode mergeNode) {
        try {
            return new SourceGenerator(mergeNode.getNode()).generate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // We only generate source for leaf nodes. So the strategy is to go depth-first looking for leaves
    // and once we find one, "generate" the source for it and wrap it in a `SourceSegment` instance. We collect
    // all such leaf source segments in a list, and sort it later for codegen.
    private void populateSourceSegments(MyNode currentNode, List<SourceSegment> leafSourceSegments) {
        if (currentNode instanceof MergeNode mergeNode) {
            // This is a `MergeNode`. Source
            String mergeNodeSource = getMergeString(mergeNode);
            var segment = new SourceSegment(mergeNode, mergeNodeSource, mergeNode.offset());
            leafSourceSegments.add(segment);
            return;
        }

        if (currentNode.isLeaf()) {
            // This is a leaf node. Generate a source segment.
            var leafSource = getLeafSource(currentNode);
            var srcSegment = new SourceSegment(currentNode, leafSource, currentNode.span());
            leafSourceSegments.add(srcSegment);
            return;
        }

        for (var child : currentNode.children()) {
            populateSourceSegments(child, leafSourceSegments);
        }
    }

    // TODO: how about we only keep the spans for each source segment and the token/segment string itself.
    private List<SourceSegment> getSourceSegments() {
        var list = new ArrayList<SourceSegment>();
        populateSourceSegments(node, list);
        return list;
    }

    public String generate() {
        var srcSegments = getSourceSegments();
        return sourceSegmentsToSource(srcSegments);
    }
}
