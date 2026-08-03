# SQL Reasoning: Reads, Writes, Joins, Grouping, CTEs, and Windows

The best SQL answers begin by defining the output grain: **what does one result row represent?** Most duplicate-row bugs begin when a candidate never answers that question.

## Reads and writes with explicit intent

Avoid `SELECT *` at service boundaries. Request the columns the contract needs; that reduces coupling, transfer, and accidental exposure.

```sql
SELECT order_id, status, total_cents, created_at
FROM purchase_order
WHERE customer_id = ?
ORDER BY created_at DESC, order_id DESC
LIMIT ?;
```

The second sort key makes ordering deterministic when timestamps tie.

For conditional updates, put the precondition in the statement:

```sql
UPDATE purchase_order
SET status = 'PAID', version = version + 1
WHERE order_id = ?
  AND status = 'CREATED'
  AND version = ?;
```

An affected-row count of zero is information: missing row, wrong state, or concurrent version change. The service must distinguish them only if its API contract requires that distinction.

## Joins: preserve the intended side

```sql
SELECT c.customer_id, COUNT(o.order_id) AS order_count
FROM customer c
LEFT JOIN purchase_order o
  ON o.customer_id = c.customer_id
 AND o.status = 'PAID'
GROUP BY c.customer_id;
```

Putting the order status in `ON` retains customers with zero paid orders. Moving it to `WHERE` removes null-extended rows and effectively turns the query into an inner join.

### Join multiplication

Joining orders to both items and payments can multiply rows (`items × payments`) before aggregation. Repair it by aggregating each many-side to the required grain first, then joining the summaries.

## Grouping and `HAVING`

`WHERE` filters input rows before grouping. `HAVING` filters groups after aggregation.

```sql
SELECT customer_id, SUM(total_cents) AS paid_total
FROM purchase_order
WHERE status = 'PAID'
GROUP BY customer_id
HAVING SUM(total_cents) >= 100_000;
```

Keep selected nonaggregated columns consistent with the grouping contract. Do not depend on permissive modes returning an arbitrary value.

## Subqueries and CTEs

A CTE names an intermediate relation; it does not guarantee materialization or performance improvement.

```sql
WITH customer_totals AS (
    SELECT customer_id, SUM(total_cents) AS total_cents
    FROM purchase_order
    WHERE status = 'PAID'
    GROUP BY customer_id
)
SELECT c.customer_id, t.total_cents
FROM customer c
JOIN customer_totals t ON t.customer_id = c.customer_id
WHERE t.total_cents >= 100_000;
```

Recursive CTEs can traverse bounded hierarchies, but graph cycle handling and recursion depth must be explicit.

## Window functions keep row detail

`GROUP BY` collapses rows. A window function computes across a partition while retaining each row.

```sql
SELECT order_id,
       customer_id,
       total_cents,
       ROW_NUMBER() OVER (
           PARTITION BY customer_id
           ORDER BY created_at DESC, order_id DESC
       ) AS recency_rank,
       SUM(total_cents) OVER (
           PARTITION BY customer_id
           ORDER BY created_at, order_id
           ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
       ) AS running_total
FROM purchase_order;
```

For running totals, write the window frame deliberately. Peer rows can make default `RANGE` behavior differ from the row-by-row result you imagined.

### Top N per group

```sql
WITH ranked AS (
    SELECT o.*,
           ROW_NUMBER() OVER (
               PARTITION BY customer_id
               ORDER BY created_at DESC, order_id DESC
           ) AS rn
    FROM purchase_order o
)
SELECT order_id, customer_id, total_cents, created_at
FROM ranked
WHERE rn <= 3;
```

## `EXISTS`, `IN`, and counting

Use `EXISTS` when the question is whether a matching row exists; it communicates that no count is needed. Optimizers may transform equivalent forms, so verify the plan rather than asserting one syntax is universally faster.

## Predict and debug

**Question:** Why can this count be too large?

```sql
SELECT o.customer_id, COUNT(*)
FROM purchase_order o
JOIN order_item i ON i.order_id = o.order_id
JOIN payment p ON p.order_id = o.order_id
GROUP BY o.customer_id;
```

**Answer:** each order appears once per item-payment combination. Decide whether the metric counts orders, items, or payments, aggregate at that grain, and only then join.

## Quick check

1. What does one result row represent in a top-three-orders query?
2. When does a `LEFT JOIN` become effectively inner?
3. Why is a CTE not automatically faster?
4. What is retained by a window function that grouping removes?

## Practice

- **Foundation:** Return customers with zero orders.
- **Interview Core:** Return the second-highest distinct order total per customer.
- **Interview Core:** Compute a seven-row moving average with deterministic order.
- **SDE-2 Follow-up:** Diagnose a revenue query that doubled after a payments join.

## Solution direction

Use `LEFT JOIN ... IS NULL` or `NOT EXISTS` for zero orders; use `DENSE_RANK` for second-highest distinct value; define a `ROWS BETWEEN 6 PRECEDING AND CURRENT ROW` frame; and aggregate each many-side at order grain before combining.
