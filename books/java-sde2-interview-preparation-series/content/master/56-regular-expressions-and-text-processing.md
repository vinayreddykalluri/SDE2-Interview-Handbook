# 56. Regular Expressions and Text Processing

## Learning objectives

By the end of this chapter, you should be able to:

- use `Pattern` and `Matcher` correctly, including their very different threading rules;
- explain greedy, reluctant, and possessive quantifiers and choose between them deliberately;
- recognize catastrophic backtracking and rewrite a pattern to eliminate it;
- apply groups, named groups, lookaround, and boundaries without over-reaching; and
- decide when a regular expression is the wrong tool.

## Why this matters at SDE-2

Regular expressions appear in log parsing, input validation, routing rules, data migration, and every ad-hoc extraction task that outlives its author. They are also one of the few places where a single line of application code can take down a service: a pattern that runs in microseconds on typical input can run for hours on a crafted string, and the thread is not interruptible while it does.

The SDE-2 signal is not whether you can recall the syntax for a character class. It is whether you can reason about how the engine executes the pattern, whether you know that `String.split` compiles a pattern on every call, and whether you can tell an interviewer why `(a+)+b` is a denial-of-service vector.

## First-principles model

Java's `java.util.regex` is a **backtracking** engine. It does not build a deterministic automaton and run the input through it once. It walks the pattern, and whenever a construct could match in more than one way, it takes one branch, continues, and - if the rest of the pattern fails - returns and tries the next alternative.

That single design fact explains almost everything that matters in practice. Backtracking is what makes backreferences and lookaround expressible. It is also what makes the worst case exponential rather than linear, because a pattern with nested ambiguous quantifiers can have exponentially many ways to divide the same substring, and a failing match forces the engine to try all of them.

A `Pattern` is a compiled, immutable, thread-safe program. A `Matcher` is the mutable execution state for one pattern against one input: current position, group boundaries, and the backtracking stack. Compiling is expensive; matching is cheap; sharing a `Matcher` across threads is a data race.

> **Specification boundary:** Java specifies the pattern syntax, the greediness of each quantifier, group numbering, and the semantics of the flags. It does not specify a time bound. Match time is a property of the engine's backtracking strategy, not of the language, and no timeout facility is built in - a runaway match is not interruptible by `Thread.interrupt`.

## Core terminology

- **Pattern:** compiled immutable regular expression; thread-safe and reusable.
- **Matcher:** mutable, single-threaded engine state for one input.
- **Greedy quantifier:** `*`, `+`, `?` - consume as much as possible, then give back on failure.
- **Reluctant quantifier:** `*?`, `+?`, `??` - consume as little as possible, then take more.
- **Possessive quantifier:** `*+`, `++`, `?+` - consume maximally and never give back.
- **Atomic group:** `(?>...)` - once matched, discards its backtracking alternatives.
- **Capturing group:** `(...)` - numbered left to right by opening parenthesis.
- **Named group:** `(?<name>...)` - retrieved by name rather than index.
- **Lookahead / lookbehind:** `(?=)`, `(?!)`, `(?<=)`, `(?<!)` - zero-width assertions.
- **Catastrophic backtracking:** exponential match time from nested ambiguous quantifiers.
- **Region:** the sub-range of input a `Matcher` is restricted to.

## Detailed mechanics

### Pattern is shared; Matcher is not

This is the single most important operational rule.

```java
public final class LogParser {
    // Compiled once. Immutable. Safe to share across every thread.
    private static final Pattern LEVEL =
            Pattern.compile("^(?<level>TRACE|DEBUG|INFO|WARN|ERROR)\\s+(?<rest>.*)$");

    public Optional<String> level(String line) {
        Matcher matcher = LEVEL.matcher(line);   // fresh Matcher per call
        return matcher.matches() ? Optional.of(matcher.group("level")) : Optional.empty();
    }
}
```

Hoisting the `Pattern` to a `static final` field is not micro-optimization. `Pattern.compile` parses the expression and builds a node tree; doing that per request in a hot path is often the largest single cost in a parsing routine.

The convenience methods hide this. `String.matches`, `String.split`, and `String.replaceAll` all call `Pattern.compile` internally on every invocation. In a loop, replace them with a hoisted pattern.

```java
// Recompiles the pattern on every iteration.
for (String line : lines) {
    if (line.matches("\\d{4}-\\d{2}-\\d{2}")) { ... }
}

// Compiles once.
private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
for (String line : lines) {
    if (ISO_DATE.matcher(line).matches()) { ... }
}
```

`String.split` has one documented fast path: a single-character, non-metacharacter separator bypasses the regex engine entirely. `split(",")` is cheap; `split("\\s*,\\s*")` is not.

### matches, find, and lookingAt

Three methods, three anchoring rules, and a frequent source of wrong results.

- `matches()` - the pattern must consume the **entire** input.
- `lookingAt()` - the pattern must match at the **start**, need not reach the end.
- `find()` - the pattern may match **anywhere**, and successive calls advance through the input.

```java
Pattern digits = Pattern.compile("\\d+");
Matcher m = digits.matcher("abc123def456");

m.matches();     // false - the whole string is not digits
m.reset().lookingAt();  // false - does not start with digits
m.reset();
while (m.find()) {
    System.out.println(m.group() + " at " + m.start());  // 123 at 3, 456 at 9
}
```

A validation method that uses `find()` where it meant `matches()` accepts every string containing a valid fragment. This is how "email validation" regularly accepts `not-an-email a@b.co garbage`.

### Greedy, reluctant, and possessive

Consider extracting the content of the first HTML-like tag from `<a><b>`.

```java
Pattern greedy    = Pattern.compile("<(.+)>");    // group: "a><b"
Pattern reluctant = Pattern.compile("<(.+?)>");   // group: "a"
Pattern possessive= Pattern.compile("<(.++)>");   // no match at all
```

The greedy `.+` consumes to the end, then backs off one character at a time until the trailing `>` can match - landing on the last `>`. The reluctant `.+?` starts with one character and grows only as needed, landing on the first `>`. The possessive `.++` consumes everything and refuses to give any back, so the final `>` has nothing left to match and the whole attempt fails.

Possessive quantifiers and atomic groups are the deliberate tools for bounding backtracking. They say: once this part has matched, never reconsider it. When you know a sub-expression's match is unambiguous, making it possessive converts a potential exponential blowup into a linear scan.

### Catastrophic backtracking

The classic failing shape is a quantifier applied to something that is itself ambiguously quantified, followed by something that fails.

```java
Pattern bad = Pattern.compile("(a+)+b");
bad.matcher("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa!").matches();
```

The input has thirty `a` characters and no `b`. The engine must prove no match exists. `(a+)+` can partition those thirty characters into groups in exponentially many ways, and the trailing `b` fails for every one of them. Match time roughly doubles per added `a`. At thirty characters this takes seconds; at forty it outlives the request timeout; the thread is pinned and cannot be interrupted.

Three fixes, in order of preference:

```java
Pattern fixed1 = Pattern.compile("a+b");        // remove the redundant nesting
Pattern fixed2 = Pattern.compile("(?>a+)+b");   // atomic group: no re-partitioning
Pattern fixed3 = Pattern.compile("(a++)+b");    // possessive: same effect
```

The first is usually available and always best - the nesting was adding nothing. Reach for atomic groups when the structure is genuinely needed.

The engineering rule: any pattern applied to untrusted input should either be provably linear or be run with a bound. Since Java offers no timeout, the practical bound is a `CharSequence` wrapper whose `charAt` throws after a budget is exhausted, or running the match on a task you can abandon while accepting the pinned thread.

### Groups, named groups, and numbering

Groups are numbered by the position of their **opening** parenthesis, left to right, starting at 1. Group 0 is the entire match. Nesting does not reset the count.

```java
Pattern p = Pattern.compile("((\\d{4})-(\\d{2}))-(\\d{2})");
Matcher m = p.matcher("2026-08-02");
m.matches();
m.group(0);  // 2026-08-02
m.group(1);  // 2026-08
m.group(2);  // 2026
m.group(3);  // 08
m.group(4);  // 02
```

Counting parentheses is a maintenance hazard: inserting a group anywhere shifts every later index. Named groups remove the problem.

```java
Pattern p = Pattern.compile("(?<year>\\d{4})-(?<month>\\d{2})-(?<day>\\d{2})");
Matcher m = p.matcher("2026-08-02");
if (m.matches()) {
    String year = m.group("year");
}
```

Use `(?:...)` for grouping without capturing. It expresses intent and avoids allocating group state you will not read.

An unmatched group returns `null`, not an empty string - a routine `NullPointerException` source when a group sits behind an optional branch.

### Lookaround

Lookaround asserts without consuming. The position does not advance, and the assertion contributes nothing to the match text.

```java
Pattern beforeK = Pattern.compile("\\d+(?=k)");   // digits followed by k
Pattern notK    = Pattern.compile("\\d+(?!k)");   // digits not followed by k
Pattern afterUsd= Pattern.compile("(?<=\\$)\\d+");// digits preceded by $
```

Java requires lookbehind to be **bounded** in length - `(?<=a{1,5})` is legal, `(?<=a*)` is not. Lookahead has no such restriction.

Lookahead is the honest way to express "all of these must hold" without dictating order, which is why password-policy checks read as a chain of assertions rather than one tangled alternation.

### Flags and Unicode

Flags may be passed to `compile` or embedded inline as `(?i)`.

- `CASE_INSENSITIVE` is ASCII-only unless combined with `UNICODE_CASE`.
- `DOTALL` makes `.` match line terminators; by default it does not.
- `MULTILINE` makes `^` and `$` match at line boundaries rather than only input boundaries.
- `COMMENTS` allows whitespace and `#` comments inside the pattern - genuinely worth using for anything long.

The default word boundary `\b` and shorthand classes such as `\w` are ASCII-oriented. `UNICODE_CHARACTER_CLASS` redefines them against Unicode properties, which matters the moment real names or non-English text arrive.

```java
Pattern ascii   = Pattern.compile("\\w+");
Pattern unicode = Pattern.compile("\\w+", Pattern.UNICODE_CHARACTER_CLASS);
// On a string containing "naive" spelled with a diaeresis over the i,
// the ASCII pattern finds two fragments, "na" and "ve", because the accented
// character is not an ASCII word character. The Unicode pattern finds the
// whole word. The same split happens to any name outside ASCII.
```

Because Java strings are UTF-16, a character outside the Basic Multilingual Plane is two `char` values. `.` matches a full code point, but a hand-rolled class such as `[a-zA-Z]` reasons in code units and will happily split a surrogate pair.

### Replacement pitfalls

In the replacement string of `replaceAll`, `$` introduces a group reference and `\` escapes. Any user-supplied replacement text must be escaped, or a stray `$1` in the data becomes a group reference and a stray `$` throws.

```java
String replacement = Matcher.quoteReplacement(userInput);
String result = pattern.matcher(source).replaceAll(replacement);
```

Symmetrically, `Pattern.quote` escapes user input used as a *pattern*, turning it into a literal. Building a pattern by concatenating unescaped user input is regular-expression injection: it is at best a wrong result and at worst a supplied `(a+)+b` that hangs the thread.

Prefer `replaceAll(Function<MatchResult, String>)` when the replacement is computed, since it avoids the escaping rules entirely.

## Worked Java example

Extracting structured fields from a log line, with the pattern hoisted, named groups, and a bounded quantifier.

```java
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AccessLogParser {

    // Anchored, no nested quantifiers, bounded status field.
    private static final Pattern ENTRY = Pattern.compile(
            "^(?<ip>[0-9.]{7,15})\\s+"
          + "\\[(?<timestamp>[^\\]]++)\\]\\s+"      // possessive: content is unambiguous
          + "\"(?<method>[A-Z]{3,7})\\s(?<path>[^\"]++)\"\\s+"
          + "(?<status>\\d{3})\\s+"
          + "(?<bytes>\\d++)$");

    public record Entry(String ip, String timestamp, String method,
                        String path, int status, long bytes) {}

    public Optional<Entry> parse(String line) {
        Matcher m = ENTRY.matcher(line);
        if (!m.matches()) {
            return Optional.empty();
        }
        return Optional.of(new Entry(
                m.group("ip"),
                m.group("timestamp"),
                m.group("method"),
                m.group("path"),
                Integer.parseInt(m.group("status")),
                Long.parseLong(m.group("bytes"))));
    }

    public static void main(String[] args) {
        AccessLogParser parser = new AccessLogParser();
        String line = "10.0.0.7 [02/Aug/2026:14:22:01 +0000] \"GET /api/orders\" 200 4821";
        System.out.println(parser.parse(line).orElseThrow());
        // Entry[ip=10.0.0.7, timestamp=02/Aug/2026:14:22:01 +0000,
        //       method=GET, path=/api/orders, status=200, bytes=4821]
        System.out.println(parser.parse("garbage").isPresent()); // false
    }
}
```

Three deliberate choices. `matches()` rather than `find()`, so a partially valid line is rejected rather than silently accepted. The quantifiers are possessive, because each field's extent is fixed by its delimiter - there is no legitimate reason for the engine to reconsider them. Named groups mean adding a field later cannot renumber the existing ones.

The `path` group is worth dwelling on, because the obvious spelling is wrong. Writing it as `\\S++` looks natural - a path contains no whitespace - but `\"` is also not whitespace, so the possessive quantifier consumes the closing quote and then refuses to give it back. The pattern fails on every well-formed line. Negating the actual delimiter with `[^\"]++` is correct: a possessive quantifier is only safe when its character class genuinely cannot overlap what follows it. This is the possessive trade-off in miniature - you gain a guarantee against backtracking and you give up the engine's ability to rescue an imprecise class.

## Execution or memory walkthrough

`Pattern.compile` runs once at class initialization. It parses the expression into a linked tree of node objects - one per construct - and stores the group count and name-to-index map. This tree is immutable, which is what makes the `Pattern` shareable.

`ENTRY.matcher(line)` allocates a `Matcher` holding a reference to the input `CharSequence`, an int array of group boundaries sized to the group count, and the append position. Nothing is copied from the input.

`matches()` anchors at position 0 and requires the terminal node to sit at the input end. The engine walks the node tree, each node advancing the position and calling the next. When `[A-Z]{3,7}` matches `GET` and the following node fails, a greedy node would restore the saved position and retry at a shorter length; the possessive nodes in this pattern discard that saved state immediately, so those regions are scanned exactly once.

`m.group("ip")` resolves the name to an index, reads the start and end offsets from the int array, and calls `subSequence` - the only string allocation in the whole parse, and only for groups actually read. This is why unnecessary capturing groups cost more than `(?:...)`: the boundary bookkeeping happens whether or not you read them.

## Complexity and performance

For a pattern without ambiguous nesting, matching is O(n-m) in input length and pattern size, and in practice close to linear. For a pattern with nested ambiguous quantifiers, the worst case is **exponential** in input length, and the bad case is reached by a failing match, not a successful one - so tests built from valid inputs never find it.

Compilation is the dominant cost in code that recompiles. A hoisted `static final Pattern` removes it entirely.

Allocation comes from group extraction and from `split`, which builds an array of substrings. Where only presence matters, `find()` without calling `group()` allocates nothing beyond the `Matcher`.

> **HotSpot note:** the matcher's inner loop is ordinary Java and inlines well, so a simple pattern over short input is genuinely fast. JIT compilation does not change algorithmic behavior: no amount of warmup rescues a pattern whose backtracking is exponential.

## Edge cases and common mistakes

- Compiling inside a loop or a request handler instead of hoisting to `static final`.
- Using `find()` for validation where `matches()` was meant, accepting any string with a valid fragment.
- Nested ambiguous quantifiers such as `(a+)+`, `(a*)*`, or `(\\s+)+` on untrusted input.
- Assuming a runaway match can be interrupted. It cannot; `Thread.interrupt` has no effect.
- Building a pattern from unescaped user input - regex injection. Use `Pattern.quote`.
- Forgetting `Matcher.quoteReplacement` for user-supplied replacement text containing `$` or `\`.
- Reading a group that did not participate in the match and dereferencing the `null`.
- Counting group numbers by hand, then inserting a group and shifting every later index.
- Expecting `\\w` and `\\b` to handle non-ASCII without `UNICODE_CHARACTER_CLASS`.
- Expecting `.` to cross a line terminator without `DOTALL`.
- Using an unbounded lookbehind, which Java rejects at compile time.
- Sharing a `Matcher` between threads. `Pattern` is safe; `Matcher` is not.
- Reaching for a regular expression to parse HTML, JSON, or any nested structure - these are not regular languages, and no pattern parses them correctly.
- Using `split` on a single-character separator with an escaped metacharacter, losing the fast path for no reason.

## Production engineering notes

Treat every pattern that touches untrusted input as a potential availability risk. Review it for nested quantifiers, prefer possessive or atomic forms where the extent is unambiguous, and fuzz it with long non-matching inputs - the failing case is the dangerous one.

Keep patterns in named constants next to the code that uses them, with `COMMENTS` mode and inline documentation once they exceed a line. A pattern nobody can read is a pattern nobody can safely change.

Prefer a real parser when the input has structure. CSV with embedded quotes, HTML, and JSON all have grammars that regular expressions cannot express, and the "almost working" pattern is worse than an obvious dependency because it fails on the rare row.

Where a regex is user-configurable - routing rules, alert filters, redaction patterns - you have accepted arbitrary code with unbounded runtime from your users. Validate on submission, keep the evaluation off the request thread, and constrain what the configuration surface allows.

Log the pattern name rather than the pattern text when reporting a match failure, and never log the input if it may contain secrets. Redaction patterns in particular tend to run over exactly the data you must not emit.

## Interview questions and model answers

**Is `Pattern` thread-safe? Is `Matcher`?**

`Pattern` is immutable and safe to share; the standard practice is one `static final` instance per pattern. `Matcher` holds mutable position and group state, so each thread must create its own via `pattern.matcher(input)`.

**What is catastrophic backtracking and how do you fix it?**

A pattern with nested ambiguous quantifiers, such as `(a+)+b`, can partition the same input exponentially many ways. On a failing match the engine tries all of them, so time doubles with each added character. Fix it by removing the redundant nesting, or by making the inner quantifier possessive or the group atomic so the engine cannot re-partition.

**What is the difference between greedy, reluctant, and possessive quantifiers?**

Greedy consumes as much as possible and gives back on failure. Reluctant consumes as little as possible and takes more on failure. Possessive consumes maximally and never gives back, which prevents backtracking and can turn an exponential pattern into a linear one - at the cost of failing some matches a greedy quantifier would find.

**Why is `String.matches` a problem in a loop?**

It compiles the pattern on every call. Hoist a `static final Pattern` and reuse it; compilation is far more expensive than the match for short inputs.

**How does `matches()` differ from `find()`?**

`matches()` requires the pattern to consume the entire input; `find()` searches for a match anywhere and can be called repeatedly to iterate. Using `find()` for validation is a common defect, since it accepts any input containing a valid substring.

**Can you parse HTML with a regular expression?**

No. HTML is arbitrarily nested and regular expressions cannot count nesting depth. A pattern can extract from a known fixed shape, but any general parsing needs a real parser.

## Exercises

1. Write `(a+)+b`, time it against inputs of 20, 25, and 30 `a` characters with no `b`, and plot the growth. Then fix it three ways and re-measure.
2. Convert a validation method that uses `find()` to one that uses `matches()`, and find an input whose acceptance changes.
3. Take a pattern with six numbered groups, convert it to named groups, then insert a new group at the front and confirm nothing else breaks.
4. Demonstrate the difference between `\\w+` with and without `UNICODE_CHARACTER_CLASS` on a string containing accented characters and an emoji.
5. Write a `replaceAll` whose replacement text comes from user input containing `$1`, observe the failure, then fix it with `Matcher.quoteReplacement`.
6. Benchmark `split(",")` against `split("\\s*,\\s*")` on a large file and explain the gap using the single-character fast path.
7. Rewrite the `AccessLogParser` pattern with greedy quantifiers throughout and measure the difference on ten thousand lines, including malformed ones.

## Chapter summary

Java's regex engine backtracks, and that one fact drives everything practical about it. `Pattern` is immutable, expensive to build, and meant to be hoisted and shared; `Matcher` is mutable, cheap, and strictly per-thread. Greedy quantifiers give back on failure, reluctant ones grow on demand, and possessive quantifiers and atomic groups refuse to reconsider - which is the primary defense against catastrophic backtracking, an exponential failure mode that only appears on inputs that *fail* to match and therefore survives most test suites. Anchor validation with `matches()` rather than `find()`, prefer named groups over hand-counted indices, escape user input with `Pattern.quote` and replacements with `Matcher.quoteReplacement`, and enable Unicode classes when real-world text arrives. When the input has nested structure, use a parser: the correct regular expression does not exist.

## Revision checklist

- [ ] I hoist every pattern to a `static final` field and never compile in a loop.
- [ ] I know `Pattern` is thread-safe and `Matcher` is not.
- [ ] I can explain `matches()`, `find()`, and `lookingAt()` and pick the right one for validation.
- [ ] I can predict what greedy, reluctant, and possessive quantifiers extract from `<a><b>`.
- [ ] I can recognize a catastrophic pattern and fix it three ways.
- [ ] I know a runaway match cannot be interrupted.
- [ ] I use named groups and `(?:...)` rather than counting parentheses.
- [ ] I escape user input with `Pattern.quote` and replacements with `Matcher.quoteReplacement`.
- [ ] I know when `\\w` and `\\b` need `UNICODE_CHARACTER_CLASS`.
- [ ] I can explain why regular expressions cannot parse HTML.
