import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executable checks for behavioral-interview preparation.
 *
 * A story bank is data: stories tagged with the signals they demonstrate. That
 * makes coverage, reuse, and gaps computable instead of a feeling. A written
 * answer is also data: its sections can be timed and its pronouns counted.
 * Every number quoted in the chapters is printed here. Run it with:
 *
 *     javac --release 21 StoryBankModel.java && java -ea StoryBankModel
 *
 * Sections map to chapters:
 *   1. Signal coverage of a story bank, and its gaps                 (ch 2)
 *   2. Choosing stories for a loop without over-reusing one          (ch 2)
 *   3. Timing a STAR answer by section                               (ch 3)
 *   4. The "I" versus "we" ratio of an answer                        (ch 3)
 *   5. Mapping the bank onto Amazon's Leadership Principles          (ch 8)
 */
public final class StoryBankModel {

    private StoryBankModel() {}

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("check failed: " + message);
        }
    }

    // ------------------------------------------------------------------
    // Signals and a sample story bank
    // ------------------------------------------------------------------

    enum Signal {
        OWNERSHIP,
        AMBIGUITY,
        TECHNICAL_DEPTH,
        DELIVERY,
        CONFLICT,
        INFLUENCE,
        FAILURE,
        FEEDBACK,
        MENTORING,
        CUSTOMER_FOCUS,
        PRIORITIZATION,
        INCIDENT
    }

    record Story(String id, String title, Set<Signal> signals) {}

    /** A composite bank for a backend engineer with about five years' experience. */
    static List<Story> sampleBank() {
        return List.of(
                new Story(
                        "S1",
                        "Migrated order service off a shared database",
                        EnumSet.of(
                                Signal.OWNERSHIP,
                                Signal.AMBIGUITY,
                                Signal.TECHNICAL_DEPTH,
                                Signal.DELIVERY)),
                new Story(
                        "S2",
                        "Led the payment outage response and the fix",
                        EnumSet.of(Signal.INCIDENT, Signal.OWNERSHIP, Signal.TECHNICAL_DEPTH)),
                new Story(
                        "S3",
                        "Disagreed with the tech lead on a caching design",
                        EnumSet.of(Signal.CONFLICT, Signal.TECHNICAL_DEPTH, Signal.INFLUENCE)),
                new Story(
                        "S4",
                        "Shipped a retry change that caused duplicate emails",
                        EnumSet.of(Signal.FAILURE, Signal.OWNERSHIP, Signal.INCIDENT)),
                new Story(
                        "S5",
                        "Convinced three teams to adopt a shared idempotency library",
                        EnumSet.of(Signal.INFLUENCE, Signal.AMBIGUITY)),
                new Story(
                        "S6",
                        "Mentored a new engineer through their first on-call",
                        EnumSet.of(Signal.MENTORING, Signal.FEEDBACK)),
                new Story(
                        "S7",
                        "Cut scope to hit a regulatory deadline",
                        EnumSet.of(Signal.PRIORITIZATION, Signal.DELIVERY, Signal.CUSTOMER_FOCUS)),
                new Story(
                        "S8",
                        "Received blunt review feedback on a design doc",
                        EnumSet.of(Signal.FEEDBACK, Signal.FAILURE)));
    }

    static Map<Signal, List<String>> coverage(List<Story> bank) {
        Map<Signal, List<String>> map = new EnumMap<>(Signal.class);
        for (Signal s : Signal.values()) {
            map.put(s, new ArrayList<>());
        }
        for (Story story : bank) {
            for (Signal s : story.signals()) {
                map.get(s).add(story.id());
            }
        }
        return map;
    }

    static void storyCoverage() {
        System.out.println("== 1. Signal coverage of an 8-story bank ==");
        Map<Signal, List<String>> map = coverage(sampleBank());
        List<Signal> thin = new ArrayList<>();
        for (Map.Entry<Signal, List<String>> e : map.entrySet()) {
            System.out.printf(
                    "  %-15s %d %s %s%n",
                    e.getKey(),
                    e.getValue().size(),
                    e.getValue().size() == 1 ? "story  " : "stories",
                    e.getValue());
            if (e.getValue().size() < 2) {
                thin.add(e.getKey());
            }
        }
        System.out.printf("  signals with fewer than two stories: %s%n", thin);
        check(
                thin.equals(
                        List.of(
                                Signal.CONFLICT,
                                Signal.MENTORING,
                                Signal.CUSTOMER_FOCUS,
                                Signal.PRIORITIZATION)),
                "the sample bank is thin on conflict, mentoring, customer focus, prioritization");
        check(
                map.values().stream().noneMatch(List::isEmpty),
                "every signal has at least one story");
    }

    // ------------------------------------------------------------------
    // 2. Choosing stories for a loop
    // ------------------------------------------------------------------

    /**
     * Assign one story per question, preferring the story that covers the
     * question's signal with the fewest other uses so far, and never using
     * one story more than maxUses times across the loop.
     */
    static Map<String, String> assign(List<Story> bank, List<Signal> questions, int maxUses) {
        Map<String, Integer> uses = new LinkedHashMap<>();
        Map<String, String> plan = new LinkedHashMap<>();
        for (int q = 0; q < questions.size(); q++) {
            Signal wanted = questions.get(q);
            Story best = null;
            for (Story story : bank) {
                if (!story.signals().contains(wanted)
                        || uses.getOrDefault(story.id(), 0) >= maxUses) {
                    continue;
                }
                if (best == null
                        || uses.getOrDefault(story.id(), 0) < uses.getOrDefault(best.id(), 0)
                        || (uses.getOrDefault(story.id(), 0).equals(uses.getOrDefault(best.id(), 0))
                                && story.signals().size() < best.signals().size())) {
                    best = story;
                }
            }
            String key = "Q" + (q + 1) + " " + wanted;
            if (best == null) {
                plan.put(key, "GAP");
            } else {
                plan.put(key, best.id());
                uses.merge(best.id(), 1, Integer::sum);
            }
        }
        return plan;
    }

    static void loopPlanning() {
        System.out.println();
        System.out.println("== 2. Planning a six-question loop, each story used at most once ==");
        List<Signal> loop =
                List.of(
                        Signal.OWNERSHIP,
                        Signal.CONFLICT,
                        Signal.FAILURE,
                        Signal.INCIDENT,
                        Signal.INFLUENCE,
                        Signal.OWNERSHIP);
        Map<String, String> plan = assign(sampleBank(), loop, 1);
        plan.forEach((q, s) -> System.out.printf("  %-18s -> %s%n", q, s));
        check(!plan.containsValue("GAP"), "the bank covers this loop with no story reused");

        List<Signal> harder = List.of(Signal.MENTORING, Signal.FEEDBACK, Signal.MENTORING);
        Map<String, String> strained = assign(sampleBank(), harder, 1);
        System.out.printf("  mentoring, feedback, mentoring again -> %s%n", strained.values());
        check(strained.containsValue("GAP"), "one mentoring story cannot answer two questions");
    }

    // ------------------------------------------------------------------
    // 3 and 4. Timing and pronouns in a written answer
    // ------------------------------------------------------------------

    static final String SITUATION =
            "Our checkout service and the order service shared one PostgreSQL database. "
                    + "Every schema change needed both teams, and a slow order query had "
                    + "caused two checkout incidents in one quarter.";
    static final String TASK =
            "I owned the plan to give the order service its own database without "
                    + "downtime, and I had one quarter to do it.";
    static final String ACTION =
            "I started by mapping every query that crossed the boundary, and found "
                    + "fourteen, three of them joins that checkout depended on. I proposed "
                    + "replacing the joins with an order-summary API, and wrote a one-page "
                    + "design that listed the three options I had rejected and why. The "
                    + "checkout lead pushed back on latency, so I built a prototype and "
                    + "measured it: the API added four milliseconds at p99, which we agreed "
                    + "was acceptable. I then ran the migration in four steps, each "
                    + "reversible: dual writes behind a flag, a backfill with row-count and "
                    + "checksum comparison, reads switched one endpoint at a time, and "
                    + "finally removing the old tables. During the backfill I found that "
                    + "two percent of rows differed because of a timezone bug in an old "
                    + "job, so I stopped, fixed the job, and re-ran the comparison before "
                    + "continuing. I wrote a runbook for each step so that anyone on call "
                    + "could roll back.";
    static final String RESULT =
            "We finished two weeks early with no customer-facing downtime. Checkout "
                    + "had no database-related incidents in the next two quarters, and "
                    + "schema changes stopped needing cross-team review. I would now start "
                    + "the data comparison earlier, because finding the timezone bug late "
                    + "cost us a week.";

    static int words(String text) {
        String trimmed = text.trim();
        return trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
    }

    static int count(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text.toLowerCase(Locale.ROOT));
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    static void answerTiming() {
        System.out.println();
        System.out.println("== 3. Timing a STAR answer at 150 words per minute ==");
        String[] names = {"Situation", "Task", "Action", "Result"};
        String[] parts = {SITUATION, TASK, ACTION, RESULT};
        int total = 0;
        for (String p : parts) {
            total += words(p);
        }
        for (int i = 0; i < parts.length; i++) {
            int w = words(parts[i]);
            System.out.printf(
                    "  %-9s %3d words  %4.1f%%  %3.0f s%n",
                    names[i], w, 100.0 * w / total, w / 150.0 * 60);
        }
        double seconds = total / 150.0 * 60;
        System.out.printf(
                "  total     %3d words          %3.0f s (%.1f min)%n",
                total, seconds, seconds / 60);
        double actionShare = (double) words(ACTION) / total;
        double setupShare = (double) (words(SITUATION) + words(TASK)) / total;
        check(actionShare >= 0.55, "action is at least 55% of the answer");
        check(setupShare <= 0.25, "situation and task together are at most 25%");
        check(seconds >= 90 && seconds <= 180, "the answer runs between 1.5 and 3 minutes");

        System.out.println();
        System.out.println("== 4. \"I\" versus \"we\" ==");
        Pattern singular = Pattern.compile("\\b(i|my|me)\\b");
        Pattern plural = Pattern.compile("\\b(we|our|us)\\b");
        String all = String.join(" ", parts);
        String actionOnly = ACTION;
        int iAll = count(singular, all);
        int weAll = count(plural, all);
        int iAction = count(singular, actionOnly);
        int weAction = count(plural, actionOnly);
        System.out.printf("  whole answer: %d singular, %d plural%n", iAll, weAll);
        System.out.printf("  action section: %d singular, %d plural%n", iAction, weAction);
        check(iAction > 3 * weAction, "the action section is about what the candidate did");
        check(weAll > 0, "shared outcomes are still credited to the team");
    }

    // ------------------------------------------------------------------
    // 5. Amazon Leadership Principles coverage
    // ------------------------------------------------------------------

    static final List<String> AMAZON_LPS =
            List.of(
                    "Customer Obsession",
                    "Ownership",
                    "Invent and Simplify",
                    "Are Right, A Lot",
                    "Learn and Be Curious",
                    "Hire and Develop the Best",
                    "Insist on the Highest Standards",
                    "Think Big",
                    "Bias for Action",
                    "Frugality",
                    "Earn Trust",
                    "Dive Deep",
                    "Have Backbone; Disagree and Commit",
                    "Deliver Results",
                    "Strive to be Earth's Best Employer",
                    "Success and Scale Bring Broad Responsibility");

    /** Which LPs a signal can evidence. A mapping, not an official Amazon rubric. */
    static Map<Signal, List<String>> lpMapping() {
        Map<Signal, List<String>> m = new EnumMap<>(Signal.class);
        m.put(Signal.OWNERSHIP, List.of("Ownership", "Deliver Results"));
        m.put(Signal.AMBIGUITY, List.of("Bias for Action", "Think Big"));
        m.put(Signal.TECHNICAL_DEPTH, List.of("Dive Deep", "Insist on the Highest Standards"));
        m.put(Signal.DELIVERY, List.of("Deliver Results", "Frugality"));
        m.put(Signal.CONFLICT, List.of("Have Backbone; Disagree and Commit", "Earn Trust"));
        m.put(Signal.INFLUENCE, List.of("Earn Trust", "Invent and Simplify"));
        m.put(Signal.FAILURE, List.of("Learn and Be Curious", "Are Right, A Lot"));
        m.put(Signal.FEEDBACK, List.of("Earn Trust", "Learn and Be Curious"));
        m.put(Signal.MENTORING, List.of("Hire and Develop the Best"));
        m.put(Signal.CUSTOMER_FOCUS, List.of("Customer Obsession"));
        m.put(Signal.PRIORITIZATION, List.of("Bias for Action", "Frugality"));
        m.put(Signal.INCIDENT, List.of("Ownership", "Dive Deep", "Bias for Action"));
        return m;
    }

    static void leadershipPrinciples() {
        System.out.println();
        System.out.println("== 5. Amazon Leadership Principles covered by the bank ==");
        check(AMAZON_LPS.size() == 16, "Amazon lists 16 Leadership Principles");
        Map<Signal, List<String>> mapping = lpMapping();
        Map<String, Integer> storiesPerLp = new LinkedHashMap<>();
        for (String lp : AMAZON_LPS) {
            storiesPerLp.put(lp, 0);
        }
        for (Story story : sampleBank()) {
            Set<String> lps = new java.util.TreeSet<>();
            for (Signal s : story.signals()) {
                lps.addAll(mapping.get(s));
            }
            for (String lp : lps) {
                check(storiesPerLp.containsKey(lp), "mapping uses real LP names: " + lp);
                storiesPerLp.merge(lp, 1, Integer::sum);
            }
        }
        List<String> uncovered = new ArrayList<>();
        for (Map.Entry<String, Integer> e : storiesPerLp.entrySet()) {
            System.out.printf("  %-44s %d%n", e.getKey(), e.getValue());
            if (e.getValue() == 0) {
                uncovered.add(e.getKey());
            }
        }
        System.out.printf("  no story yet for: %s%n", uncovered);
        check(
                uncovered.equals(
                        List.of(
                                "Strive to be Earth's Best Employer",
                                "Success and Scale Bring Broad Responsibility")),
                "the two newest, people-and-impact LPs need their own stories");
    }

    public static void main(String[] args) {
        storyCoverage();
        loopPlanning();
        answerTiming();
        leadershipPrinciples();
        System.out.println();
        System.out.println("All behavioral-preparation checks passed.");
    }
}
