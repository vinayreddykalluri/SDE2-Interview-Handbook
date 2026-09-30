# 6. Failure, Feedback, and Growth

## Learning objectives

By the end of this chapter, you should be able to:

- choose a failure that was real, yours, and consequential;
- tell it in a way that shows ownership, recovery, and a lasting change;
- describe receiving hard feedback and what you did differently afterwards;
- describe giving hard feedback kindly and effectively; and
- answer "what is your biggest weakness?" without a disguised strength.

## Why this matters at SDE-2

Failure and feedback questions test self-awareness, which interviewers treat as a predictor of growth. They are also where rehearsed candidates are most transparent: a "failure" that was really someone else's fault, or a weakness that is secretly a strength, tells the interviewer more than a real mistake would. At SDE-2, interviewers expect failures with real consequences - an incident, a missed commitment, a bad design decision - and a change in behaviour that lasted.

## First-principles model

A failure story is scored almost entirely on what happened **after** the failure:

1. **Ownership:** you say plainly what you did wrong, without shifting blame.
2. **Recovery:** what you did to limit the damage.
3. **Prevention:** what you changed so that it would not happen again - to the system, the process, or your own habits.
4. **Evidence of the change:** a later situation where you behaved differently.

The failure itself only needs to be real and to matter.

## Core terminology

- **Blameless post-mortem:** an incident review focused on systemic causes rather than individual fault.
- **Contributing factor:** a condition that made a failure possible or worse.
- **Corrective action:** a concrete change made to prevent recurrence.
- **Feedback:** specific, actionable information about behaviour and its effect.

## Detailed mechanics

### Choosing a failure

Good choices are real mistakes with consequences, where you were a primary cause:

- a change you shipped that caused an incident (S4 in the sample bank: a retry change that sent duplicate emails);
- a design decision that did not hold up and had to be reversed;
- an estimate or commitment you missed, and how you handled it;
- a problem you saw coming and did not raise early enough.

Avoid:

- failures that were someone else's fault in disguise;
- trivial failures ("I once forgot to update a README");
- catastrophic failures with no recovery or learning;
- failures from so long ago that the learning is not current.

### The shape of a failure story

S4, told well:

> **What happened.** I changed our email service's retry policy to handle a provider's intermittent timeouts. I retried on timeout without checking whether the provider had actually accepted the request. During a provider slowdown, about four thousand customers received the same order confirmation two or three times.
>
> **My part.** The mistake was mine: I treated a timeout as a failure, when it only meant the outcome was unknown. The reviewer did not catch it either, but I wrote the change and did not test the timeout path.
>
> **Recovery.** I rolled the change back within forty minutes, worked with support on a short apology email, and wrote the incident review.
>
> **Prevention.** I added an idempotency key to every provider call so that a retry could not duplicate a send, and a test that simulates a timeout after the provider accepts. I also proposed a checklist item for any change to retry logic: "what happens if the first attempt actually succeeded?"
>
> **Lasting change.** Six months later, reviewing a payments retry change, I asked exactly that question and found the same bug before it shipped. That idea later became the shared idempotency library.

The last paragraph is what distinguishes a strong answer: evidence that the learning changed later behaviour.

### Receiving feedback

"Tell me about a time you received critical feedback" is scored on your response, not the feedback:

- **The feedback, stated specifically and fairly.** S8: "My design documents were long and buried the decision; reviewers could not tell what I was asking them to approve."
- **Your first reaction, honestly.** It is fine to say it stung.
- **What you did.** Asked for examples, rewrote the next document with the decision and alternatives on the first page, and asked the same reviewer to check it.
- **The result.** Reviews got faster; the one-page format became the team template.

### Giving feedback

Giving hard feedback well is an SDE-2 signal, especially for mentoring and team health:

- give it **privately and promptly**, about **specific behaviour and its effect**, not character;
- **ask for their view** before concluding;
- **agree a next step**, and follow up.

"I told them their pull requests were too large" is weak. "I showed them two recent pull requests that had waited three days for review, explained that reviewers were skipping them because of size, and paired on splitting the next one; their review time dropped to under a day" is strong.

### "What is your biggest weakness?"

Choose a real, work-relevant weakness that is not disqualifying for the role, and show what you are doing about it, with evidence:

- "I used to go deep on a problem before checking whether others had already solved it. I now spend the first hour of any new problem searching our design docs and asking in the team channel; twice in the last quarter that found an existing solution."

Avoid disguised strengths ("I work too hard") and weaknesses central to the role ("I find debugging tedious").

## Failure modes and common mistakes

- **Blame-shifting**, even subtly: "the requirements were unclear", "QA missed it".
- **No prevention**, only recovery.
- **No evidence the learning lasted.**
- **Disguised-strength weaknesses.** Interviewers have heard them all.
- **Defensive retelling of feedback.** The score is about your response, not whether the feedback was fair.

## Interview questions and model answers

**Q: Tell me about a time you failed.**
A: S4: the retry change that duplicated emails. What I did wrong, stated plainly; rollback in forty minutes; idempotency keys and a timeout test as prevention; and six months later catching the same bug in a payments review.

**Q: Tell me about critical feedback you received.**
A: S8: design documents that buried the decision. What the feedback was, what I did, and that the one-page format became the team template.

**Q: What is your biggest weakness?**
A: A real, relevant weakness, the specific change I made, and evidence that the change is working.

## Exercises

1. Write your main failure story in five parts: what happened, your part, recovery, prevention, lasting change.
2. If the "lasting change" part is empty, choose a different failure or find the later evidence.
3. Write down the hardest feedback you have received in two years, and what you changed afterwards.
4. Write an answer to "biggest weakness" that includes evidence of improvement.

## Chapter summary

Failure and feedback stories are scored on what you did afterwards. Choose a real, consequential mistake that was yours; state your part plainly; describe recovery, prevention, and evidence that the learning lasted. For feedback, the score is your response. For weaknesses, choose a real one and show evidence that you are improving.

## Revision checklist

- [ ] My failure story is real, mine, and consequential.
- [ ] It includes prevention and a later example of changed behaviour.
- [ ] I have a received-feedback story and a given-feedback story.
- [ ] My weakness answer is real, relevant, and shows improvement.
