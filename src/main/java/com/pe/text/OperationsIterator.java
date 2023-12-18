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
        this.edits = result.streamEditTypes().mapToInt(OperationType::ordinal).toArray();
        this.includeMatchingOperation = includeMatchingOperation;
    }

    int size() {
        if (includeMatchingOperation) {
            return result.pattern().text().length()
                    + result.streamEditTypes().mapToInt(type -> type.ordinal() - 2).sum();
        }
        return edits.length;
    }

    private boolean hasNextPatternChar() {
        return patternIndex < result.pattern().text().length();
    }

    @Override
    public boolean hasNext() {
        return editIndex < edits.length || (includeMatchingOperation && hasNextPatternChar());
    }

    @Override
    public Operation next() {
        while (hasNext()) {
            final boolean hasPatternChar = hasNextPatternChar();
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
                        return new Operation(OperationType.DELETION,
                                null,
                                new CharWithIndex(t, textIndex++)
                        );
                    case REPLACEMENT:
                        return new Operation(OperationType.REPLACEMENT,
                                new CharWithIndex(p, patternIndex++),
                                new CharWithIndex(t, textIndex++)
                        );
                    case INSERTION:
                        return new Operation(OperationType.INSERTION,
                                new CharWithIndex(p, patternIndex++),
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
                return new Operation(
                        OperationType.MATCHING,
                        new CharWithIndex(p, patternIndex++),
                        new CharWithIndex(t, textIndex++)
                );
            }

            patternIndex++;
            textIndex++;
        }
        throw new NoSuchElementException();
    }


}
