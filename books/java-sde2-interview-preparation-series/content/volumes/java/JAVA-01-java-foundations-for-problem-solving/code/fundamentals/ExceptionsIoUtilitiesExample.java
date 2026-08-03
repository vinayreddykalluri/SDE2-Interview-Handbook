import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

public final class ExceptionsIoUtilitiesExample {
    static int[] parseLine(String line) {
        if (line == null || line.isBlank()) {
            return new int[0];
        }

        String[] tokens = line.trim().split("\\s+");
        int[] values = new int[tokens.length];
        for (int index = 0; index < tokens.length; index++) {
            try {
                values[index] = Integer.parseInt(tokens[index]);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "invalid integer at token " + index + ": " + tokens[index],
                        exception);
            }
        }
        return values;
    }

    static int[] readValues(String source) throws IOException {
        try (BufferedReader reader =
                     new BufferedReader(new StringReader(source))) {
            return parseLine(reader.readLine());
        }
    }

    public static void main(String[] args) throws IOException {
        int[] values = readValues("30 10 20");
        Arrays.sort(values);
        System.out.println(Arrays.toString(values));

        try {
            readValues("4 nope 6");
        } catch (IllegalArgumentException exception) {
            System.out.println(exception.getMessage());
            System.out.println(exception.getCause().getClass().getSimpleName());
        }
    }
}
