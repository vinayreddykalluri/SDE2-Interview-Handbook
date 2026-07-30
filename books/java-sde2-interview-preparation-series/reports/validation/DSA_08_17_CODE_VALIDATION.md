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

`python3 scripts/validate_series.py --source-only` passed under Java 21 with `-Xlint:all -Werror`.

| Publication-validation metric | Result |
|---|---:|
| Complete series-native classes compiled and executed | 40 |
| Complete classes belonging to DSA 08-17 | 30 |
| Existing DSA 08-17 companion classes | 10 |
| Existing DSA 08-17 main pattern classes | 10 |
| New essential-clinic classes | 10 |
| New clinic runtime assertions | 30 |
| Compilation or execution failures | 0 |
| Existing Number Systems assertions retained | 820 |
| Existing Number Systems standalone printed examples retained | 24 |

The validator now scans mapped chapter classes even when the volume also supplies a separate companion. Incomplete educational method fragments remain excluded; every complete public class with `main` is compiled and executed.
