# 7. Mentoring and Leadership Without Authority

## Learning objectives

By the end of this chapter, you should be able to:

- show leadership as an individual contributor, without a management title;
- tell a mentoring story whose outcome is the other person's growth, not your help;
- describe raising the bar for a team through reviews, documents, or process;
- describe leading during an incident; and
- calibrate these stories to SDE-2 rather than to a staff or management role.

## Why this matters at SDE-2

At SDE-1, leadership is a bonus. At SDE-2 it is expected in a specific form: an engineer who makes the people and systems around them better without being told to. Interviewers look for mentoring, technical leadership on a project, improvements to how the team works, and composure during incidents. Candidates with no management experience often assume they have nothing to say. Almost every SDE-2 candidate has these stories; they have just not framed them.

## First-principles model

Leadership without authority is **changing outcomes through other people when you cannot instruct them.** The evidence is always the same shape: a situation where the team was worse off than it could be, what you did to change how others worked, and the lasting effect.

For mentoring specifically, the score is on the mentee's growth. "I answered their questions" is help; "they now answer other people's questions" is mentoring.

## Core terminology

- **Technical lead (informal):** the engineer who drives a project's design and sequencing without managing its people.
- **Raising the bar:** improving a team's standards - reviews, testing, documentation, on-call.
- **Force multiplier:** work that makes several other people more effective.
- **Incident commander:** the person coordinating an incident response, distinct from those fixing it.

## Detailed mechanics

### Mentoring

The sample bank's S6: a new engineer's first on-call rotation.

- **Starting point:** they were anxious, had not seen the systems under load, and the previous new hire had escalated nearly every page.
- **What you did:** shadowed them for the first two shifts, then reversed roles and let them lead with you shadowing; walked through three past incidents using the runbooks; agreed a rule that they would spend fifteen minutes on a page before escalating.
- **Their growth:** by the fourth week they handled pages alone and escalated one in ten; six months later they were mentoring the next hire the same way.
- **What you learned:** you had been solving problems for mentees too quickly; now you ask what they would try first.

The last two bullets make it a mentoring story rather than a helping story.

### Technical leadership on a project

SDE-2 candidates often led a project's technical direction without a title. Evidence to include:

- **you set the direction**: wrote the design, sequenced the work, identified the risks;
- **you enabled others**: split work so that others could own pieces, reviewed their designs, unblocked them;
- **you kept it on track**: noticed slippage early and changed the plan, not just the dates.

The database migration (S1) is also a technical-leadership story if told from this angle: the four-step plan let two teammates own the backfill and the read switch-over in parallel.

### Raising the bar

Improvements to how a team works are strong SDE-2 evidence because they outlast you:

- a design-document template that made reviews faster (S8's outcome);
- a checklist for risky changes, such as the retry-logic question from S4;
- a runbook standard that let new on-call engineers handle incidents alone;
- a code-review norm - response within one working day, or size limits - that cut cycle time.

State the before and after with numbers where you can: review time, incident count, on-call escalations.

### Leading during an incident

Incident leadership is scored on composure and coordination, not heroics:

- **you separated coordination from fixing**: someone led communication while others investigated;
- **you prioritised mitigation over diagnosis**: rolled back or failed over first, found the root cause afterwards;
- **you communicated on a schedule**: status every fifteen or thirty minutes, even without news;
- **you owned the follow-up**: a blameless post-mortem with corrective actions that were actually completed.

S2, the payment outage, belongs here: the candidate recognised that three engineers were all debugging and nobody was updating support, took the coordinator role, asked one engineer to prepare a rollback while the others kept investigating, and rolled back at the twenty-minute mark when the cause was still unclear.

### Calibrating to SDE-2

Do not over-reach. An SDE-2 candidate who describes managing people, setting organisational strategy, or hiring decisions may be asked why they are interviewing for SDE-2. Aim for team-level leadership: your project, your team's practices, your mentees, your incidents.

## Failure modes and common mistakes

- **"I don't have leadership experience."** You almost certainly do; frame it.
- **Helping presented as mentoring.** Show the mentee's growth.
- **Heroic incident stories.** Coordination and mitigation, not all-night debugging.
- **Improvements without a measured effect.**
- **Over-reaching** to manager or staff scope.

## Interview questions and model answers

**Q: Tell me about a time you mentored someone.**
A: S6: the new engineer's first on-call. Their starting point, the shadow-then-reverse approach, the escalation rule, and the result - handling pages alone within four weeks and mentoring the next hire six months later.

**Q: Tell me about a time you led without formal authority.**
A: The migration as a technical-leadership story: I wrote the design and the four-step plan, split the work so that two teammates could own the backfill and the read switch-over, and reviewed their designs.

**Q: Tell me about a time you improved how your team works.**
A: The one-page design-document format from S8: review turnaround dropped from about a week to two days, and the team adopted it as the template.

**Q: Walk me through how you handled a production incident.**
A: S2: took coordination when nobody was communicating, rolled back at twenty minutes before finding the cause, sent updates every fifteen minutes, and completed the post-mortem actions.

## Exercises

1. Write a mentoring story in which the final sentence is about the mentee, not you.
2. List three things you changed about how your team works. Put a before-and-after number on one.
3. Write your incident story with one sentence each for coordination, mitigation, communication, and follow-up.

## Chapter summary

Leadership at SDE-2 means improving outcomes through other people without authority: mentoring measured by the mentee's growth, technical leadership that enabled others, lasting improvements to how the team works, and calm coordination during incidents. Frame the stories you already have, measure their effect, and stay at team scope.

## Revision checklist

- [ ] My mentoring story ends with the mentee's growth.
- [ ] I have a technical-leadership story that shows how I enabled others.
- [ ] I have a team-improvement story with a measured effect.
- [ ] My incident story emphasises coordination and mitigation.
