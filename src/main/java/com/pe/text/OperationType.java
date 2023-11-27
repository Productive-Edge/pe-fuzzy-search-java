package com.pe.text;

public enum OperationType {
    MATCHING,
    DELETION,
    REPLACEMENT,
    INSERTION;

    static final OperationType[] values = values();
}
