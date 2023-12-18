package com.pe.text;

/**
 * Enumeration of text operations, including the matching which has 0 in the Levenshtein metric
 */
public enum OperationType {
    /**
     * No edit operation is applied - the pattern character and the text one do match
     */
    MATCHING,
    /**
     * Edit operation - deletion of the character in the text
     */
    DELETION,

    /**
     * Edit operation - replacement of the character in the text onto character from the pattern
     */
    REPLACEMENT,

    /**
     * Edit operation - insertion of the pattern character into the text
     */
    INSERTION;

    /**
     * Cached instances of all operations as array for fast conversion from ordinal value
     */
    static final OperationType[] values = values();
}
