package com.tridimensity.io.fix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FixReport {
    private final List<String> warnings = new ArrayList<>();

    public void warn(String message) {
        if (message != null && !message.isEmpty()) {
            warnings.add(message);
        }
    }

    public List<String> warnings() {
        return Collections.unmodifiableList(warnings);
    }
}

