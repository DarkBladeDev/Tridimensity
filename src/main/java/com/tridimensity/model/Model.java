package com.tridimensity.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The root container for a Blockbench model.
 */
public class Model {
    private final List<ModelNode> roots;

    public Model() {
        this.roots = new ArrayList<>();
    }

    public void addRoot(ModelNode node) {
        this.roots.add(node);
    }

    public List<ModelNode> getRoots() {
        return new ArrayList<>(roots);
    }

    public ModelInstance instantiate() {
        return new ModelInstance(this);
    }
}
