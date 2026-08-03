# Atomicity, Transactions, Scripts/Functions, Rate Limits, and Locks

## Atomic single commands first

Prefer one built-in command when it expresses the invariant. `SET key value NX PX ttl` atomically claims a lease; separate `SETNX` then `EXPIRE` can leave a permanent key if the client dies between them.

## `MULTI`/`EXEC`

Redis transactions queue commands and execute them without interleaving at `EXEC`. They do not provide SQL-style rollback for a command that errors during execution. Syntax/queue-time errors and runtime type errors have different outcomes; inspect every result.

```text
MULTI
INCR counter
EXPIRE counter 60
EXEC
```

Another client cannot interleave commands during the execution block, but reads before `MULTI` are not protected.

## Optimistic control with `WATCH`

```text
WATCH balance:42
GET balance:42
MULTI
SET balance:42 new-value
EXEC
```

If a watched key changes, `EXEC` aborts and the client retries from a fresh read. Keep retries bounded and avoid external effects inside the attempt. Under high contention, repeated aborts can amplify load.

## Lua scripts and Redis Functions

A script/function can check and mutate atomically on the executing node:

```lua
local current = tonumber(redis.call('GET', KEYS[1]) or '0')
local requested = tonumber(ARGV[1])
if current < requested then
  return 0
end
redis.call('DECRBY', KEYS[1], requested)
return 1
```

Pass keys through `KEYS`, values through `ARGV`, keep execution short/deterministic, and make all cluster keys share a slot. Long scripts block command progress on that node. Version/deploy/test scripts as production code and handle the “write completed, reply lost” case.

## Rate limiting

### Fixed window

Increment a key and set TTL atomically. Simple, but permits bursts around window boundaries.

### Sliding log

Use a sorted set of unique request IDs scored by time; atomically remove old entries, count, and insert if allowed. Accurate but memory grows with requests in the window.

### Token bucket

Store tokens and last-refill time; script calculates refill and consumes atomically. Supports controlled bursts with bounded state. Use server time or carefully address client clock skew.

Every limiter needs scope/key, limit/window, clock, atomicity, TTL cleanup, fail-open/closed policy, and observability. A Redis outage cannot be answered universally: login abuse may fail closed; low-risk recommendation refresh may fail open.

## Distributed locks and fencing

Acquire:

```text
SET lock:order:{42} random-owner-token NX PX 5000
```

Release only if value still matches, using a script. Never `DEL` blindly: the lease may expire and another owner may acquire before the old owner releases.

Even token-safe release does not stop a paused old owner from modifying a protected database after its lease expires:

```text
owner A lease=1 pauses
lease expires
owner B lease=2 writes
owner A resumes and writes stale result
```

A **fencing token** is a monotonically increasing lease number that the protected resource rejects when older than the last seen token. If the resource cannot enforce fencing/idempotent conditional writes, the Redis lock is only a best-effort coordination hint.

## Edge matrix

| Trap | Failure | Repair |
|---|---|---|
| `SETNX` then `EXPIRE` | permanent lock | atomic `SET NX PX` |
| `DEL` on release | deletes another owner’s lease | compare token then delete |
| long script | node-wide latency | bounded atomic work |
| `MULTI` assumed rollback | partial runtime command errors | validate/types and inspect results |
| rate limiter client clock | skew/manipulation | server time or defined tolerance |
| retry after timeout | duplicate mutation | idempotent operation ID/result ledger |

## Practice and solution direction

- **Foundation:** Repair a two-command lock acquisition.
- **Interview Core:** Implement token bucket inputs and outputs as one script/function.
- **SDE-2 Follow-up:** Protect a database write with fencing: allocate token, include it in `UPDATE ... WHERE last_token < ?`, and reject stale owners.
