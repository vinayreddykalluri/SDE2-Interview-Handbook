# Bit Manipulation Topic Coverage

| Topic | Required depth | Previous state | Final state | Chapter | Examples or checks | Practice | Validation |
|---|---|---|---|---:|---|---|---|
| Binary place value | Foundation | Brief | Visual and sequential | 1 | decimal 26, binary 45 | K01-K03, A1 | PDF + source |
| Hex-to-bit grouping | Foundation | Missing | Four-bit table and masks | 1 | `0x2D`, `0xFF` | K01-K03 | PDF |
| Fixed-width signed representation | Interview Core | Adequate | Expanded with unsigned APIs | 1, 6 | `-1`, MIN_VALUE | O04, O15, O20 | companion + PDF |
| Two's complement | Interview Core | Adequate | Step-by-step and boundary-safe | 1 | `5`, `-5`, MIN_VALUE | K07, A1 | companion + PDF |
| AND/OR/XOR/NOT | Foundation | Brief | Truth tables and dry runs | 1 | 12 and 10 | O01-O02 | companion + PDF |
| Left/right shifts | Interview Core | Adequate | Signed, logical, rounding, masked distance | 1 | `-3`, `1 << 32` | O03-O05, D03 | companion + PDF |
| Small-type promotion | Interview Core | Adequate | Focused byte inversion examples | 1 | `(~flags) & 0xFF` | K10, D05 | companion + PDF |
| Bit display/debugging | Foundation | Missing | Padded 32/64-bit helpers | 1 | `bits32` | C01 | companion |
| Test/set/clear/toggle | Foundation | Adequate | Derived and validated | 2 | `long` helpers | D01-D04, C02 | companion |
| Range masks and fields | Interview Core | Brief | Full 0/64-width and value validation | 2, 6 | extract/replace | C03-C04, D16 | companion |
| Lowest set bit | Interview Core | Strong | Proof plus set-position iteration | 2 | `x & -x` | K14, O07 | companion |
| Remove lowest set bit | Interview Core | Strong | Proof, invariant, count | 2 | Kernighan | K13, C05 | companion |
| Power of two/four | Interview Core | Partial | Contracts and mask derivation | 2 | positive checks | O09, D06, C06 | companion |
| Hamming distance | Interview Core | Missing | XOR plus bit count | 2 | two patterns | C07 | companion |
| Reverse bits | Interview Core | Missing | 32-step unsigned consumption | 2 | `reverseBits` | C08 | companion |
| Count bits through n | Interview Core | Missing | Two DP recurrences | 2 | `countBitsThrough` | C05 | companion |
| Java bit utilities | Interview Core | Brief | API decision table | 2, 6 | `bitCount`, zeros, rotate | O08, O18 | companion + PDF |
| Single among pairs | Interview Core | Strong | Contract, invariant, dry run | 3 | `[4,1,2,1,2]` | C09, F04 | companion |
| Missing 0 through n | Interview Core | Missing | XOR implementation with caveat | 3 | `[3,0,1]` | D08, C10 | companion |
| Two singles | Interview Core | Strong | Sign-bit-safe partition | 3 | `[1,2,1,3,2,5]` | D10, C11 | companion |
| Single among triples | Interview Core | Strong but compressed | Count baseline plus state machine | 3 | negative answer | D11, C12 | companion |
| Prefix/range XOR | Interview Core | Adequate | Half-open model and validation | 3 | `[4,2,7,2]` | D09, C13 | companion |
| XOR 0 through n | Interview Core | Partial | Four-case derivation | 3 | first eight prefixes | O12, C14 | companion |
| Target-XOR subarrays | Interview Core | Missing | Prefix-frequency solution | 3 | target 6 | D15, C15 | companion |
| Subset enumeration | Interview Core | Adequate | Duplicate semantics and output bounds | 4 | `[10,20,30]` | C16, F08 | PDF |
| Set-position iteration | Interview Core | Missing | Sparse mask loop | 4 | trailing-zero index | C16 | companion concepts |
| Submask enumeration | SDE-2 | Adequate | Zero-safe loop and proof | 4 | mask `10110` | O13, D14, C17 | companion |
| `O(3^n)` derivation | SDE-2 | Brief | Three-role proof | 4 | all mask/submask pairs | K24, A2 | PDF |
| Gray code | SDE-2 | Missing | Incremental-state use and limit | 4 | first eight codes | O14, C18 | companion |
| Bitmask state/DP | SDE-2 | Missing | Assignment-state introduction | 4 | `best[mask]` | F09, A3 | PDF |
| Maximum XOR trie | SDE-2 | Too shallow | Full Java, baseline, signed policy | 5 | classic max 28 | C19, F10 | companion |
| Offline constrained XOR | SDE-2 | Missing | Sort-query strategy | 5 | limit sweep | F10 | PDF |
| Range bitwise AND | SDE-2 | Missing | Common prefix and clear-lowest | 5 | `[26,30]` | O19, C20 | companion |
| Total bits 1 through n | SDE-2 | Missing | Highest-block recurrence | 5 | `n = 13` | D19, C21 | companion |
| Significant complement | Interview Core | Missing | Logical-width method | 5 | 5 to 2 | D13 | companion |
| Minimum XOR pair | SDE-2 | Missing | Sorted-adjacency proof sketch | 5 | `[9,5,3]` | C22 | companion |
| Distinct subarray OR | SDE-2 | Missing | Width-bounded frontier | 5 | `[1,2]` | C23 | companion |
| Add without plus | Interview Core | Missing | Sum/carry derivation | 5 | `-4 + 9` | O17 | companion |
| BitSet | Interview Core | Brief | Usage, mutation, API traps | 6 | length/cardinality | O16, D18 | companion + PDF |
| EnumSet | Interview Core | Brief | Named-flag decision | 6 | permissions | K25, C24 | PDF |
| BigInteger bits | SDE-2 Follow-up | Missing | Immutable arbitrary-width intro | 6 | bit 100 | K25 | PDF |
| Atomic flag updates | SDE-2 Follow-up | Brief | Lost-update explanation and CAS API | 6 | `AtomicLong` | D17, F14 | companion |
| Schema and serialization | SDE-2 Follow-up | Strong | Version, trust, byte order | 6 | known-mask validation | F13-F15 | PDF |
| Interview traps | Interview Core | Partial | Twenty-item rapid trap list | 6 | width/sign/mutation | O/D banks | PDF |
| Cumulative readiness | All levels | Missing | Three assessments and final gate | 7-8 | assessment rubrics | A1-A3 | PDF |

## Coverage result

All required foundation, interview-core, and targeted SDE-2 bit-manipulation topics are now covered in prerequisite order. Deep dynamic programming, advanced XOR linear bases, and compressed bitmap internals remain correctly bounded to later or specialized study.
