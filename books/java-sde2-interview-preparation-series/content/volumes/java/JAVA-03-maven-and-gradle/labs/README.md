# Maven and Gradle executable labs

These fixtures build the same two-module Java 21 application with Maven and Gradle. They intentionally contain no third-party Java dependencies so validation focuses on lifecycle, project graph, compilation, packaging, and runtime behavior.

Run all checks from the series root:

```bash
bash content/volumes/java/JAVA-03-maven-and-gradle/labs/validate_build_labs.sh
```

The validator copies fixtures to a temporary directory before building, so `target/` and `build/` outputs never modify the canonical source tree. It uses installed Maven and Gradle when available and always validates the dependency-free Java companion.

Real projects should check in Maven or Gradle wrapper files after reviewing their distribution version, URL, and integrity policy. Binary wrapper JARs are intentionally not duplicated inside this educational fixture.
