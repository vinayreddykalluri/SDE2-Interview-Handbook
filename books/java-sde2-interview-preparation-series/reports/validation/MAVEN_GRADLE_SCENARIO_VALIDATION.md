# Maven and Gradle Scenario Validation

## Validated assets

| Asset | Contract |
|---|---|
| `BuildToolModel.java` | dependency-first order, cycle rejection, affected modules and deterministic input key |
| Maven fixture | two-module reactor compile, package and runtime output |
| Gradle fixture | two-project task graph compile, package and runtime output |

## Command

```bash
bash content/volumes/java/JAVA-03-maven-and-gradle/labs/validate_build_labs.sh
```

## Result

- Java 21 companion: compiled with `-Xlint:all -Werror` and passed assertions.
- Maven 3.9.9 fixture: `verify` passed; application printed `total=42`; JAR contained `Main.class`.
- Gradle 8.13 fixture: `build` and `:app:run` passed; application printed `total=42`; JAR contained `Main.class`.
- Canonical fixture directories remained clean because all build work ran in a temporary directory.

## Series integration

`scripts/validate_series.py` runs this validator whenever BUILD is published. Maven or Gradle execution is reported as skipped when that executable is unavailable, while structural and Java companion checks remain mandatory. In the publication environment both tools were available and passed.

## Remaining warnings

- Fixture versions validate the stable concepts taught by the book. Production repositories must use their reviewed wrappers and compatibility policy.
- The fixtures intentionally avoid third-party dependencies; dependency-resolution examples are educational snippets and official-reference-backed diagnostic commands.
