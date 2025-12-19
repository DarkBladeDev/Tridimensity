package com.tridimensity.io.dto;

import java.util.Map;

public class ElementDto {
    public String name;
    public String uuid;
    public float[] from;
    public float[] to;
    public Map<String, FaceDto> faces;
    
    public static class FaceDto {
        public float[] uv;
        public String texture;
        public int rotation;
    }
}
