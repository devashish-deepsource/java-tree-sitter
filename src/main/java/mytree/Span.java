package mytree;

import ai.serenade.treesitter.Range;

import java.util.ArrayList;
import java.util.List;

public class Span {
    private Range range;
    private List<Span> consumedSpans = new ArrayList<>();

    public Span(Range range) {
        this.range = range;
    }

    public void reset() {
        consumedSpans.clear();
    }

    // For a span to contain another span, it has to appear first (or equal) in source and end last (or equal).
    // TODO: Is it better to compare start and end bytes here? Explore that.
    boolean contains(Span span) {
        boolean firstInRowStart = range.startRow < span.range.startRow;
        boolean equalInRowStart = range.startRow == span.range.startRow;
        boolean firstInColStart = range.startCol < span.range.startCol;
        boolean equalInColStart = range.startCol == span.range.startCol;

        boolean lastInRowEnd = range.endRow > span.range.endRow;
        boolean equalInRowEnd = range.endRow == span.range.endRow;
        boolean lastInColEnd = range.endCol > span.range.endCol;
        boolean equalInColEnd = range.endCol == span.range.endCol;

        boolean startOkay = firstInRowStart || (equalInRowStart && (firstInColStart || equalInColStart));
        boolean endOkay = lastInRowEnd || (equalInRowEnd && (lastInColEnd || equalInColEnd));
        return startOkay && endOkay;
    }

    private boolean slotIsConsumed(Span span) {
        // If a consumed span contains this span, then this span is consumed as well.
        for (var consumed : consumedSpans) {
            if (consumed.contains(span))
                return true;
        }
        return false;
    }

    // TODO: Do we have to keep track of consumed spans at all?
    public void consume(Span span) {
        if (!this.contains(span))
            throw new RuntimeException("This span doesn't contain the other span!");
        if (slotIsConsumed(span))
            throw new RuntimeException("Span slot is consumed!");
        consumedSpans.add(span);
    }

    public int startRow() {
        return range.startRow;
    }

    public int startCol() {
        return range.startCol;
    }

    public int endRow() {
        return range.endRow;
    }

    public int endCol() {
        return range.endCol;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Span other))
            return false;
        return startRow() == other.startRow() && startCol() == other.startCol() && endRow() == other.endRow() && endCol() == other.endCol();
    }
}
