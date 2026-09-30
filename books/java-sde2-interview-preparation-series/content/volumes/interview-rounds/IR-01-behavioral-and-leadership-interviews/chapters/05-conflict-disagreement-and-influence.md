# 5. Conflict, Disagreement, and Influence

## Learning objectives

By the end of this chapter, you should be able to:

- choose a conflict story with real stakes and a relationship that survived;
- show how you disagreed: with evidence, directly, and with respect;
- explain what you did when the decision went against you;
- show influence over people you did not manage; and
- avoid the conflict stories that quietly disqualify candidates.

## Why this matters at SDE-2

"Tell me about a disagreement with a colleague" is one of the most common behavioral questions at every level, and one of the easiest to fail. At SDE-2 the bar rises: interviewers expect disagreements about technical direction or priorities, often with more senior people or other teams, resolved through evidence rather than escalation. They also expect you to be able to lose gracefully - disagree, then commit fully to the decision.

## First-principles model

A conflict story is scored on three things:

1. **The disagreement was about substance** - a design, a priority, a risk - not personalities.
2. **Your conduct was effective and respectful**: you understood the other view, brought evidence, and spoke directly to the person rather than around them.
3. **The outcome and the relationship**: whether you won or lost, the decision improved or was committed to, and you still work well with the person.

Influence is the same skill without the conflict: changing a decision that you did not own.

## Core terminology

- **Disagree and commit:** argue your position fully, then fully support the decision once it is made.
- **Steelman:** state the other person's position in its strongest form.
- **Escalation:** involving someone with more authority to resolve a disagreement.
- **Influence without authority:** changing others' decisions through evidence, relationships, and framing.
- **Pre-wiring:** discussing a proposal with key people individually before a group decision.

## Detailed mechanics

### Choosing the story

Good conflict stories involve a real technical or priority disagreement with real stakes, with someone whose opinion mattered - a senior engineer, a tech lead, another team, a product manager. The sample bank's S3 is a disagreement with the tech lead about a caching design.

Avoid:

- **Interpersonal drama** - "a colleague who was difficult". It invites the question of your contribution to the difficulty.
- **Conflicts where the other person was simply wrong** and you were simply right. Real disagreements have merit on both sides; a story without that reads as low empathy.
- **Stories that end in escalation as the first move.** Escalation is sometimes correct, but as the second step, after a direct conversation.

### The shape of a good conflict story

```text
stakes      what decision, and why it mattered
their view  the other person's position, stated fairly (steelman)
your view   your position and the evidence behind it
conduct     how you raised it: directly, privately first, with data
resolution  what was decided and how
aftermath   the relationship, and what you learned
```

S3, told in that shape: the tech lead wanted a write-through cache for product data to cut read latency; the candidate worried about stale prices during flash sales. Their view was sound - read latency was the top complaint. The candidate measured how often prices changed during sales and found bursts of hundreds of updates per minute, then proposed a shorter TTL with explicit invalidation on price changes. They raised it one to one before the design review, not in it. The lead agreed to the invalidation but kept a longer TTL for non-price fields - a better design than either original. They went on to co-write the next design.

### When you lose

Some of the strongest conflict stories are ones where the decision went the other way. The evidence to include:

- you argued your case fully, with data;
- once it was decided, you committed: implemented the decision well, and did not relitigate it in every meeting;
- if your concern later proved right, you raised it with evidence and without "I told you so" - and if it proved wrong, you said so.

This is what "disagree and commit" means, and it is scored directly at Amazon under Have Backbone; Disagree and Commit, and indirectly almost everywhere else.

### Influence without authority

Influence stories - S5, the cross-team library - follow a recognisable pattern:

1. **Find the other side's incentive.** What do the other teams care about? Fewer incidents, less on-call load, less code to maintain.
2. **Lower the cost of saying yes.** A working library, a migration guide, an offer to pair on the first integration.
3. **Show evidence from a pilot.** One team's results persuade the next.
4. **Pre-wire.** Talk to each lead individually before asking for a group decision.

The candidate who says "I convinced them because my idea was better" has told a weaker story than the one who says "I found out what each team was worried about and made adoption cheaper than not adopting".

### With a manager

"Tell me about a time you disagreed with your manager" needs the same shape and extra care. Show that you raised it privately, with evidence, and accepted their authority to decide - and that it was about the work, not about them.

## Failure modes and common mistakes

- **A story with no real disagreement** - "we discussed and agreed".
- **Villain stories** in which the other person was unreasonable and you were right.
- **Escalating first.** Direct conversation comes first.
- **Winning at the cost of the relationship.** The aftermath is part of the score.
- **Never losing.** A candidate who always won every disagreement is not credible.
- **Relitigating after the decision.** The opposite of commit.

## Interview questions and model answers

**Q: Tell me about a time you disagreed with a technical decision.**
A: S3: the caching design. State the lead's view fairly, the data you gathered on price-change bursts, that you raised it one to one before the review, the combined design that resulted, and that you co-wrote the next design together.

**Q: Tell me about a time a decision went against you.**
A: A story where you argued with data, lost, committed, and implemented the decision well - and what happened next, including whether your concern turned out to be right.

**Q: How do you get another team to do something they don't own?**
A: Find their incentive, lower the cost of saying yes, show a pilot's results, and talk to each lead before the group decision. S5 is the example: a working library, a migration guide, and one team's results.

## Exercises

1. Write your main conflict story in the six-part shape above. State the other person's view in a way they would agree with.
2. Find a story where you lost a disagreement and committed. If you have none, look again - you almost certainly do.
3. For an influence story, write down the other team's incentive in one sentence.

## Chapter summary

Conflict stories are scored on substance, conduct, and aftermath. Choose a real disagreement about the work, state the other view fairly, show direct and evidence-based conduct, and describe a relationship that survived. Have a story where you lost and committed. Influence stories show that you understood the other side's incentives and made agreement cheap.

## Revision checklist

- [ ] I have a conflict story about substance, with a fair statement of the other view.
- [ ] I raised the disagreement directly before escalating.
- [ ] I have a "disagree and commit" story where I lost.
- [ ] My influence story names the other side's incentive and a pilot.
