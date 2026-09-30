# 5. Split Expenses: Money, Invariants, and Settlement

## Learning objectives

By the end of this chapter, you should be able to:

- represent money so that no operation can create or destroy a cent;
- split an amount equally or by percentage and prove that the shares always sum to the total;
- model expenses, balances, and settlements so that the zero-sum invariant holds after every operation;
- explain why minimising the number of settlement transfers is NP-hard, and what to build instead; and
- extend the design to groups, currencies, and edits to past expenses.

## Why this matters at SDE-2

"Design Splitwise" looks like CRUD: users, groups, expenses. The interviewer is actually probing two things. The first is money: whether you know that floating point cannot represent cents and that division creates remainders someone must own. The second is the settlement algorithm: whether you reach for "minimise transactions" and whether you know that its exact form is intractable. Both are domain invariants, and this problem exists to see whether you protect them.

## First-principles model

A shared-expense system is a **ledger with a zero-sum invariant**.

- An **expense** says one person paid an amount and assigns a share of it to each participant.
- A person's **balance** is what they paid minus what they owe. Positive means they are owed money; negative means they owe.
- **Invariant:** across a group, balances always sum to exactly zero. Every expense adds `+paid` to the payer and `-share` to each participant, and the shares sum to the amount paid - *if and only if* splitting never loses or invents a cent.
- A **settlement plan** is a list of transfers that brings every balance to zero.

The first invariant is about arithmetic and the second is about algorithms. Get the first wrong and no algorithm can fix the second.

## Core terminology

- **Minor unit:** the smallest unit of a currency - cents for USD, paise for INR, and none for JPY.
- **Remainder:** what is left when an amount does not divide evenly; it must be assigned to someone.
- **Basis point:** one hundredth of a percent; 10,000 basis points make 100%.
- **Largest-remainder method:** assign rounded-down shares, then give the leftover units to the shares with the largest fractional parts.
- **Net balance:** paid minus owed, for one person across all expenses.
- **Zero-sum group:** a subset of people whose balances sum to zero and can settle among themselves.

## Detailed mechanics

### Floating point cannot hold money

The companion runs the classic demonstrations:

```text
double: $100 / 3 rounded to cents = 33.33 each, x3 = 99.99
double: 0.1 + 0.2 == 0.3 is false
```

The first is not really a floating-point problem - it is a *remainder* problem that floating point hides. Three shares of $33.33 lose a cent no matter how the arithmetic is done. The second is the floating-point problem: 0.1 has no exact binary representation, so sums drift.

The fix is to store money as an integer count of minor units - `long` cents - and to make division return shares that provably sum to the total. `BigDecimal` is also correct, but it still needs an explicit rounding mode and still leaves a remainder; it does not remove the need to decide who gets the extra cent.

Minor units are per currency. USD has 2 decimal places, JPY has 0, and some currencies have 3. A `Money` value object should carry its currency and refuse to add amounts in different currencies.

### Equal split: the remainder must go somewhere

```java
static Map<String, Long> splitEqually(long totalCents, List<String> participants) {
    long base = totalCents / participants.size();
    long remainder = totalCents % participants.size();
    Map<String, Long> shares = new LinkedHashMap<>();
    for (int i = 0; i < participants.size(); i++) {
        shares.put(participants.get(i), base + (i < remainder ? 1 : 0));
    }
    return shares;
}
```

```text
cents:  $100 / 3 -> {asha=3334, ben=3333, chen=3333}, sum $100.00
```

The rule "the first `remainder` participants in list order pay one extra cent" is arbitrary, and that is fine - *deterministic* is what matters. State the rule. Some products give the extra cent to the payer, so the payer is never owed a fraction of a cent; others rotate it. Any of these is defensible; an undefined order is not.

### Percentage split: the largest-remainder method

Percentages add a second source of rounding. Store them as basis points so that 33.33% is the integer 3,333, and require that they sum to exactly 10,000. Then:

1. compute each exact share as `total x bp`, which is in units of 1/10,000 of a cent;
2. give each person the rounded-down share;
3. hand out the leftover cents one at a time to the largest fractional remainders.

```text
$100.01 at 33.34/33.33/33.33% -> {asha=3335, ben=3333, chen=3333}
```

Asha's exact share is 3,334.3334 cents; Ben's and Chen's are 3,333.3333. After rounding down, one cent is left over and goes to the largest remainder - Asha's. The companion checks the method over 10,000 random totals and random three-way percentages:

```text
largest-remainder split: 10,000 random totals and percentages, every sum exact
```

A split function whose output provably sums to its input is the foundation the rest of the design stands on.

### Expenses, balances, and the zero-sum invariant

```text
Expense (entity)    id, paidBy, amount (Money), shares: Map<UserId, Money>, createdAt
Balance             Map<UserId, Money> per group, derived from expenses
SplitStrategy       Map<UserId, Money> split(Money total, List<UserId> people, params)
                    - EqualSplit, PercentageSplit, ExactSplit, SharesSplit
```

`SplitStrategy` is the policy seam: "split by exact amounts" and "split by shares (2 : 1 : 1)" are new implementations. Every strategy must satisfy the same contract - output sums to input - and one shared test can check every implementation.

The zero-sum check is one line, and the design should assert it after every expense is added:

```java
check(balances.values().stream().mapToLong(Long::longValue).sum() == 0, "balances sum to zero");
```

Should balances be stored or derived? Derive them from the expense list for correctness, and cache them for speed. The expense list is the source of truth; the balance map is a projection that can always be rebuilt. This is what makes *editing* or *deleting* a past expense safe: reverse its effect on the cache, or rebuild.

### Settlement: greedy is fine, optimal is intractable

The standard settlement is greedy: repeatedly have the largest debtor pay the largest creditor as much as possible. It always finishes in at most `n - 1` transfers for `n` people with non-zero balances, because each transfer zeroes at least one balance.

It is not always minimal. The companion's example has five balances:

```text
balances {A=3000, B=2000, C=2000, D=-4000, E=-3000}
greedy settles in 4 transfers: D->A 3000, E->B 2000, D->C 1000, E->C 1000
minimum is 3: {A,E} and {B,C,D} are zero-sum groups
```

The optimum settles `E -> A` for $30 inside one group, and `D -> B` and `D -> C` for $20 each inside the other: three transfers. The general rule: if *g* is the largest number of disjoint zero-sum groups the *n* non-zero balances can be partitioned into, the minimum settlement is exactly *n - g* transfers. A group of *k* people that cannot be split into smaller zero-sum groups needs *k - 1* transfers, and summing *k - 1* over the groups gives *n - g*. Maximising *g* is a partition problem, which is NP-hard.

The companion computes the optimum with an exponential dynamic program over subsets - fine for a dinner with five friends, useless for a group of 60. So the defensible design is:

- use greedy by default - at most *n - 1* transfers, O(n log n) with two heaps;
- optionally run the exact search when the group is small (say, at most 12 non-zero balances);
- tell the interviewer *why* you are not promising the minimum.

Candidates who announce "I'll minimise the number of transactions" and then write greedy have claimed something false. Candidates who say "minimum transfers is NP-hard, so I use greedy, which is bounded by *n - 1*" have shown they know where the difficulty is.

### Concurrency and ordering

Two people add expenses to the same group at the same time. If balances are a cached projection, updating them is a read-modify-write, and two concurrent updates can lose one. Options: a per-group lock around "append expense and update balances", or a single-writer queue per group. The per-group lock is the natural answer, because the zero-sum invariant is per group.

Settlements are payments: they must be idempotent. A settlement request carries a client-generated id, and recording the same id twice is a no-op. Otherwise a retried "Ben paid Asha $33.34" is applied twice and the ledger no longer balances.

### Standard extensions

| Extension | Where it lands |
|---|---|
| Split by shares (2 : 1 : 1) | a new `SplitStrategy`, reusing largest remainder |
| Multiple currencies | `Money` carries currency; balances are per currency; conversion is an explicit, dated event |
| Edit or delete an expense | reverse it against the cached balances, or rebuild from the expense log |
| Recurring expenses | a scheduler that creates ordinary expenses (chapter 6) |
| Activity feed | an event published per expense; the feed is a read model |
| Simplify debts across groups | the same settlement over the union of balances, with the same NP-hard caveat |

## Failure modes and common mistakes

- **`double` for money.** Drift and lost remainders.
- **Dropping the remainder.** `total / n` for every participant loses up to `n - 1` cents per expense.
- **Percentages as `double`.** 33.33% x 3 is 99.99%; validate integer basis points summing to 10,000.
- **No zero-sum assertion.** The ledger silently stops balancing.
- **Claiming minimal settlement with greedy.** Greedy is bounded, not optimal; minimal is NP-hard.
- **Non-idempotent settlements.** A retried payment is applied twice.

## Interview questions and model answers

**Q: How do you store amounts?**
A: As `long` minor units with a currency - cents for USD. Floating point cannot represent most decimal amounts, and even exact arithmetic leaves remainders on division. My split functions return shares that provably sum to the total.

**Q: $100 split three ways. Who pays the extra cent?**
A: One person pays $33.34 and two pay $33.33. The rule I use is the first participant in a fixed order; some products give it to the payer instead. What matters is that the rule is deterministic and stated, and that the shares sum to exactly $100.00.

**Q: How do you split by percentage?**
A: Percentages as integer basis points summing to 10,000, then the largest-remainder method: round every share down, then give the leftover cents to the largest fractional parts. I test it with random totals and percentages and check that every sum is exact.

**Q: Can you minimise the number of payments needed to settle up?**
A: The exact minimum is NP-hard: it equals the number of non-zero balances minus the maximum number of disjoint zero-sum groups, and finding those groups is a partition problem. I use greedy - largest debtor pays largest creditor - which needs at most *n - 1* transfers. For small groups I can run an exact search over subsets. With balances +30, +20, +20, -40, -30, greedy uses four transfers and the optimum is three.

**Q: Two people add expenses at the same moment. What can go wrong?**
A: If balances are cached, two read-modify-write updates can lose one. I lock per group around appending the expense and updating the balances, because the invariant is per group. Settlements carry an idempotency key so retries cannot double-apply.

## Exercises

1. Implement `SharesSplit` for ratios such as 2 : 1 : 1 and reuse the random sum-preservation test.
2. Add currency to the ledger and write the rule for a group that spends in two currencies.
3. Implement greedy settlement with two priority queues and confirm the *n - 1* bound on random groups.
4. Delete an expense from the middle of a group's history and show that the balances still sum to zero.

## Chapter summary

Money is integer minor units with a currency, and every split must return shares that sum exactly to the total: equal splits assign the remainder by a stated rule, and percentage splits use basis points and the largest-remainder method. Balances form a zero-sum ledger, derived from expenses and asserted after every change. Greedy settlement is bounded by *n - 1* transfers but is not minimal; the minimum is NP-hard, and saying so is part of the answer.

## Revision checklist

- [ ] I can show why `double` loses money and why remainders exist even without floating point.
- [ ] I can write an equal split and a largest-remainder percentage split that sum exactly.
- [ ] I assert that balances sum to zero after every expense.
- [ ] I can give the five-person counterexample where greedy settlement is not minimal.
- [ ] I can explain why minimum settlement is NP-hard and what I build instead.
