# Compilable Java 21 examples

These examples are small, dependency-free companions to the book. They favor an explicit invariant or platform mechanism over framework code.

Compile everything without Maven:

```bash
mkdir -p build/classes
find src/main/java -name '*.java' -print0 \
  | xargs -0 javac --release 21 -d build/classes
java -cp build/classes com.interviewbook.examples.AllExamplesSmokeTest
```

Or run `mvn -q package` with Maven 3.9+ and JDK 21.

Some examples intentionally demonstrate behavior that depends on scheduling. Assertions check contracts, not a particular interleaving.
