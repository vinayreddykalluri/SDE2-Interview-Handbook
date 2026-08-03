# Packaging, Layers, Containers, and Runtime Resources

The build is not complete when tests pass. The deployed artifact must contain the intended classes and dependencies, start with the supported command, expose the right ports, run as a restricted user, respect resource limits, and stop within the platform budget.

## Executable jar anatomy

The Boot plugin repackages application classes and dependencies with launcher metadata. Verify:

```bash
./mvnw clean verify
jar tf target/order-service.jar | head
java -jar target/order-service.jar --spring.profiles.active=smoke
```

Do not deploy an IDE classpath. Test the exact artifact produced by CI.

## Layered jars

Application code changes more often than dependencies. Layer metadata allows container builds to cache stable dependency layers separately from snapshot dependencies and application classes.

```text
dependencies
spring-boot-loader
snapshot-dependencies
application
```

Layering improves rebuild/pull efficiency. It does not reduce runtime memory by itself.

## Buildpacks

```bash
./mvnw spring-boot:build-image \
  -Dspring-boot.build-image.imageName=registry.example/orders:1.4.2
```

Cloud Native Buildpacks create an OCI image without a handwritten Dockerfile. They choose build/run images, JVM settings, layers, and metadata. Pin trusted builders/run images according to organizational policy and scan the final image.

## A deliberate Dockerfile path

When requirements need a custom image, use a multi-stage build and a minimal runtime image. Copy the verified jar, run as non-root, set an explicit entry point, and avoid shell surprises.

```dockerfile
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 appuser
WORKDIR /app
COPY order-service.jar app.jar
USER 10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

The image tag should be immutable or paired with a digest. Never bake environment secrets into image layers.

## Resource awareness

Container memory includes heap, metaspace, code cache, thread stacks, direct buffers, native libraries, and side effects such as memory-mapped files. Setting heap equal to the container limit invites termination.

Measure:

- live heap after warmup;
- allocation and GC pause behavior;
- thread count and stack size;
- direct/network buffer usage;
- class/metaspace growth;
- headroom during traffic spikes and diagnostics.

CPU limits can change available processors and default pool sizing. Do not let every framework/library derive a large pool independently.

## Supply-chain evidence

Produce and retain:

- dependency graph and lock/verification metadata;
- software bill of materials;
- vulnerability results with reachability/context;
- artifact checksum and signature where used;
- base-image provenance;
- build and deployment version information.

## Common mistakes

- Running as root.
- Using `latest` as the only deployable identity.
- Copying the Maven cache and source tree into the runtime image.
- Setting heap to 100 percent of container memory.
- Expecting a Docker `HEALTHCHECK` to replace orchestrator probes.
- Installing troubleshooting tools in every production image without risk review.
- Building one artifact locally and deploying another from CI.

## Interview angle

**Interviewer:** A pod with a 1 GiB limit is OOM-killed although `-Xmx768m`. Explain.

**Strong answer:** The limit covers more than heap: metaspace, code cache, thread stacks, direct buffers, native libraries, and process overhead. I inspect container termination reason and native memory evidence, count threads/direct buffers, leave measured headroom, and tune workload/pools before simply lowering or raising heap.

## Quick check

1. What does executable repackaging add?
2. Why use layers?
3. What does buildpack generation still require from the team?
4. Which memory areas exist outside heap?
5. Why use immutable image identity?

## Practice

- **Foundation:** Run the exact packaged jar in CI.
- **Interview Core:** Compare a buildpack image and custom Dockerfile.
- **Interview Core:** Create a memory budget for a 1 GiB container.
- **SDE-2 Follow-up:** Design artifact provenance from source commit to deployed digest.
