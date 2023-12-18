package com.pe.text;

/**
 * Immutable record with information about edit/matching operation in the {@link FuzzyResult}
 */
public final class Operation {

    private final OperationType type;
    private final CharWithIndex patternChar;
    private final CharWithIndex textChar;

    public Operation(OperationType type, CharWithIndex patternChar, CharWithIndex textChar) {
        this.type = type;
        this.patternChar = patternChar;
        this.textChar = textChar;
    }

    /**
     * Returns operation type
     *
     * @return operation type
     * @see OperationType
     */
    public OperationType type() {
        return type;
    }

    /**
     * Returns pattern character with index which was used in the operation
     * <ul>
     *     <li>{@link OperationType#MATCHING} - matched pattern character value</li>
     *     <li>{@link OperationType#DELETION} - must be {@code null} for deletion</li>
     *     <li>{@link OperationType#REPLACEMENT} - pattern character used for replacement in the text</li>
     *     <li>{@link OperationType#INSERTION} - pattern character inserted into the text</li>
     * </ul>
     *
     * @return pattern character with index or {@code null} in case of {@link OperationType#DELETION}
     */
    public CharWithIndex patternChar() {
        return patternChar;
    }

    /**
     * Returns character and its index in the found text where edit operation was applied:
     * <ul>
     *     <li>{@link OperationType#MATCHING} - matched text character</li>
     *     <li>{@link OperationType#DELETION} - deleted text character</li>
     *     <li>{@link OperationType#REPLACEMENT} - replaced text character</li>
     *     <li>{@link OperationType#INSERTION} - must be {@code null} for insertion</li>
     * </ul>
     *
     * @return character and its index in the found text or {@code null} in case of {@link OperationType#INSERTION}
     */
    public CharWithIndex textChar() {
        return textChar;
    }

    @Override
    public String toString() {
        return "Operation{" +
                "type=" + type +
                ", patternChar=" + patternChar +
                ", textChar=" + textChar +
                '}';
    }
}
