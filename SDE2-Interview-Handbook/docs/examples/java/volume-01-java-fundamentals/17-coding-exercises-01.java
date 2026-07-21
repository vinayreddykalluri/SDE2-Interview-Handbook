public final class ExercisePattern {
    public static int solveIfSafe(boolean condition, int value) {
        if (!condition) {
            throw new IllegalArgumentException("Invalid inputs");
        }
        return value;
    }
}
