# DSA 08-17 Code Validation

## Companion compilation

Command:

```bash
find content/volumes/{08-hashing-maps-sets-and-prefix-state,09-recursion-and-backtracking,10-linked-lists,11-stacks-queues-deques-and-monotonic-patterns,12-binary-search,13-trees-bsts-and-tries,14-heaps-priority-queues-and-top-k,15-graphs,16-greedy-algorithms,17-dynamic-programming}/code -name '*.java' -print0 \
  | xargs -0 javac --release 17 -Xlint:all -Werror -d <temporary-classes>
```

| Metric | Result |
|---|---:|
| Standalone companions | 10 |
| Successfully compiled | 10 |
| Compilation failures | 0 |
| Executed companions | 10 |
| Executable checks | 41 |
| Failed checks | 0 |
| Output mismatches | 0 |

Observed outputs:

```text
PASS 5 hashing checks
PASS 4 recursion checks
PASS 4 linked-list checks
PASS 3 ordering-structure checks
PASS 5 binary-search checks
PASS 3 tree checks
PASS 5 heap checks
PASS 4 graph checks
PASS 3 greedy checks
PASS 5 dynamic-programming checks
```

## Repository source validation

`python3 scripts/validate_series.py --source-only` passed. It compiled and ran 19 series-native classes across the complete series and retained the existing Number Systems validation of 820 assertions plus 24 standalone printed examples.

Markdown method excerpts are educational fragments; the complete companion class in each volume is the executable source of truth for the newly added checks.
