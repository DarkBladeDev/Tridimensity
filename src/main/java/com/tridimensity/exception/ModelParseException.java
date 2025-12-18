package com.tridimensity.exception;

public class ModelParseException extends RuntimeException {
    private final Integer line;
    private final String pointer;

    public ModelParseException(String message) {
        super(message);
        this.line = null;
        this.pointer = null;
    }

    public ModelParseException(String message, Throwable cause) {
        super(message, cause);
        this.line = null;
        this.pointer = null;
    }

    public ModelParseException(String message, int line) {
        super(message + " (line=" + line + ")");
        this.line = line;
        this.pointer = null;
    }

    public ModelParseException(String message, int line, String pointer) {
        super(message + " (line=" + line + (pointer != null ? ", pointer=" + pointer : "") + ")");
        this.line = line;
        this.pointer = pointer;
    }

    public Integer getLine() {
        return line;
    }

    public String getPointer() {
        return pointer;
    }
}
