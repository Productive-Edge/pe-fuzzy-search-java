package com.pe.text;

class OperationRecord implements Operation {

    private final OperationType type;
    private final CharDetails pattern;
    private final CharDetails text;

    OperationRecord(OperationType type, CharDetails pattern, CharDetails text) {
        this.type = type;
        this.pattern = pattern;
        this.text = text;
    }

    @Override
    public OperationType type() {
        return type;
    }

    @Override
    public CharDetails patternChar() {
        return pattern;
    }

    @Override
    public CharDetails textChar() {
        return text;
    }
}
