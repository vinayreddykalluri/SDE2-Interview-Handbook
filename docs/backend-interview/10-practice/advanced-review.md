# Advanced Review: Interview Simulation

## Advanced practice model

| Practice mode | Purpose |
| --- | --- |
| Blocked drill | Isolate one weak skill |
| Interleaved drill | Select the model without category hints |
| Timed mock | Execute structure under pressure |
| Constraint mutation | Test adaptation instead of memorization |
| Adversarial follow-up | Expose shallow assumptions |
| Teach-back | Detect gaps hidden by recognition |
| Error replay | Retry the missed signal with a new prompt |
| Full loop | Measure consistency across all rounds |

## Four-week rotation

| Week | Coding | Design | Production | Behavioral |
| --- | --- | --- | --- | --- |
| 1 | Pattern selection | LLD | Debugging | Ownership |
| 2 | Correctness proof | HLD | Data failure | Conflict |
| 3 | Performance | API/data | Reliability | Incident |
| 4 | Mixed mock | HLD + LLD | Security/cloud | Full loop |

## Pressure injections

- Halve the remaining time and request the minimum viable answer.
- Change one consistency or latency requirement.
- Make a dependency slow rather than unavailable.
- Add a hot tenant, duplicate message, or partial migration.
- Request cost reduction without violating the SLO.
- Challenge the preferred technology and request an alternative.

## Retrieval prompts

1. Which dimension repeatedly scores below 2?
2. Does the first five minutes create enough structure?
3. Which answers name technology without mechanism?
4. Which failure mode is repeatedly omitted?
5. Does time pressure degrade correctness?
6. Which green topic has not been tested for 30 days?
7. Can the candidate recover after a wrong assumption?
8. Does feedback produce a drill and retry date?

## Exit criteria

Complete two full loops with no dimension below 2, no repeated critical miss, and a score of 3 in coding, design, production reasoning, and ownership.
