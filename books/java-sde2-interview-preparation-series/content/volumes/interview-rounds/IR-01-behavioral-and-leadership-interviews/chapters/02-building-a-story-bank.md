# 2. Building a Story Bank

## Learning objectives

By the end of this chapter, you should be able to:

- mine your own history for candidate stories systematically instead of from memory on the day;
- choose eight to ten stories that together cover every signal at least twice;
- record each story in a compact, reusable format;
- plan which story answers which question in a loop without over-using one; and
- find and fill the gaps before an interviewer does.

## Why this matters at SDE-2

Candidates who prepare behavioral rounds by rehearsing answers to specific questions run out of material: there are hundreds of phrasings and only a few real experiences. Candidates who prepare a *story bank* - a small set of rich stories tagged with the signals each can evidence - can answer almost any phrasing by choosing a story and emphasising the right part. The bank also exposes gaps in advance, which is the only time they can be fixed.

## First-principles model

A story bank is **a many-to-many mapping from stories to signals**. One rich story evidences several signals; each signal should be covered by at least two stories, because a loop may ask about the same signal twice and one story cannot be used for every round.

Treat the bank as data. The companion model does exactly that: it tags stories with signals, prints the coverage, and flags any signal with fewer than two stories.

## Core terminology

- **Story bank:** the set of prepared stories, each tagged with signals.
- **Coverage:** the number of stories that evidence a given signal.
- **Anchor story:** a large, rich story that covers four or more signals.
- **Loop plan:** the assignment of stories to the questions you expect across a day of interviews.
- **Gap:** a signal with no story, or with only one.

## Detailed mechanics

### Mining for stories

Do not start from the questions. Start from your history, because that is where the evidence is:

1. **Walk your resume backwards**, project by project, for the last three to five years.
2. **For each project, list the moments**: a decision you made, a disagreement, something that broke, something you changed your mind about, someone you helped.
3. **Check your written trail**: design documents, incident post-mortems, performance reviews, and long pull-request threads. They contain specifics - numbers, dates, names of alternatives - that memory has blurred.
4. **Keep everything at first**, then choose.

A useful target is twenty candidate moments, from which you will choose eight to ten stories.

### Choosing the bank

Prefer stories that are:

- **Recent** - within about three years, so that the scope reflects your current level.
- **Yours** - you made the key decisions, even if a team delivered the result.
- **Measurable** - there is a number: latency, incidents, time saved, revenue, adoption.
- **Rich** - an anchor story covers several signals and survives deep probing.
- **Varied** - different projects, teams, and kinds of problem.

### The sample bank and its gaps

The companion's sample bank is a composite for a backend engineer with about five years of experience: eight stories, including a database migration (S1), leading an outage response (S2), a design disagreement (S3), a self-inflicted bug (S4), a cross-team library adoption (S5), mentoring through on-call (S6), a scope cut for a regulatory deadline (S7), and hard design-review feedback (S8). Its coverage:

```text
OWNERSHIP       3 stories [S1, S2, S4]
AMBIGUITY       2 stories [S1, S5]
TECHNICAL_DEPTH 3 stories [S1, S2, S3]
DELIVERY        2 stories [S1, S7]
CONFLICT        1 story   [S3]
INFLUENCE       2 stories [S3, S5]
FAILURE         2 stories [S4, S8]
FEEDBACK        2 stories [S6, S8]
MENTORING       1 story   [S6]
CUSTOMER_FOCUS  1 story   [S7]
PRIORITIZATION  1 story   [S7]
INCIDENT        2 stories [S2, S4]
signals with fewer than two stories: [CONFLICT, MENTORING, CUSTOMER_FOCUS, PRIORITIZATION]
```

Every signal is covered, which looks fine at a glance. The model says otherwise: four signals rest on a single story. When I first wrote the expected gaps for this chapter, I missed conflict - the bank has an obvious disagreement story, so it felt covered. But conflict is one of the most frequently asked signals, and with only one story, a second conflict question in the same loop has no fresh answer. The fix is to add a story, not to stretch S3 twice.

### The story card

Record each story on one card, in notes rather than prose:

```text
ID / title:     S1 - Migrated order service off a shared database
When / scope:   2024, 2 services, 2 teams, 1 quarter
Situation:      shared DB; schema changes needed both teams; 2 checkout incidents / quarter
My decisions:   mapped 14 cross-boundary queries; replaced 3 joins with an API;
                4-step reversible migration; stopped the backfill on a 2% mismatch
Alternatives:   shared read replica (rejected: coupling stays); big-bang cutover (rejected: risk)
Conflict:       checkout lead worried about latency -> prototype -> +4 ms p99, agreed
Result:         2 weeks early, no downtime, 0 DB incidents in 2 quarters
Learning:       start data comparison earlier (found bug late, cost a week)
Signals:        ownership, ambiguity, technical depth, delivery (+ influence via the prototype)
```

A card is for recall under pressure. The numbers and the rejected alternatives are the parts people forget and interviewers probe.

### Planning a loop

A typical SDE-2 loop asks five to eight behavioral questions across several interviewers, who often compare notes afterwards. Telling the same story to every interviewer wastes the loop's evidence and looks thin. Plan it:

```text
Q1 OWNERSHIP       -> S2
Q2 CONFLICT        -> S3
Q3 FAILURE         -> S8
Q4 INCIDENT        -> S4
Q5 INFLUENCE       -> S5
Q6 OWNERSHIP       -> S1
```

The companion's planner assigns each question the least-used story that covers it, with a limit of one use per story. This six-question loop is covered with no reuse. A different loop is not:

```text
mentoring, feedback, mentoring again -> [S6, S8, GAP]
```

A second mentoring question finds no unused story - the same gap the coverage table predicted. In practice, you may reuse a rich story for a different signal with a different emphasis, but you should choose to, not be forced to.

### Filling gaps honestly

A gap does not mean you lack the experience. It usually means you have not noticed which of your experiences counts. For mentoring: onboarding a new hire, reviewing an intern's design, pairing through someone's first on-call. For customer focus: a time you pushed back on a requirement because of what users actually did. If there is genuinely nothing, say so in the round and describe what you are doing about it. Do not invent a story; it will not survive probing.

## Failure modes and common mistakes

- **Preparing answers to questions instead of stories.** You will face phrasings you did not prepare.
- **Too few stories.** Five stories cannot cover a six-question loop without repetition.
- **One story per signal.** Looks covered; fails on the second question.
- **Stories without numbers.** The first probe asks "how did you know it worked?"
- **Old stories.** A strong story from six years ago shows the scope you had six years ago.

## Interview questions and model answers

**Q: Tell me about a time you had to work with ambiguous requirements.**
A strong answer comes from a card - S1 above: the goal was "stop the incidents", not a specification; the candidate framed it as a data-ownership problem, mapped the queries, and chose an approach with named alternatives.

**Q: Can you give me another example?**
The test of a bank. Have a second story for every signal: here, S5, getting three teams to adopt a shared library when nobody had agreed what the problem was.

## Exercises

1. Mine your last three years and list twenty candidate moments.
2. Choose eight to ten stories and fill in a card for each.
3. Tag each card with signals and build the coverage table. List every signal with fewer than two stories, and find a second story for each.
4. Plan a six-question loop that uses no story twice.

## Chapter summary

Prepare stories, not answers. Mine your history systematically, choose eight to ten recent, measurable stories that are genuinely yours, and record each on a card with its decisions, rejected alternatives, numbers, and learning. Treat the bank as data: every signal needs at least two stories, and a planned loop should not force reuse. The sample bank looked complete and still had four single-story signals, including conflict.

## Revision checklist

- [ ] I have twenty candidate moments and eight to ten chosen stories.
- [ ] Every story has a card with numbers and rejected alternatives.
- [ ] Every signal has at least two stories.
- [ ] I have planned a loop with no forced reuse.
