package com.tridimensity.io.options;

public class ParserOptions {
    private final boolean autoFixTransforms;

    public ParserOptions(boolean autoFixTransforms) {
        this.autoFixTransforms = autoFixTransforms;
    }

    public static ParserOptions strict() {
        return new ParserOptions(false);
    }

    public boolean isAutoFixTransforms() {
        return autoFixTransforms;
    }
}

