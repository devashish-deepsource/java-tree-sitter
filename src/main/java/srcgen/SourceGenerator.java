package srcgen;

import mytree.MyNode;
import mytree.Span;
import mytree.TreeWrapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class SourceGenerator {
    record SourceSegment(String string, Span span) {
    }

    private final List<String> sourceLines;

    private final Comparator<SourceSegment> sourceSegmentSorter = (seg1, seg2) -> {
        var rowDiff = seg1.span.startRow() - seg2.span.startRow();
        var colDiff = seg1.span.startCol() - seg2.span.startCol();
        return rowDiff == 0 ? colDiff : rowDiff;
    };

    public SourceGenerator(String inputFilePath) throws IOException {
        var sourceString = Files.readString(Paths.get(inputFilePath));
        sourceLines = Arrays.stream(sourceString.split("\n")).toList();
    }

    private String getLeafSource(MyNode node) {
        if (!node.isLeaf())
            throw new RuntimeException("Can't get source for non-leaf nodes!");

        var span = node.span();
        // TODO: Document consume and also clarify if it is required at all.
        // Consume this span in the parent.
        node.parent().span().consume(span);

        boolean endsOnTheSameLine = span.startRow() == span.endRow();
        var startLine = sourceLines.get(span.startRow());
        if (endsOnTheSameLine)
            return startLine.substring(span.startCol(), span.endCol());

        var builder = new StringBuilder("\n");
        // If the segment does not end on the same line, then it probably consumes this line completely.
        String startSegment = startLine.substring(span.startCol());
        builder.append(startSegment);
        for (int i = span.startRow() + 1; i < span.endRow(); i++) {
            var line = sourceLines.get(i);
            builder.append(line).append("\n");
        }

        // It's unlikely that a leaf node expands more than one line, but if it does, we're handling it here.
        var endLine = sourceLines.get(span.endRow());
        String endSegment = endLine.substring(0, span.endCol());
        builder.append(endSegment);
        return builder.toString();
    }

    // FIXME: Trailing comments at the end of the line are never considered.
    private String getWhitespacesBetweenSegments(SourceSegment seg1, SourceSegment seg2) {
        var sp1 = seg1.span;
        var sp2 = seg2.span;

        boolean lineOverlaps = sp1.endRow() == sp2.startRow();
        if (lineOverlaps) {
            int spaceDiff = Math.abs(sp2.startCol() - sp1.endCol());
            // There will only be spaces in the middle.
            return " ".repeat(spaceDiff);
        }

        // Line changes. Figure out how many newline characters are there.
        int lineDiff = Math.abs(sp2.startRow() - sp1.endRow());
        var newLines = "\n".repeat(lineDiff);
        var spaces = " ".repeat(sp2.startCol());
        return newLines + spaces;
    }

    private String sourceSegmentsToSource(List<SourceSegment> sourceSegments) {
        sourceSegments.sort(sourceSegmentSorter);
        var builder = new StringBuilder();
        SourceSegment lastSegment = null;
        for (var srcSegment : sourceSegments) {
            if (lastSegment != null) {
                var whitespaces = getWhitespacesBetweenSegments(lastSegment, srcSegment);
                builder.append(whitespaces);
            }
            lastSegment = srcSegment;
            builder.append(srcSegment.string);
        }
        return builder.toString();
    }

    // We only generate source for leaf nodes. So the strategy is to go depth-first looking for leaves
    // and once we find one, "generate" the source for it and wrap it in a `SourceSegment` instance. We collect
    // all such leaf source segments in a list, and sort it later for codegen.
    private void populateSourceSegments(MyNode node, List<SourceSegment> leafSourceSegments) {
        if (node.isDeleted())
            return;
        if (node.isLeaf()) {
            // This is a leaf node. Generate a source segment.
            var leafSource = getLeafSource(node);
            var srcSegment = new SourceSegment(leafSource, node.span());
            leafSourceSegments.add(srcSegment);
            return;
        }

        for (var child : node.children()) {
            populateSourceSegments(child, leafSourceSegments);
        }
    }

    // TODO: how about we only keep the spans for each source segment and the token/segment string itself.
    private List<SourceSegment> getSourceSegments(MyNode node) {
        var list = new ArrayList<SourceSegment>();
        populateSourceSegments(node, list);
        return list;
    }

    public String generate(TreeWrapper tree) {
        var srcSegments = getSourceSegments(tree.root());
        return sourceSegmentsToSource(srcSegments);
    }
}
