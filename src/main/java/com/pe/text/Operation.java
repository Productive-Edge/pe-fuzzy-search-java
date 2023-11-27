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
     * Returns pattern character details used in the operation
     * <ul>
     *     <li>{@link OperationType#MATCHING} - matched pattern character value</li>
     *     <li>{@link OperationType#DELETION} - must be {@code null} for deletion</li>
     *     <li>{@link OperationType#REPLACEMENT} - pattern character used for replacement in the text</li>
     *     <li>{@link OperationType#INSERTION} - pattern character inserted into the text</li>
     * </ul>
     *
     * @return pattern character and its position index used in the operation
     */
    public CharWithIndex patternChar() {
        return patternChar;
    }

    /**
     * Returns text character and its index before the edit operation was applied
     * <ul>
     *     <li>{@link OperationType#MATCHING} - matched text character</li>
     *     <li>{@link OperationType#DELETION} - deleted text character</li>
     *     <li>{@link OperationType#REPLACEMENT} - replaced text character</li>
     *     <li>{@link OperationType#INSERTION} - must be {@code null} for insertion</li>
     * </ul>
     *
     * @return text character and its index before the edit operation was applied
     */
    public CharWithIndex textChar() {
        return textChar;
    }
}
