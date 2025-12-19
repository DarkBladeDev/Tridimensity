package com.tridimensity.util;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class TransformUtils {
    private TransformUtils() {}

    public static Quaternionf extractRotation(Matrix4f m) {
        Quaternionf q = new Quaternionf();
        m.getUnnormalizedRotation(q);
        return q;
    }

    public static Vector3f extractScale(Matrix4f m) {
        Vector3f s = new Vector3f();
        m.getScale(s);
        return s;
    }

    public static Vector3f extractTranslation(Matrix4f m) {
        Vector3f t = new Vector3f();
        m.getTranslation(t);
        return t;
    }

    public static Parts decompose(Matrix4f m) {
        return new Parts(extractTranslation(m), extractRotation(m), extractScale(m));
    }

    public static final class Parts {
        public final Vector3f translation;
        public final Quaternionf rotation;
        public final Vector3f scale;

        public Parts(Vector3f translation, Quaternionf rotation, Vector3f scale) {
            this.translation = translation;
            this.rotation = rotation;
            this.scale = scale;
        }
    }
}
