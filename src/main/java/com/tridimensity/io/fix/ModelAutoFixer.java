package com.tridimensity.io.fix;

import com.tridimensity.io.ast.ModelAst;

public interface ModelAutoFixer {
    void apply(ModelAst model, FixReport report);
}

