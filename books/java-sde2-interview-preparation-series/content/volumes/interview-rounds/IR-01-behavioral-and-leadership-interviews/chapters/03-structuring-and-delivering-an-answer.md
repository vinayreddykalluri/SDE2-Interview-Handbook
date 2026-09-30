# 3. Structuring and Delivering an Answer

## Learning objectives

By the end of this chapter, you should be able to:

- structure an answer with STAR and allocate time so that most of it is about your actions;
- keep an answer between about one and a half and three minutes;
- use "I" for your decisions and "we" for shared outcomes, and check the balance;
- make results concrete, and close with what you learned;
- handle probes, clarifying questions, and the moment you realise you picked the wrong story.

## Why this matters at SDE-2

Two candidates with the same experience can score very differently on delivery alone. One spends ninety seconds on background and runs out of time before the decision that mattered. The other states the stakes in two sentences and spends a minute on what they decided and why. Delivery is not polish; it determines whether the evidence reaches the interviewer at all.

## First-principles model

An answer has one job: **make your decisions and their consequences unmistakable, fast.** Everything else - context, team, outcome - exists to make those decisions understandable.

STAR is the standard shape:

- **Situation:** the context, in two or three sentences, including the stakes.
- **Task:** what you specifically owned.
- **Action:** what you did, decided, and rejected - the bulk of the answer.
- **Result:** what happened, measured, and what you learned.

Many interviewers also listen for a closing **Learning** (sometimes written STARL): what you would do differently, or what you now do by default.

## Core terminology

- **STAR:** Situation, Task, Action, Result.
- **Setup share:** the fraction of the answer spent on situation and task.
- **Action share:** the fraction spent on your actions and decisions.
- **Probe:** an interviewer's follow-up question that tests a detail.
- **Headline:** a one-sentence summary said first, so the interviewer knows where the story is going.

## Detailed mechanics

### Allocating time

The companion measures a worked answer - the database migration from chapter 2 - section by section, at a speaking rate of 150 words per minute. That rate is an assumption: typical conversational speech is often quoted at around 130 to 160 words per minute, and nerves push it higher. Time yourself.

```text
Situation  30 words  12.0%   12 s
Task       22 words   8.8%    9 s
Action    154 words  61.6%   62 s
Result     44 words  17.6%   18 s
total     250 words          100 s (1.7 min)
```

The targets the model checks:

- **Action at least 55%** of the answer. The actions are the evidence.
- **Situation and task together at most 25%.** Context beyond what makes the decisions understandable is time taken from evidence.
- **One and a half to three minutes in total.** Shorter leaves no detail to score; longer loses the interviewer and leaves no time for probes, which is where much of the scoring happens.

### The worked answer

Here is the answer the model measured, as it would be spoken. Notice what each part does.

> **Situation.** Our checkout service and the order service shared one PostgreSQL database. Every schema change needed both teams, and a slow order query had caused two checkout incidents in one quarter.
>
> **Task.** I owned the plan to give the order service its own database without downtime, and I had one quarter to do it.
>
> **Action.** I started by mapping every query that crossed the boundary, and found fourteen, three of them joins that checkout depended on. I proposed replacing the joins with an order-summary API, and wrote a one-page design that listed the three options I had rejected and why. The checkout lead pushed back on latency, so I built a prototype and measured it: the API added four milliseconds at p99, which we agreed was acceptable. I then ran the migration in four steps, each reversible: dual writes behind a flag, a backfill with row-count and checksum comparison, reads switched one endpoint at a time, and finally removing the old tables. During the backfill I found that two percent of rows differed because of a timezone bug in an old job, so I stopped, fixed the job, and re-ran the comparison before continuing. I wrote a runbook for each step so that anyone on call could roll back.
>
> **Result.** We finished two weeks early with no customer-facing downtime. Checkout had no database-related incidents in the next two quarters, and schema changes stopped needing cross-team review. I would now start the data comparison earlier, because finding the timezone bug late cost us a week.

What makes it strong: the stakes are in the first two sentences; the action section contains decisions (replace joins with an API, four reversible steps, stop the backfill), alternatives (three rejected options), a disagreement resolved with evidence (the prototype), and a number for each claim; the result is measured and followed by an honest learning.

### "I" versus "we"

Interviewers score what *you* did. The companion counts first-person pronouns in the worked answer:

```text
whole answer: 11 singular, 4 plural
action section: 8 singular, 1 plural
```

The action section is overwhelmingly "I", because it describes the candidate's decisions. "We" appears where it is honest - the agreement with the checkout lead, and the team's shared result. That is the balance to aim for: "I" for what you decided and did, "we" for outcomes the team shares. A story told entirely in "we" cannot be credited to you; a story told entirely in "I" about a team effort sounds as though you are taking credit for others' work.

### Start with a headline

Before the situation, say one sentence that tells the interviewer where you are going: "This is about moving a service off a shared database without downtime, where I had to convince another team." It lets the interviewer listen for the right things and redirect you immediately if the story does not fit the question.

### Results with numbers - and without them

Prefer a number: latency, error rate, incidents, time, cost, adoption. If there is no number, use the next best evidence: "the other two teams adopted the library within a month", "the on-call runbook became the team template". If the result was bad, say so; a failed project with a clear learning can be a strong story.

### Handling probes

- **Answer the question asked**, briefly, then stop. Probes are short questions wanting short answers.
- **Say "I don't remember the exact figure, but it was roughly..."** rather than inventing a precise number. Precision you cannot defend damages everything else you said.
- **Credit others precisely.** "The design was mine; the backfill tooling was built by a teammate" is a strong answer, not a weak one.

### Picking the wrong story

If a minute in you realise the story does not fit the question, say so: "I realise this is more about delivery than conflict - would you like me to finish it, or switch to a better example?" Interviewers respect the self-awareness, and it saves the round.

## Failure modes and common mistakes

- **Long setup.** Ninety seconds of context and thirty of action.
- **All "we".** Nothing can be credited to you.
- **No alternatives.** There is no evidence of judgement without the paths not taken.
- **No number in the result**, or an invented one.
- **Memorised word-for-word.** Sounds recited and breaks at the first probe. Rehearse the card, not a script.
- **Over five minutes.** The interviewer loses the thread and has no time to probe.

## Interview questions and model answers

**Q: Tell me about a project you're proud of.**
A: A headline, then a two-minute STAR answer like the one above: stakes in two sentences, your decisions and rejected alternatives in the middle, a measured result, and what you would do differently.

**Q: What exactly was your part, versus the team's?**
A: Name the split precisely: "I wrote the design, built the prototype, and ran the migration steps; a teammate built the backfill tool, and the checkout team changed their callers to the new API."

**Q: How do you know it worked?**
A: The number, and how it was measured: "no database-related checkout incidents in the two quarters after, from our incident tracker, against two in the quarter before."

## Exercises

1. Write one story as a full STAR answer. Count the words in each section. Is action at least 55%?
2. Record yourself telling it. Time it, and count "I" and "we".
3. Rewrite the answer so that the stakes appear in the first two sentences.
4. Have someone ask you five probes and answer each in under twenty seconds.

## Chapter summary

Make your decisions unmistakable, fast. Open with a headline, spend no more than a quarter of the answer on setup and at least 55% on actions and decisions, and keep the whole answer to one and a half to three minutes. Use "I" for your decisions and "we" for shared outcomes, include the alternatives you rejected, measure the result, and close with a learning. Rehearse from the card, not a script, so that probes find real detail.

## Revision checklist

- [ ] I can state the STAR time allocation and the total length target.
- [ ] My main stories each open with a headline.
- [ ] My action sections name rejected alternatives.
- [ ] My results have numbers, or honest substitutes.
- [ ] I know how to recover when I pick the wrong story.
