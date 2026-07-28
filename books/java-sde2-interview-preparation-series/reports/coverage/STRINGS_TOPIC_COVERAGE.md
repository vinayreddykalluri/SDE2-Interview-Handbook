# Strings and String Patterns Topic Coverage

| Topic | Required depth | Previous state | Final state | Chapter | Examples/figures | Practice | Cross-reference | Validation |
|---|---|---|---|---:|---|---|---|---|
| String object/reference model | Foundation | Adequate mapped | Strong visual | 1 | reference/immutability figure | A1-A3, B1 | Java Fundamentals | companion mechanics |
| Immutability/reassignment/final | Foundation | Adequate | Strong | 1 | returned-value and alias examples | A2/A7, B2 | Java Fundamentals | source/companion |
| Literals/pool/new String | Foundation | Adequate | Strong | 1 | identity/content runnable output | A3, B1 | JVM Internals | source validation |
| Core String API | Foundation | Too shallow | Strong | 1 | substring/search/replace/strip/conversion | A5-A8, B3/B9 | Java Fundamentals | companion basics |
| Null/empty/blank | Foundation | Missing | Strong visual | 1 | contract decision figure | A4/A8, B4, C3 | Exceptions | source validation |
| Equality/order/case | Foundation | Adequate | Strong | 1-2 | equals/Objects/compare/locale | A3, B1/B9, C2 | Advanced Java | companion equality |
| Pass-by-value | Foundation | Too shallow | Strong | 1 | reassigned parameter | A13 | Java Fundamentals | source validation |
| UTF-16 char units | Interview Core | Adequate | Strong visual | 2 | four-unit diagram and indexes | A14-A15, B5 | Java Fundamentals | code-point checks |
| Code-point traversal/index mapping | Interview Core | Too shallow | Strong | 2 | index/advance dry run | C6, D6 | Advanced Java | reverse checks |
| Grapheme boundary | SDE-2 intro | Mentioned | Adequate bounded | 2 | combining-sequence failure | D6, E3/E5 | Advanced Java | contextual validation |
| Normalization/case/locale | SDE-2 intro | Too shallow | Strong bounded | 2/7 | NFC and Locale.ROOT examples | A16, C12, F3 | Security/Advanced Java | source validation |
| Digits and Character API | Foundation | Adequate | Strong | 2-3 | ASCII and Unicode conversions | A12, B6, C5 | Number Systems | parser checks |
| Bytes and charsets | Interview Core | Too shallow mapped | Strong boundary | 2-3 | explicit UTF-8 round trip | F1/G | I/O and NIO | source validation |
| StringBuilder APIs | Foundation | Adequate | Strong visual | 3 | construction/capacity figure | A9-A10, B7, C4 | Complexity | join check |
| Delimiters/StringJoiner | Foundation | Missing | Strong | 3 | three construction patterns | D3 | Java Fundamentals | join check |
| char-array transformations | Interview Core | Adequate | Strong qualified | 3 | ASCII reversal | C6, E3 | Arrays | source validation |
| split/regex/trailing fields | Interview Core | Too shallow | Strong | 3 | dot and negative-limit traps | A11, B8, C7 | Advanced Java | companion check 50 |
| strict parsing/overflow | Interview Core | Adequate | Strong | 3 | negative accumulation/dry run | D5, F1 | Number Systems | min/max/overflow checks |
| grammar validation | Interview Core | Missing | Strong | 3 | ASCII identifier scanner | D4 | Parsing/Advanced Java | source validation |
| palindrome | Interview Core | Adequate | Strong visual | 4 | copy baseline, exact/phrase | A17, D7 | Two Pointers | checks 11-15 |
| one-deletion palindrome | Interview Core | Missing | Strong | 4 | first-mismatch branching | D8 | Recursion | checks 16-17 |
| anagram sorting/frequency | Interview Core | Adequate | Strong visual | 4 | sort/array/map choices | A18, E2 | Hashing | checks 18-19 |
| anagram grouping/signatures | Interview Core | Too shallow | Strong | 4 | delimiter-safe signature | D9 | Hashing | signature check |
| common prefix/run encoding | Interview Core | Missing | Strong | 4 | shrinking prefix/run scanner | D10-D11 | Tries/Parsing | checks 20-23 |
| Fixed sliding window | Foundation/Core | Missing | Strong | 5 | maximum-vowel update | D1 | Loop Mastery | checks 26-27 |
| Longest unique substring | Interview Core | Strong compressed | Strong visual | 5 | count/jump/result variants | B10, C8, D13 | Loop Mastery | checks 28-30 |
| At-most/exactly K distinct | Interview Core | Missing | Strong | 5 | map repair/count identity | A19, D14 | Hashing | checks 31-34 |
| Anagram window | Interview Core | Too shallow | Strong | 5 | nonzero-slot rolling difference | D12 | Hashing | checks 35-36 |
| Minimum covering window | Interview Core/SDE-2 | Too shallow | Strong visual | 5 | satisfied-types trace | A20, D15, F2 | Hashing | checks 37-39 |
| Replacement-budget window | SDE-2 | Missing | Strong | 5 | stale-maximum proof boundary | E4 | Sliding Windows | check 40 |
| Code-point windows | SDE-2 | Too shallow | Strong qualified | 5 | int-array/map variant | E5 | Unicode | source validation |
| Naive substring search | Foundation/Core | Too shallow | Strong | 6 | last-alignment implementation | D16 | Complexity | checks 41-42 |
| LPS derivation and KMP | Interview Core/SDE-2 | Adequate compressed | Strong visual | 6 | LPS figure, dry run, first match | A21, C10, F3 | String Algorithms | checks 43-46 |
| All/overlapping matches | Interview Core | Missing | Strong | 6 | fallback after full match | B11-B12, D16 | String Algorithms | check 45 |
| Rolling hash | SDE-2 | Adequate | Strong qualified | 6 | verified modular window | A22, C11, D17 | Hashing/Number Systems | checks 47-48 |
| Z algorithm | SDE-2 overview | Adequate | Adequate bounded | 6 | prefix-match intuition/choice table | A22/E8 | Advanced DSA | source validation |
| Differential testing | SDE-2 | Missing | Strong | 7 | fixed-seed naive/KMP comparison | D18, F3 | Testing | check 49 |
| Interview communication | SDE-2 | Too shallow | Strong | 7 | seven-question contract/five-part explanation | E1-E8 | All later books | contextual validation |
| Production boundaries | SDE-2 | Too shallow | Strong | 7 | size/redaction/security/regex/stream/cache | A24, E6-E7, G | Backend/Security | review |
| Practice and solutions | Publication | Too shallow | Strong | 8-9 | 78 lab items and rubrics | all categories | next-volume handoff | separated solutions |
| Executable companion | Publication | Missing | Strong | 10 | Java 21 class | 50 checks | none | warning-free pass |

## Coverage conclusion

The enhanced volume covers the Java String mechanics required by SDE-1 interviews and the high-value text patterns expected at SDE-2. Full regex-engine internals, locale collation, tries, suffix structures, approximate matching, edit-distance DP, streaming decoders, and search-system design remain intentionally cross-referenced rather than duplicated.
