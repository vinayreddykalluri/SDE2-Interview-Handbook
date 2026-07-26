# 32. Java I/O, NIO.2, Files, Buffers, and Serialization Boundaries

## Learning objectives

By the end of this chapter, you should be able to:

- choose byte, character, buffered, channel, and file APIs by data semantics;
- manage resources, partial operations, exceptions, charsets, and durability explicitly;
- use `Path` and `Files` without confusing lexical paths with filesystem identity;
- derive the `Buffer` position-limit-capacity state machine;
- explain blocking channels, selectors, direct buffers, and memory mapping at a contract boundary;
- perform safer temporary-file publication and directory traversal; and
- treat Java native serialization and all external formats as security boundaries.

## Why this matters at SDE-2

I/O connects Java's managed object world to files, sockets, pipes, and external data. Many production incidents originate at that boundary: leaked descriptors, default-charset corruption, partial writes, non-atomic publication, unbounded reads, path traversal, symlink races, or unsafe deserialization.

At SDE-2, you should reason beyond method names. What owns the resource? Can a read return fewer bytes? Is the output durable after `close`? Does moving a file replace atomically? Can input be trusted? Is text decoded incrementally? A robust design answers these questions and preserves them in tests and operational limits.

## First-principles model

I/O transfers finite sequences of bytes between a program and an external endpoint. Text is not bytes by itself; a charset maps characters to encoded bytes and back.

```text
Java characters --CharsetEncoder--> bytes --external storage/network
Java characters <--CharsetDecoder-- bytes <--external storage/network
```

Classic I/O exposes sequential streams:

- `InputStream` and `OutputStream` transfer bytes.
- `Reader` and `Writer` transfer characters.
- decorators add buffering, data conversion, compression, or other behavior.

NIO exposes buffers and channels:

```text
channel <-> ByteBuffer <-> application
```

A buffer has three core indexes satisfying:

```text
0 <= position <= limit <= capacity
```

Write into a fresh buffer from `position` toward `limit`. After filling, `flip()` sets `limit` to the amount written and `position` to zero, preparing for reads. After consuming, `clear()` prepares the entire storage for overwrite; `compact()` preserves unread bytes and prepares the remaining suffix for more input.

> **Specification boundary:** Java APIs define resource, buffer, path, channel, and serialization behavior. Operating-system caches, filesystem atomicity support, descriptor limits, direct-buffer allocation, selector implementation, and durability details vary by platform and JDK.

## Core terminology

- **Byte stream:** Sequence of raw 8-bit units.
- **Character stream:** Sequence decoded or encoded using a charset.
- **Decorator:** Wrapper that adds behavior while delegating to another stream.
- **Buffering:** Accumulating data to reduce calls and improve transfer efficiency.
- **Channel:** I/O connection supporting buffer-based operations.
- **Path:** Filesystem path representation; it may identify a nonexistent entry.
- **Partial read/write:** Successful operation transferring fewer units than requested.
- **Atomic move:** Move observed as one indivisible namespace update when supported.
- **Durability:** Extent to which data survives process or machine failure.
- **Direct buffer:** Buffer whose storage is intended to support efficient native I/O outside the ordinary Java heap array model.
- **Memory mapping:** Mapping a file region into virtual memory through a buffer-like view.
- **Serialization:** Encoding an object or data model into a transportable representation.

## Detailed mechanics

### Bytes, characters, and charsets

Use byte APIs for images, compressed data, protocols, hashes, and already encoded content. Use readers and writers for text. Always specify the charset at persistent or network boundaries, commonly `StandardCharsets.UTF_8`. The platform default can differ across environments or be configured, and relying on it makes formats ambiguous.

Unicode characters may require multiple bytes, and a buffer boundary can split one encoded character. `InputStreamReader` and `OutputStreamWriter` maintain encoding state correctly. Converting arbitrary chunks independently with `new String(chunk, UTF_8)` can corrupt a multibyte character split across chunks.

Malformed or unmappable input has a policy. Convenience decoding may replace invalid sequences. When strict validation matters, configure a `CharsetDecoder` with `CodingErrorAction.REPORT`.

### Stream layering and buffering

Streams compose:

```java
try (var in = new java.io.BufferedInputStream(
        new java.util.zip.GZIPInputStream(
                java.nio.file.Files.newInputStream(path)))) {
    // consume decompressed bytes
}
```

The outermost close normally closes delegated resources, but check third-party wrappers. Buffering reduces calls across Java/native or network boundaries. It does not change asymptotic byte count, and wrapping an already buffered API repeatedly can waste memory.

`read()` returns an unsigned byte value `0..255` or `-1` for end-of-stream. Bulk `read(byte[])` can return any positive count up to array length, `-1` at end, and in some APIs zero under specified circumstances. Never assume one read fills a requested buffer. Similarly, a channel write can consume fewer remaining bytes, so loop until completion or until a nonblocking policy says to wait.

### Resource ownership and try-with-resources

Open streams, channels, directory streams, and file walks must be closed. Try-with-resources closes in reverse declaration order even when work throws. If both work and close throw, the work exception is primary and close exceptions are suppressed:

```java
catch (IOException ex) {
    for (Throwable suppressed : ex.getSuppressed()) {
        log(suppressed);
    }
}
```

Do not close a resource you merely borrow unless the API transfers ownership. A method accepting an `OutputStream` often should not close it; document whether it flushes. Conversely, a method that opens a path internally should normally own and close the opened resource.

`flush` asks buffered layers to push data downstream. It does not necessarily make bytes durable on physical storage. File-channel `force` requests stronger synchronization, but filesystem, device, and platform semantics still matter. Durable transactional publication may require syncing both file data and directory metadata according to platform-specific design.

### Path and Files

`Path` is a value-like representation of path components. It can be relative or absolute and does not imply existence. Useful distinctions:

- `normalize()` removes redundant lexical `.` and resolvable `..` segments without accessing the filesystem.
- `toAbsolutePath()` anchors a relative path but does not resolve symbolic links.
- `toRealPath()` accesses the filesystem, resolves links by default, and requires existence.
- `resolve` joins paths; if the argument is absolute, it may replace the base.
- `relativize` computes a lexical relative path between compatible paths.

Files methods offer creation, copying, moving, metadata, directory streams, buffered readers, and attributes. Check options precisely: for example, `CREATE_NEW` fails if the target already exists, while `TRUNCATE_EXISTING` destroys previous content once opening succeeds.

`Files.move` with `ATOMIC_MOVE` requests atomicity; providers may throw `AtomicMoveNotSupportedException`. Cross-filesystem moves generally cannot be one atomic rename. `REPLACE_EXISTING` controls collision behavior, but replacement details can vary. Design and test a fallback explicitly rather than silently weakening guarantees.

### Safe path containment and symbolic links

This check is only lexical:

```java
Path candidate = root.resolve(userInput).normalize();
if (!candidate.startsWith(root.normalize())) reject();
```

It blocks simple `..` traversal but does not defeat symlinks or validation-use races. Security-sensitive access needs a trusted root, link-aware operations, and a platform-specific threat model.

Directory traversal methods such as `Files.list`, `Files.walk`, and `Files.find` return streams backed by open resources and must be closed. Avoid following symbolic links unless cycle and trust behavior is deliberate. Never read an untrusted entire file with `readAllBytes` without a size limit; file size can change after checking.

### Buffer state transitions

For `ByteBuffer.allocate(8)`:

```text
initial: position=0, limit=8, capacity=8
put 3 bytes: position=3, limit=8
flip: position=0, limit=3
get 2 bytes: position=2, limit=3, remaining=1
compact: unread byte moves to index 0; position=1, limit=8
```

`clear()` does not erase bytes; it resets indexes so storage can be overwritten. `rewind()` sets position to zero without changing limit, allowing rereading current content. `mark` records a position and `reset` returns to it when the mark remains valid. Relative gets advance position; absolute indexed gets do not.

Buffer overflow and underflow indicate state-machine errors: writing beyond remaining space or reading beyond the limit. `slice` and `duplicate` create buffers with independent position/limit state but shared underlying content. Mutation is visible through aliases. Read-only buffers reject writes but may still reflect shared storage changes.

Byte order matters for multi-byte numeric values. Network protocols often specify big-endian order. Set and document `ByteOrder` rather than relying on defaults or native order.

### Channels, nonblocking I/O, and selectors

File channels support positioned reads/writes, transfers, locking, mapping, and forcing. Socket channels can be blocking or nonblocking. In nonblocking mode, zero-byte progress can be a normal result, so a readiness loop registers channels with a `Selector` and reacts to acceptable, connectable, readable, or writable keys.

Readiness means an operation is likely to make progress, not that an entire message is available. Protocol framing must accumulate partial bytes across events. A selected key can become stale; handle cancellation, closure, and exceptions without spinning. Interest in write readiness should generally be enabled only while pending output exists, or a loop may wake continuously.

### Heap, direct, and mapped buffers

Heap buffers use managed memory and may expose an array. Direct buffers can reduce copying for native transfers, but allocation and reclamation are costlier and memory sits outside normal heap sizing. Mapped buffers expose file regions through virtual memory; page faults, truncation, flush, and unmapping are platform-sensitive. Neither `force` nor mapping creates an application transaction.

> **HotSpot note:** Direct-buffer cleanup, native memory accounting, zero-copy transfer paths, selector providers, and mapped-buffer unmapping are implementation and OS sensitive. Do not depend on internal cleaner APIs.

### Serialization boundaries

Java native serialization reconstructs object graphs for types implementing `Serializable`. It captures private implementation shape, uses `serialVersionUID` for compatibility checks, supports custom hooks, and can invoke code during reconstruction. This makes it brittle for long-lived schemas and dangerous for untrusted data.

Never deserialize an untrusted native object stream merely because classes appear familiar. Apply process and protocol boundaries, strict allow-list filters where legacy serialization is unavoidable, size/depth/reference limits, and current security guidance. Prefer explicit schemas such as validated JSON, protocol buffers, or another designed wire format for service boundaries. Those formats also require input limits and safe parser configuration.

`transient` excludes a field from default serialization; it is not encryption. Constructors of ordinary serializable classes are not invoked in the usual way during reconstruction, so validate invariants in controlled reconstruction paths. A stable `serialVersionUID` prevents only certain version mismatch failures; it does not make schema evolution automatically correct or secure.

> **Specification boundary:** Native serialization defines a protocol and hooks, but class evolution has limited compatibility rules. It is not a general-purpose safe RPC format and should be treated as a legacy trust boundary.

## Worked Java example

This method writes UTF-8 text to a sibling temporary file, forces file content, and attempts atomic publication:

```java
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

final class AtomicTextFile {
    static void replace(Path target, Iterable<String> lines) throws IOException {
        Path absolute = target.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent == null) throw new IOException("target has no parent");
        Files.createDirectories(parent);

        Path temp = Files.createTempFile(parent, ".pending-", ".tmp");
        boolean published = false;
        try {
            try (FileChannel channel = FileChannel.open(
                    temp, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                for (String line : lines) {
                    if (line.indexOf('\n') >= 0 || line.indexOf('\r') >= 0) {
                        throw new IllegalArgumentException("line contains a newline");
                    }
                    byte[] encoded = (line + "\n").getBytes(StandardCharsets.UTF_8);
                    ByteBuffer buffer = ByteBuffer.wrap(encoded);
                    while (buffer.hasRemaining()) {
                        channel.write(buffer);
                    }
                }
                channel.force(true);
            }

            try {
                Files.move(temp, absolute,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                // This fallback preserves complete-file publication but not atomic replacement.
                Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
            published = true;
        } finally {
            if (!published) {
                Files.deleteIfExists(temp);
            }
        }
    }
}
```

Creating the temporary file in the target directory improves the chance that source and target share a filesystem, which is usually necessary for atomic rename. The fallback is an explicit weakening; a system that requires atomicity should propagate failure instead. The example does not claim full crash durability of directory metadata on every platform.

## Execution or memory walkthrough

For lines `alpha` and `beta`:

1. A unique temp file is created in the target directory.
2. `alpha\n` encodes to six UTF-8 bytes and `ByteBuffer.wrap` starts with `position=0`, `limit=6`, `capacity=6`.
3. Each channel write advances position by the actual count. The loop handles partial writes until `position == limit`.
4. The same process writes `beta\n`. Each encoded byte array becomes unreachable after its iteration.
5. `force(true)` requests synchronization of content and metadata for the file channel.
6. Closing the channel releases its descriptor.
7. Atomic move is attempted. Before the namespace update, readers see the old target; after it, they see the complete new target under supporting provider semantics.
8. If writing or moving fails, the `finally` block attempts to remove the temporary file. The original target is not truncated by the write phase.

This design uses memory proportional to the largest encoded line, not the whole file. It still permits an arbitrarily large single line and an infinite iterable; callers may need explicit byte and line limits.

## Complexity and performance

Writing `b` encoded bytes is `O(b)` time and uses `O(maxLineBytes)` temporary memory in the example. Filesystem and device latency dominate CPU complexity. Buffering changes call counts and constants, not the amount of data.

| Task | Typical time | Important caveat |
|---|---:|---|
| sequential read/write | `O(bytes)` | partial operations and device latency |
| path `normalize` | `O(components)` | lexical only |
| directory traversal | `O(entries)` | metadata calls, symlinks, open resources |
| copy or move across filesystems | `O(bytes)` | cannot usually be atomic |
| same-filesystem rename | often near `O(1)` namespace work | provider-dependent atomicity/durability |
| buffer flip/clear | `O(1)` | clear does not erase content |
| buffer compact | `O(unread bytes)` | moves retained content |
| memory map access | `O(bytes touched)` logical | page faults and OS caching dominate |

Many tiny reads and writes pay syscall, locking, TLS, or device overhead repeatedly. Choose buffer sizes empirically. Huge buffers increase latency to first processing, direct-memory use, and concurrent footprint. Zero-copy channel transfers can help large file movement on supported systems but can fall back internally.

## Edge cases and common mistakes

- Relying on the platform default charset for stored or transmitted text.
- Assuming one read fills an array or one write drains a buffer.
- Forgetting that `read` uses `-1` for end-of-stream.
- Leaking streams returned by `Files.list` or `Files.walk`.
- Calling `readAllBytes` on unbounded input.
- Confusing `normalize`, absolute path, and real path.
- Treating a lexical `startsWith` containment check as symlink-safe authorization.
- Assuming `ATOMIC_MOVE` is supported or silently weakening a required guarantee.
- Believing `flush` or close alone guarantees crash durability.
- Calling `clear()` and assuming sensitive bytes were erased.
- Forgetting `flip()` before reading data just written to a buffer.
- Losing partial protocol frames between nonblocking reads.
- Enabling selector write interest continuously and causing a busy loop.
- Allocating unbounded direct buffers and watching only Java heap metrics.
- Mutating shared storage through a sliced or duplicated buffer alias.
- Deserializing native Java objects from untrusted input.
- Treating `transient` as confidentiality or `serialVersionUID` as safe evolution.

## Production engineering notes

Every I/O entry point needs limits: maximum bytes, lines, nesting, files, traversal depth, open handles, concurrency, and time. Use streaming parsing rather than whole-file loading for large content. Reject overlong records before allocating proportional memory.

Make ownership explicit. The layer that opens a resource should normally close it. When APIs accept caller-owned streams, state close and flush behavior. Propagate interruption and cancellation through blocking workflows; configure socket connect/read deadlines and handle stalled peers.

Use temp-write plus rename for complete-file publication, with a documented policy when atomic move is unavailable. Separate atomic visibility from crash durability; use a storage engine when durable transactions are required.

Protect endpoints with least-privilege permissions, trusted roots, generated names, and symlink-aware operations. Validate archive paths and decompression ratios.

Measure bytes, latency, open handles, failures, direct memory, and rejected input. Avoid logging payloads. Isolate legacy native serialization with narrow filters, graph limits, an allow-list, and a migration plan.

## Interview questions and model answers

**When do you use a Reader instead of an InputStream?**

Use a reader when the data is text and must be decoded with a known charset. Use an input stream for raw bytes. An `InputStreamReader` bridges them while handling multibyte boundaries.

**Why must channel writes be in a loop?**

A successful write may consume fewer than all remaining bytes, especially for sockets or nonblocking channels. Advance is reflected in buffer position, so loop while `hasRemaining`, subject to readiness and timeout policy.

**Explain `flip`, `clear`, and `compact`.**

`flip` changes from filling to draining by setting limit to current position and position to zero. `clear` prepares all storage for overwrite without erasing it. `compact` preserves unread bytes, moves them to the front, and prepares the remaining area for more input.

**Is `Path.normalize()` sufficient to prevent path traversal?**

No. It is a lexical operation. It can reject simple `..` escapes but does not resolve symlinks or remove validation-use races. Security-sensitive access needs a trusted-root and filesystem-aware strategy.

**Does try-with-resources lose close exceptions?**

If work throws, close exceptions are attached as suppressed exceptions to the primary failure. Resources close in reverse declaration order.

**Why is Java native deserialization dangerous?**

It reconstructs graphs and can invoke class-specific code from attacker-controlled stream content. Crafted graphs can abuse gadget paths or exhaust resources. Do not use it for untrusted data; tightly filter and bound unavoidable legacy uses.

**Do virtual threads replace NIO?**

They make blocking-style concurrency practical for many workloads, but do not remove protocol framing, timeouts, capacity, partial I/O, or the value of event loops in specialized systems.

## Exercises

1. Write a byte-copy loop that handles partial reads and writes with a reusable `ByteBuffer`. Dry-run its indexes for a split read.
2. Decode UTF-8 using a strict `CharsetDecoder` and reject malformed input.
3. Compare `normalize`, `toAbsolutePath`, and `toRealPath` for a path containing `..` and a symbolic link.
4. Implement a bounded line reader that rejects more than one megabyte or ten thousand lines.
5. Modify the worked example so atomic move support is mandatory. Explain the operational consequence.
6. Design a nonblocking length-prefixed message decoder that preserves incomplete header and body bytes across reads.
7. Threat-model archive extraction, including traversal, symlinks, entry count, and decompression bombs.
8. Propose a migration plan from a native serialized cache to a versioned explicit schema.

## Chapter summary

Java I/O begins with byte semantics, explicit character encoding, partial transfer, and resource ownership. Classic streams compose decorators; NIO channels move data through buffers governed by position, limit, and capacity. `Path` is a representation, while filesystem identity, symlinks, atomic moves, and durability are provider concerns. Direct and mapped buffers trade heap copying for native lifecycle complexity. Every external format is a trust boundary, and native Java deserialization is especially risky. Production I/O must be bounded, observable, charset-explicit, and honest about atomicity and durability.

## Revision checklist

- [ ] I choose byte versus character APIs and always define external charsets.
- [ ] I handle partial reads/writes, EOF, buffering, and close ownership.
- [ ] I understand suppressed exceptions in try-with-resources.
- [ ] I distinguish lexical, absolute, and real paths.
- [ ] I can explain atomic visibility versus crash durability.
- [ ] I can dry-run `position`, `limit`, and `capacity` through flip, clear, rewind, and compact.
- [ ] I understand shared content in buffer slices and duplicates.
- [ ] I can explain selector readiness and partial protocol framing.
- [ ] I monitor direct/native memory as well as heap.
- [ ] I reject unbounded input and unsafe native deserialization.
