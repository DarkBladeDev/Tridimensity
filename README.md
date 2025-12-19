# Tridimensity

**Tridimensity** is a robust Java library designed to parse, validate, and transform **Blockbench** models for use with **Minecraft Display Entities**.

It is platform-agnostic (works with Spigot, Paper, Fabric, or standalone Java apps) and focuses on mathematical correctness and strict validation of the model hierarchy.

## 🚀 Features

*   **Format Support**: Load `Generic Models` JSON files exported from Blockbench.
*   **Scene Graph**: Full support for hierarchical parenting (Groups/Bones).
*   **Strict Validation**: Fail-fast parser that rejects invalid models (missing pivots, duplicate UUIDs, inverted geometry).
*   **Math Engine**: Built-in transformation engine using [JOML](https://github.com/JOML-CI/JOML) to compute World Matrices for every node.
*   **Coordinate System**: Automatically handles conversion from Blockbench units (pixels) to Minecraft units (blocks, 1/16 scale).
*   **Zero Logic Dependencies**: Does not spawn entities directly; it provides the *data* so you can use your own spawning logic.

## 📦 Installation

### JitPack
First, add the jitpack repo in your project:

[![](https://jitpack.io/v/DarkBladeDev/Tridimensity.svg)](https://jitpack.io/#DarkBladeDev/Tridimensity)

### Gradle (Kotlin DSL)
```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.DarkBladeDev:Tridimensity:VERSION")
}
```

### Gradle (Groovy DSL)
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
		}
dependencies {
    implementation 'com.github.DarkBladeDev:Tridimensity:VERSION'
	}
```

### Maven (`pom.xml`)
```xml
<dependency>
    <groupId>com.github.DarkBladeDev</groupId>
    <artifactId>Tridimensity</artifactId>
    <version>VERSION</version>
</dependency>
```

## 📚 Usage Guide

### 1. Loading a Model
The `BlockbenchLoader` is the entry point. It requires an `InputStream` of the JSON file.

```java
import com.tridimensity.io.BlockbenchLoader;
import com.tridimensity.model.Model;
import java.io.InputStream;

try (InputStream stream = getClass().getResourceAsStream("/assets/my_model.json")) {
    Model model = BlockbenchLoader.load(stream);
    System.out.println("Model loaded successfully!");
} catch (ModelParseException e) {
    System.err.println("Invalid model: " + e.getMessage());
}
```

### 2. Computing Transformations
Tridimensity separates the static `Model` data from the runtime `ModelInstance` calculations.

```java
import com.tridimensity.model.ModelInstance;
import com.tridimensity.model.ModelNode;
import org.joml.Matrix4f;
import java.util.Map;

// Create an instance (lightweight wrapper for calculation)
ModelInstance instance = model.instantiate();

// Compute World Matrices for all nodes
// This walks the hierarchy: Parent * Local = World
Map<ModelNode, Matrix4f> transforms = instance.computeWorldTransforms();
```

### 3. Rendering (Example with Minecraft API)
Tridimensity gives you the matrices. You decide how to use them (e.g., spawning `ItemDisplay` or `BlockDisplay` entities).

**Note:** Blockbench units are **pixels**. Tridimensity automatically scales translation vectors by `1/16` to match Minecraft blocks.

```java
transforms.forEach((node, matrix) -> {
    // 'matrix' represents the transformation of the GROUP (Bone).
    // The cubes inside this group move with it.
    
    // Example: Convert JOML Matrix4f to a Minecraft friendly format (Transformation)
    // Or extract position/rotation directly:
    
    Vector3f translation = new Vector3f();
    Quaternionf rotation = new Quaternionf();
    Vector3f scale = new Vector3f();
    
    matrix.getTranslation(translation);
    matrix.getUnnormalizedRotation(rotation);
    matrix.getScale(scale);
    
    // Spawn your entity at 'translation' with 'rotation'
    // spawnDisplayEntity(node.getName(), translation, rotation, scale);
});
```

### 4. Accessing Cube Geometry
If you need to construct the mesh or display specific cubes:

```java
for (ModelNode node : model.getRoots()) {
    processNode(node);
}

void processNode(ModelNode node) {
    for (ModelCube cube : node.getCubes()) {
        Vector3f size = cube.getSize();   // e.g. (16, 16, 16)
        Vector3f from = cube.getFrom();   // Local coords
        
        // Access UVs
        cube.getFaces().forEach((dir, face) -> {
             System.out.println("Face: " + dir + " Texture: " + face.getTexture());
        });
    }
    
    // Recurse
    for (ModelNode child : node.getChildren()) {
        processNode(child);
    }
}
```

## 📐 Coordinate Systems & Math

*   **Units**: 
    *   Input (JSON): Blockbench Pixels (1 block = 16 pixels).
    *   Output (Matrices): Minecraft Blocks (1 unit = 1 block).
*   **Axes**:
    *   X: East (+) / West (-)
    *   Y: Up (+) / Down (-)
    *   Z: South (+) / North (-)
*   **Rotation Order**: Euler XYZ (Standard Blockbench behavior).

## 🛠️ Building form Source

Requirements: Java 21 JDK.

```bash
# Run tests
./gradlew test

# Build JAR
./gradlew build
```

## License
MIT License.
