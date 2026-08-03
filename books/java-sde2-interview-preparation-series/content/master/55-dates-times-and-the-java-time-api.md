# 55. Dates, Times, Zones, and the java.time API

## Learning objectives

By the end of this chapter, you should be able to:

- choose the correct `java.time` type for an instant, a local date, a wall-clock appointment, and a duration;
- explain the difference between `Instant`, `LocalDateTime`, `OffsetDateTime`, and `ZonedDateTime`;
- reason about time zones, daylight-saving gaps and overlaps, and the tzdb update cycle;
- apply `Duration`, `Period`, and `ChronoUnit` without conflating machine time and calendar time; and
- design storage, serialization, and test boundaries so that time is an injected dependency rather than ambient state.

## Why this matters at SDE-2

Time bugs are rarely caught by unit tests and are almost always found in production. A scheduled job fires twice on the night the clocks go back. A subscription renews an hour early for users in one region. A report joins two tables whose timestamps mean different things, and nobody notices until the totals disagree at a quarter boundary.

The legacy `java.util.Date` and `Calendar` types encouraged these failures: `Date` is a mutable instant with a misleading name, `Calendar` months are zero-based, and neither type distinguishes a point on the timeline from a local calendar reading. `java.time` replaced them with immutable types whose names state their meaning. At SDE-2, the interview question is usually "how do you store a timestamp," and the correct answer depends on whether the value is an event that happened or an appointment that will happen.

## First-principles model

There are two fundamentally different kinds of time, and most defects come from using one where the other is required.

**Machine time** is a point on a continuous timeline, independent of any calendar or location. `Instant` models this: a count of seconds and nanoseconds from the 1970-01-01T00:00:00Z epoch. Two observers anywhere in the world agree on an `Instant`.

**Calendar time** is a human reading - a date and a wall-clock time as displayed by a local calendar. `LocalDate`, `LocalTime`, and `LocalDateTime` model this. They carry no zone and therefore do not identify a point on the timeline. `2026-03-08T02:30` is a valid `LocalDateTime` and, in several time zones, an instant that never existed.

A zone converts between the two. `ZoneId` names a region with a full history of offset rules ("America/New_York"); `ZoneOffset` is a fixed displacement from UTC ("-05:00"). A region is not an offset: New York is `-05:00` in January and `-04:00` in July. `ZonedDateTime` is a local date-time plus a zone plus the resolved offset, so it identifies a real instant and remembers the rules that produced it.

> **Specification boundary:** Java specifies the ISO-8601 calendar system, the arithmetic of each `java.time` type, and the resolution strategy for daylight-saving gaps and overlaps. It does not specify the contents of the time-zone database. Zone rules are political, change several times a year, and ship as tzdb data inside the JDK or from `-Djava.time.zone.DefaultZoneRulesProvider`. Correct code can produce wrong answers on a stale runtime.

## Core terminology

- **Instant:** a point on the timeline, in UTC, with nanosecond field precision.
- **LocalDate / LocalTime / LocalDateTime:** calendar readings with no zone and no offset.
- **ZoneId:** a region whose UTC offset varies over time according to tzdb rules.
- **ZoneOffset:** a fixed offset from UTC; a `ZoneId` subtype with no rule history.
- **OffsetDateTime:** a local date-time plus a fixed offset - a real instant, no rule history.
- **ZonedDateTime:** a local date-time plus a `ZoneId` plus the resolved offset.
- **Duration:** machine-time amount measured in seconds and nanoseconds.
- **Period:** calendar-time amount measured in years, months, and days.
- **Gap:** a local time that does not exist because the clocks sprang forward.
- **Overlap:** a local time that occurs twice because the clocks fell back.
- **Clock:** the injectable abstraction supplying "now" and the default zone.

## Detailed mechanics

### Choosing the type

The decision is driven by what the value means, not by what is convenient to store.

| The value is | Use | Reason |
|---|---|---|
| Something that happened - a log line, an audit record, a payment capture | `Instant` | The event has one unambiguous time; local readings are a display concern. |
| A birthday, an invoice date, a holiday | `LocalDate` | There is no time and no zone; the date is the same fact everywhere. |
| A store's opening time, a recurring alarm | `LocalTime` | The wall-clock reading is the requirement, whatever the offset that day. |
| A future appointment in a named place | `ZonedDateTime` | The user means "9 a.m. in Chicago," which must survive a rule change. |
| An API or database timestamp with a known offset | `OffsetDateTime` | Unambiguous instant, and it round-trips through `TIMESTAMP WITH TIME ZONE`. |
| An elapsed measurement or a timeout | `Duration` | Machine time; must not be affected by calendar arithmetic. |
| "One month later" on a billing cycle | `Period` | Calendar time; the day count deliberately varies by month. |

The two rows most often confused are the first and the fourth. Store a *past* event as an `Instant`. Store a *future* appointment as a `ZonedDateTime` or as a `LocalDateTime` plus a `ZoneId` column. If you collapse a future appointment to an `Instant` at write time and the region later changes its rules, the meeting silently moves.

### Immutability and the wither pattern

Every `java.time` value is immutable and thread-safe. Mutating methods do not exist; `plusDays`, `withHour`, and `truncatedTo` return new values.

```java
LocalDate date = LocalDate.of(2026, 1, 31);
date.plusMonths(1);              // result discarded - a common bug
LocalDate next = date.plusMonths(1);   // 2026-02-28
```

Note the clamping in the second call. Adding one calendar month to 31 January yields 28 February, because month arithmetic resolves to the last valid day. This is intentional and specified, but it is not reversible: `date.plusMonths(1).minusMonths(1)` is 28 January, not 31 January. Any billing logic that assumes round-tripping is wrong.

### Duration versus Period

`Duration` counts elapsed seconds. `Period` counts calendar fields. On a daylight-saving boundary they disagree, and that disagreement is the point.

```java
ZoneId chicago = ZoneId.of("America/Chicago");
ZonedDateTime before = ZonedDateTime.of(
        LocalDate.of(2026, 3, 7), LocalTime.of(12, 0), chicago);

ZonedDateTime plusPeriod = before.plus(Period.ofDays(1));   // 2026-03-08T12:00-05:00
ZonedDateTime plusDuration = before.plus(Duration.ofDays(1)); // 2026-03-08T13:00-05:00
```

`Period.ofDays(1)` means "same wall-clock time tomorrow" and consumes 23 real hours across the spring-forward boundary. `Duration.ofDays(1)` means "86,400 seconds later" and lands an hour further along the clock. A daily 12:00 job must use `Period` or a `ZonedDateTime` field addition; a one-day cache expiry must use `Duration`.

### Gaps and overlaps

When a local date-time does not exist, `ZonedDateTime.of` does not throw. It shifts the result forward by the size of the gap.

```java
ZoneId chicago = ZoneId.of("America/Chicago");
LocalDateTime springForward = LocalDateTime.of(2026, 3, 8, 2, 30);
ZonedDateTime resolved = ZonedDateTime.of(springForward, chicago);
// 2026-03-08T03:30-05:00 - 02:30 never occurred
```

When a local date-time occurs twice, `of` selects the **earlier** offset. `withLaterOffsetAtOverlap()` selects the other. Silently picking the earlier one is right for most displays and wrong for anything that must not fire twice; consult `ZoneRules.getValidOffsets` when the distinction matters.

```java
ZoneRules rules = chicago.getRules();
LocalDateTime fallBack = LocalDateTime.of(2026, 11, 1, 1, 30);
List<ZoneOffset> valid = rules.getValidOffsets(fallBack);
// size 0 -> gap, size 1 -> normal, size 2 -> overlap
```

A scheduler that treats "size 2" as ordinary will run the 01:30 job twice each November.

### Parsing, formatting, and the locale trap

`DateTimeFormatter` is immutable and thread-safe - unlike `SimpleDateFormat`, which is not and which caused a long tail of production corruption when shared across threads.

Formatters default to the JVM's locale unless told otherwise. `DateTimeFormatter.ofPattern("MMM d")` produces "Jan 5" on one host and a localized month name on another. Always pin the locale for machine-readable output, and prefer the predefined ISO constants for wire formats.

```java
DateTimeFormatter machine = DateTimeFormatter.ISO_INSTANT;
DateTimeFormatter human = DateTimeFormatter
        .ofPattern("d MMMM yyyy", Locale.US)
        .withZone(ZoneId.of("America/Chicago"));
```

The pattern letters are a frequent source of defects: `yyyy` is the calendar year and `YYYY` is the week-based year. They differ for a few days each January, which is why "the dashboard showed 2025 for three days" is a recurring new-year incident.

### Clock injection

`Instant.now()` reads the system clock and the default zone. Both are ambient global state, and code that calls them directly cannot be tested deterministically without changing the machine.

`Clock` is the seam. Inject it, default it to `Clock.systemUTC()`, and substitute a fixed clock in tests.

```java
public final class TrialService {
    private final Clock clock;

    public TrialService(Clock clock) {
        this.clock = clock;
    }

    public boolean expired(Instant startedAt, Duration trialLength) {
        return Instant.now(clock).isAfter(startedAt.plus(trialLength));
    }
}
```

`Clock.fixed(instant, zone)` freezes time; `Clock.offset(base, duration)` shifts it. Neither requires mocking a static method, which is why this design is worth the one extra constructor parameter.

## Worked Java example

A billing service that renews a subscription must add a calendar month in the customer's own zone, then resolve to an instant for storage.

```java
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class RenewalSchedule {
    private final Clock clock;

    public RenewalSchedule(Clock clock) {
        this.clock = clock;
    }

    /**
     * Next renewal at the same local billing hour, one calendar month later,
     * resolved in the customer's zone. Returned as an Instant because the
     * scheduler compares against a monotonic timeline.
     */
    public Instant nextRenewal(LocalDate anchorDate, LocalTime billingHour, ZoneId customerZone) {
        ZonedDateTime current = ZonedDateTime.of(anchorDate, billingHour, customerZone);
        ZonedDateTime next = current.plus(Period.ofMonths(1));
        return next.toInstant();
    }

    public boolean dueNow(Instant renewalAt) {
        return !Instant.now(clock).isBefore(renewalAt);
    }

    public static void main(String[] args) {
        RenewalSchedule schedule = new RenewalSchedule(Clock.systemUTC());
        ZoneId chicago = ZoneId.of("America/Chicago");

        Instant fromJan31 = schedule.nextRenewal(
                LocalDate.of(2026, 1, 31), LocalTime.of(2, 30), chicago);
        System.out.println(fromJan31); // 2026-02-28T08:30:00Z

        Instant acrossDst = schedule.nextRenewal(
                LocalDate.of(2026, 2, 8), LocalTime.of(2, 30), chicago);
        System.out.println(acrossDst); // 2026-03-08T08:30:00Z
    }
}
```

The first call demonstrates month clamping: 31 January has no counterpart in February, so the renewal lands on the 28th. The second call crosses the spring-forward boundary; 02:30 on 8 March does not exist in Chicago, so the resolution rule shifts it to 03:30 local, which is 08:30Z.

## Execution or memory walkthrough

`ZonedDateTime.of(anchorDate, billingHour, customerZone)` builds a `LocalDateTime`, asks `ZoneRules` for the valid offsets of that local value, and stores the local value, the zone, and the chosen offset as three fields. No lookup table is copied; `ZoneRules` instances are shared per zone.

`plus(Period.ofMonths(1))` operates on the **local** fields first. It adds one to the month, clamps the day to the month length, and then re-resolves the offset against the zone rules. This ordering is what makes the result "the same wall-clock time next month" rather than "a fixed number of seconds later."

`toInstant()` subtracts the resolved offset from the local date-time and converts to epoch seconds. The returned `Instant` holds two primitive fields - a long of seconds and an int of nanoseconds - and carries no zone. The zone information is deliberately discarded, which is why the *future* appointment case needs the `ZonedDateTime` retained rather than only its instant.

Every intermediate value here is a separate immutable object. The allocation is real but small and short-lived; these objects die in the young generation.

## Complexity and performance

All field arithmetic is O(1). Offset resolution is a binary search over the zone's transition table - O(log t) in the number of historical transitions, typically a few hundred - and the table is loaded once per zone and cached.

`ZoneId.of` and `DateTimeFormatter.ofPattern` are the expensive calls. Both parse and build structures that should be hoisted to `static final` fields rather than constructed per request. Formatting and parsing dominate any realistic profile of date-heavy code; the arithmetic does not.

`Instant` comparison is a long comparison. Prefer comparing instants over comparing formatted strings, which is both slower and wrong across offsets.

> **HotSpot note:** `java.time` values are ordinary heap objects. They are good escape-analysis candidates - a `LocalDate` created and consumed inside one inlined method may be scalar-replaced - but this is an optimization, not a guarantee. Do not restructure clear date logic to avoid allocation without a benchmark showing the allocation matters.

## Edge cases and common mistakes

- Treating `LocalDateTime` as an instant. It has no zone; converting it assumes a default that varies by host.
- Storing a future appointment as an `Instant`, losing the intent when zone rules change.
- Using `ZoneOffset` where `ZoneId` is required, freezing an offset that is only correct half the year.
- Assuming `plusMonths(1).minusMonths(1)` is the identity. Month clamping is lossy.
- Using `Duration.ofDays` for calendar days across a DST boundary.
- Sharing a `SimpleDateFormat` across threads. Migrate to `DateTimeFormatter`, which is immutable.
- `YYYY` instead of `yyyy` in a pattern, producing a wrong year for a few days each January.
- Calling `LocalDate.now()` with no argument in domain logic, making the code untestable and host-dependent.
- Assuming a day has 86,400 seconds. Leap seconds are smeared by most infrastructure, but DST alone breaks the assumption.
- Comparing timestamps with `equals` across types. `Instant` and `OffsetDateTime` never compare equal; use `isEqual` or compare instants.
- Persisting to a database column that silently converts. MySQL `TIMESTAMP` normalizes to UTC using the session zone; `DATETIME` does not. Know which you have.
- Relying on a JDK's bundled tzdb in a long-lived container image. A container built two years ago has two-year-old political data.

## Production engineering notes

Store instants in UTC and convert at the edges. The database column should be `TIMESTAMP WITH TIME ZONE` or an explicit UTC-normalized type; the application should convert to the user's zone only at rendering time. This single rule removes most cross-region reporting defects.

Keep the customer's `ZoneId` as first-class data when future scheduling is involved. A subscription row that carries `next_renewal_local` and `zone_id` survives a tzdb update correctly; one that carries only `next_renewal_utc` does not.

Update tzdb deliberately. The JDK ships it, so a base-image bump changes time behavior. Treat that as a production change with the same care as a dependency upgrade, and know that `TZUpdater` exists for patching a JDK in place when a full upgrade is not available.

Inject `Clock` in every service that reads the current time. This is the difference between a test that asserts renewal behavior across a DST boundary in milliseconds and a test that cannot be written at all.

Log in ISO-8601 with an explicit offset. `2026-03-08T08:30:00Z` is greppable, sortable, and unambiguous; `Mar 8 03:30:00` is none of those and has lost the information needed to reconstruct the event ordering.

## Interview questions and model answers

**What is the difference between `Instant` and `LocalDateTime`?**

`Instant` is a point on the timeline in UTC - an unambiguous moment every observer agrees on. `LocalDateTime` is a calendar reading with no zone, so it does not identify a moment. Converting between them requires a zone, and choosing the wrong one is the most common time defect in a distributed system.

**When would you use `ZonedDateTime` instead of `Instant`?**

For future events tied to a place. "9 a.m. in Chicago next March" must remain 9 a.m. even if the offset rules change before then, so the zone must be retained rather than collapsed to an instant at write time. Past events go the other way: they already happened at one moment, so `Instant` is correct.

**What is the difference between `Duration` and `Period`?**

`Duration` is machine time - seconds and nanoseconds - and is unaffected by calendars. `Period` is calendar time - years, months, days - and deliberately varies in real length. Across a spring-forward boundary, `Period.ofDays(1)` advances 23 hours while `Duration.ofDays(1)` advances 24.

**What happens when you construct a `ZonedDateTime` for a time that does not exist?**

It does not throw. The gap resolution rule shifts the result forward by the length of the gap, so 02:30 on a spring-forward morning becomes 03:30. If you need to detect this rather than absorb it, call `ZoneRules.getValidOffsets` and check for an empty list.

**Why is `java.time` preferred over `Date` and `Calendar`?**

The types are immutable and thread-safe, the names state whether a value has a zone, months are one-based, and the API separates machine time from calendar time. `SimpleDateFormat`'s thread-unsafety alone caused a large class of production bugs that `DateTimeFormatter` cannot reproduce.

**How do you make time-dependent code testable?**

Inject a `Clock` and read the current time through it. `Clock.fixed` then makes DST boundaries, month-end clamping, and expiry edges ordinary unit tests rather than untestable behavior.

## Exercises

1. Write a method that returns every local time between 01:00 and 03:00 on a fall-back date in a zone of your choice, and mark which readings are ambiguous.
2. Take an existing service that calls `Instant.now()` directly and refactor it to accept a `Clock`; add a test that asserts behavior at a month boundary.
3. Demonstrate the `yyyy` versus `YYYY` discrepancy by formatting 29 December through 2 January.
4. Model a monthly subscription anchored on the 31st and produce twelve renewal dates. Explain each clamped result.
5. Store the same appointment as an `Instant` and as a `ZonedDateTime`, then simulate a zone-rule change and show which representation survives.
6. Benchmark `DateTimeFormatter.ofPattern` inside a loop against a hoisted `static final` formatter and report the difference.

## Chapter summary

`java.time` separates machine time from calendar time, and nearly every date defect comes from confusing the two. `Instant` is a moment; `LocalDate` and `LocalDateTime` are calendar readings without a zone; `ZonedDateTime` binds a local reading to a region's rule history and therefore to a real moment. `Duration` measures elapsed seconds while `Period` measures calendar fields, and they diverge exactly where daylight-saving boundaries make the divergence matter. Every type is immutable and thread-safe, so formatters can be shared and values can cross threads freely. Store past events as UTC instants, retain the zone for future appointments, pin locales on formatters, treat tzdb as production data, and inject `Clock` so that the hard cases become testable.

## Revision checklist

- [ ] I can state which `java.time` type models an event, a birthday, an appointment, and a timeout.
- [ ] I can explain why `LocalDateTime` does not identify a moment.
- [ ] I know the difference between `ZoneId` and `ZoneOffset` and when each is wrong.
- [ ] I can predict the result of `Period.ofDays(1)` and `Duration.ofDays(1)` across a DST boundary.
- [ ] I can detect a gap and an overlap using `ZoneRules.getValidOffsets`.
- [ ] I know why month arithmetic does not round-trip.
- [ ] I pin the locale on every human-facing formatter and hoist formatters to constants.
- [ ] I inject `Clock` rather than calling `Instant.now()` in domain logic.
- [ ] I can explain how a stale tzdb produces correct code with wrong answers.
