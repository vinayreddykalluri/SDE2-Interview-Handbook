# Behavioral Interview Drills: Worked Solutions

These solutions use the sample story bank from the chapters. Your artifacts should use your own stories; what transfers is the shape and the level of detail.

## Foundation: the bank

### D1. Audit a story bank

The companion's coverage table shows four signals resting on a single story: **conflict** (S3), **mentoring** (S6), **customer focus** (S7), and **prioritization** (S7).

The most at-risk question type is **conflict**, because it is among the most frequently asked and a loop may ask it twice - once directly and once as "tell me about a time you disagreed with your manager". With one story, the second question has no fresh answer.

Gap-filling ideas for a backend engineer:

- **Conflict:** a disagreement with a product manager about shipping a feature without an alert or dashboard, resolved by agreeing a minimum observability bar.
- **Mentoring:** reviewing an intern's first design document and coaching them through the review meeting.
- **Customer focus:** pushing back on an API change after reading support tickets that showed how merchants actually used the field.
- **Prioritization:** choosing to fix a flaky test suite before starting a planned feature, and the argument you made for it.

### D2. Plan a loop

```text
Customer Obsession                  -> S7  (scope cut kept what the regulator and users needed)
Ownership                           -> S4  (the retry bug, owned end to end)
Dive Deep                           -> S2  (payment outage investigation)
Have Backbone; Disagree and Commit  -> S3  (caching disagreement)
Deliver Results                     -> S1  (migration, two weeks early)
Earn Trust                          -> S5  (three-team library adoption)
```

No story is used twice. The weakest assignments are **Ownership via S4**, because a failure story is an indirect ownership story and S1 - the strongest ownership story - is already spent on Deliver Results; and **Customer Obsession via S7**, the bank's only customer story. Both point back to the gaps in D1.

### D3. Upgrade a weak answer

What is missing: the candidate's own role ("our team decided"), any decision or alternative, any number ("a lot of incidents", "a lot better"), any difficulty, and any learning. There is nothing to score.

Five questions that recover the evidence:

1. How many incidents, over what period, and what did they have in common?
2. What was your specific role - did you propose the refactor, design it, or implement part of it?
3. What alternatives did you consider, and why did you reject them?
4. What went wrong or was hard during the refactor, and what did you do about it?
5. What was the incident rate afterwards, and how did you measure it?

## Structure and delivery

### D4. Rebalance an answer

```text
current:  situation 120 (42.9%)  task 40 (14.3%)  action 90 (32.1%)  result 30 (10.7%)
total     280 words  ->  112 s at 150 words per minute
```

Setup is 57% of the answer and action only 32% - the answer spends almost two thirds of its time before the candidate does anything. At the same 280 words, a budget that meets the targets (action at least 55%, situation and task at most 25%):

```text
target:   situation 40   task 25   action 160   result 55      total 280
shares    14.3%          8.9%      57.1%        19.6%
```

The 80 words cut from the situation become decisions, alternatives, and numbers in the action section.

### D5. Fix "we"

> I read the logs from the three failed payments and traced all of them to the retry logic: we retried on timeout without knowing whether the first attempt had succeeded. I proposed idempotency keys on every provider call and wrote the change; a teammate reviewed it and built the test harness for the timeout case. We rolled it out to one region first and then the rest over two weeks.

"I" marks the candidate's decisions and work; "we" marks the shared rollout; the teammate's contribution is credited precisely.

## Signal-specific stories

### D6. Conflict you lost

```text
stakes      whether to split a monolith's reporting module into its own service this quarter
their view  the lead wanted it now: reporting queries were slowing the main database
your view   add a read replica first; the split would take a quarter and the replica a week
conduct     raised it one to one with a cost comparison, then in the design review
resolution  the lead chose the split, because reporting would grow and a replica only deferred it
aftermath   I committed: I took the hardest part, the data-sync design, and it shipped on time;
            a year later reporting load had tripled, and the lead had been right
```

The last line is what makes it strong: the candidate lost, committed fully, and says plainly that the other view was correct.

### D7. A real failure

Use the five-part shape from chapter 6. The check is the fifth part: it must name a *later, specific* event where the lesson changed behaviour - "six months later, reviewing a payments retry change, I asked what happens if the first attempt succeeded and found the same bug". A general statement such as "since then I always test edge cases" is not evidence.

### D8. Mentoring measured by the mentee

End with the mentee, for example: "Four weeks later she was taking on-call shifts alone and escalating about one page in ten. Six months later she onboarded the next new hire using the same shadow-then-reverse approach."

## Company formats

### D9. Cover the newest principles

- **Strive to be Earth's Best Employer:** reducing on-call load for your team - for example, fixing the top five noisy alerts so that out-of-hours pages dropped by half - or making code review more inclusive for new or remote teammates, with a measured effect.
- **Success and Scale Bring Broad Responsibility:** considering the wider effect of a system - adding rate limits and abuse controls to a public API before launch, or making a data pipeline delete personal data on request - and the trade-off it cost.

## Challenge

### D10. Full mock behavioral round

Score each answer against chapter 3's targets: one and a half to three minutes; action at least 55%; "I" dominant in the action section; every probe answered with a specific detail. Most first mocks show the same two problems: setup that runs too long, and results without numbers. Rework the two stories with the lowest scores first, and repeat the mock with a different interviewer.
