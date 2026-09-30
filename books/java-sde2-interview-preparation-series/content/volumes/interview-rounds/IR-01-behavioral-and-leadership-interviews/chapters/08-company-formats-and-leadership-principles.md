# 8. Company Formats and Leadership Principles

## Learning objectives

By the end of this chapter, you should be able to:

- map your story bank onto a company's published values, using Amazon's Leadership Principles as the worked example;
- find the principles your bank does not yet cover;
- adapt the same stories to rounds that are labelled differently at different companies; and
- use your recruiter's preparation material as the specification.

## Why this matters at SDE-2

The signals in this book are universal, but companies name and weight them differently, and some structure whole rounds around their own values. A candidate who walks into an Amazon loop without having mapped their stories to the Leadership Principles, or into any loop without having read the recruiter's preparation guide, gives away evidence they already have.

## First-principles model

Every company's behavioral rubric is **a relabelling and reweighting of roughly the same signals.** Preparation for a specific company is therefore not a new story bank; it is a mapping from your existing stories onto the company's names, followed by a check for gaps.

> **Specification boundary:** interview formats change, and companies describe them in their own preparation material. Treat that material as the specification, and treat anything in this chapter - other than Amazon's published list of principles - as a general pattern to verify, not a guarantee of a particular company's current format.

## Core terminology

- **Leadership Principles (LPs):** Amazon's published list of sixteen principles used across its hiring process.
- **Values interview:** a round explicitly scored against a company's stated values.
- **Hiring-manager round:** an interview with the prospective manager, often mixing behavioral and technical discussion.
- **Recruiter preparation guide:** the material a recruiter sends describing the loop and what each round evaluates.

## Detailed mechanics

### Amazon's Leadership Principles

Amazon publishes sixteen Leadership Principles and uses them throughout its interviews; behavioral questions in an Amazon loop are typically each aimed at one or two of them. The list, as published:

```text
Customer Obsession                    Think Big
Ownership                             Bias for Action
Invent and Simplify                   Frugality
Are Right, A Lot                      Earn Trust
Learn and Be Curious                  Dive Deep
Hire and Develop the Best             Have Backbone; Disagree and Commit
Insist on the Highest Standards       Deliver Results
Strive to be Earth's Best Employer    Success and Scale Bring Broad Responsibility
```

The companion maps each of the twelve signals from chapter 1 onto the principles it can evidence - a preparation aid, not an official Amazon rubric - and counts how many stories in the sample bank reach each one:

```text
Customer Obsession                           1
Ownership                                    3
Invent and Simplify                          2
Are Right, A Lot                             2
Learn and Be Curious                         3
Hire and Develop the Best                    1
Insist on the Highest Standards              3
Think Big                                    2
Bias for Action                              5
Frugality                                    2
Earn Trust                                   4
Dive Deep                                    4
Have Backbone; Disagree and Commit           1
Deliver Results                              4
Strive to be Earth's Best Employer           0
Success and Scale Bring Broad Responsibility 0
no story yet for: [Strive to be Earth's Best Employer, Success and Scale Bring Broad Responsibility]
```

Two findings follow. First, the two newest principles - added in 2021 and about people and broader impact - have no story at all in a bank built around the generic signals. Stories that fit them: making on-call sustainable for a team, improving inclusion in reviews or hiring, or considering the wider effect of a system on users and the business - accessibility, privacy, or reliability for vulnerable users. Second, Customer Obsession, Hire and Develop the Best, and Have Backbone rest on a single story each, which is the same gap as the single-story conflict signal in chapter 2, seen through a different lens.

### Using the mapping

For an Amazon loop:

1. Map every story in your bank onto its principles, as above.
2. Make sure every principle has at least one story and the most common ones - Customer Obsession, Ownership, Dive Deep, Deliver Results, Have Backbone - have two.
3. Use the principle's language naturally in your answer where it is honest: "I wanted to dive deep here because the averages hid the problem." Do not recite principle names; interviewers notice.

### Other common formats

Formats vary, but three patterns recur across large companies:

- **A dedicated values or behavioral round**, scored on a published or semi-published set of values. Map your bank onto the named values exactly as with Amazon's principles.
- **A hiring-manager round** mixing behavioral questions with a discussion of a past project in technical depth. Your anchor story (chapter 2) is the natural centrepiece; expect deep technical probes on it.
- **Behavioral questions embedded in technical rounds** - "tell me about a system you designed" at the start of a design round. Keep these short, so that the technical part has its time.

For any company, the recruiter's preparation guide is the best available description of the current loop. Read it, note the values or signals it names, and map your bank onto them.

### The same story, three labels

S3, the caching disagreement, is:

- **Have Backbone; Disagree and Commit** at Amazon;
- **conflict resolution** or **collaboration** in a generic behavioral rubric;
- a **technical judgement** example in a hiring-manager conversation.

The story does not change; the emphasis does. For Have Backbone, dwell on raising the concern with evidence to a more senior person. For collaboration, dwell on the combined design. For technical judgement, dwell on the price-change data.

## Failure modes and common mistakes

- **Not reading the recruiter's guide.** It is the closest thing to a specification.
- **A new story bank per company.** Map the existing one instead.
- **Reciting principle names.** Use the ideas, not the labels.
- **Ignoring the newer principles** in an Amazon loop.
- **Long behavioral answers inside technical rounds.** Keep them short.

## Interview questions and model answers

**Q: Tell me about a time you insisted on high standards.** *(Amazon: Insist on the Highest Standards)*
A: S1's backfill: stopping a migration on a 2% data mismatch rather than continuing to the deadline, fixing the root cause, and re-running the comparison.

**Q: Tell me about a time you went deep into data to solve a problem.** *(Dive Deep)*
A: S3: measuring price-change bursts during sales to show where the proposed cache would serve stale prices.

**Q: Tell me about a time you made things better for the people on your team.** *(Strive to be Earth's Best Employer)*
A: The gap the companion found. A good fit is S6 reframed: redesigning the on-call onboarding so that new engineers were not overwhelmed, with its effect on escalations and on the next hire.

## Exercises

1. Map your own bank onto Amazon's sixteen principles. Which have no story, and which have only one?
2. Take the values list from any company you are interviewing with and map your bank onto it.
3. Tell S3, or your own conflict story, three ways: emphasising backbone, collaboration, and technical judgement.

## Chapter summary

Company rubrics relabel and reweight the same core signals, so prepare one story bank and map it onto each company's values. Amazon's sixteen Leadership Principles are the clearest published example; the sample bank covered fourteen of them and missed the two newest. Treat the recruiter's preparation guide as the specification, and adjust emphasis - not stories - to the label a question is scored against.

## Revision checklist

- [ ] I can list Amazon's sixteen Leadership Principles.
- [ ] My bank maps onto every principle, with two stories for the most common ones.
- [ ] I have read the preparation guide for each company I am interviewing with.
- [ ] I can tell one story with three different emphases.
