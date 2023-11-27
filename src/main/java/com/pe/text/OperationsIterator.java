package com.pe.text;

import java.util.Iterator;
import java.util.NoSuchElementException;

class OperationsIterator implements Iterator<Operation> {
    final int[] edits;
    final boolean includeMatchingOperation;
    final FuzzyResult result;
    final CharSequence text;
    private int editIndex;
    private int patternIndex;
    private int textIndex;

    OperationsIterator(FuzzyResult result, boolean includeMatchingOperation) {
        this.result = result;
        this.text = result.foundText();
        this.edits = result.streamEdits().mapToInt(OperationType::ordinal).toArray();
        this.includeMatchingOperation = includeMatchingOperation;
    }

    int size() {
        if (includeMatchingOperation) {
            return result.pattern().text().length()
                    + result.streamEdits().mapToInt(type -> type.ordinal() - 2).sum();
        }
        return edits.length;
    }

    @Override
    public boolean hasNext() {
        return editIndex < edits.length;
    }

    @Override
    public Operation next() {
        while (hasNext()) {
            final boolean hasPatternChar = patternIndex < result.pattern().text().length();
            final char p = hasPatternChar
                    ? result.pattern().text().charAt(patternIndex)
                    : (char) 0;

            final boolean hasTextChar = textIndex < text.length();
            final char t = hasTextChar
                    ? text.charAt(textIndex)
                    : (char) 0;

            boolean isSame = hasPatternChar && hasTextChar && (
                    result.pattern().caseInsensitive()
                            ? Character.toLowerCase(p) == Character.toLowerCase(t)
                            : p == t
            );

            if (!isSame) {
                switch (OperationType.values[edits[editIndex++]]) {
                    case DELETION:
                        return new OperationRecord(OperationType.DELETION,
                                null,
                                new CharDetailsRecord(t, textIndex++)
                        );
                    case REPLACEMENT:
                        return new OperationRecord(OperationType.REPLACEMENT,
                                new CharDetailsRecord(p, patternIndex++),
                                new CharDetailsRecord(t, textIndex++)
                        );
                    case INSERTION:
                        return new OperationRecord(OperationType.INSERTION,
                                new CharDetailsRecord(p, patternIndex++),
                                null
                        );
                    default:
                        throw new IllegalStateException(
                                "Unexpected edit[" + (editIndex - 1) + "]=" + OperationType.values[edits[editIndex - 1]]
                                        + ' ' + result
                        );
                }
            }

            if (includeMatchingOperation) {
                return new OperationRecord(
                        OperationType.MATCHING,
                        new CharDetailsRecord(p, patternIndex++),
                        new CharDetailsRecord(t, textIndex++)
                );
            }
            
            patternIndex++;
            textIndex++;
        }
        throw new NoSuchElementException();
    }


}
