import java.util.ArrayList;
import java.util.List;

public final class WrappersGenericsEnumsExample {
    enum Difficulty { FOUNDATION, INTERVIEW_CORE, SDE2_FOLLOW_UP }

    record Exercise(String title, Difficulty difficulty) {}

    static <T> T first(List<T> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("empty values");
        }
        return values.get(0);
    }

    public static void main(String[] args) {
        List<Integer> scores = new ArrayList<>();
        scores.add(Integer.parseInt("90"));

        List<Exercise> exercises = List.of(
                new Exercise("Array traversal", Difficulty.FOUNDATION));

        System.out.println(first(scores));
        System.out.println(first(exercises).difficulty());
    }
}
