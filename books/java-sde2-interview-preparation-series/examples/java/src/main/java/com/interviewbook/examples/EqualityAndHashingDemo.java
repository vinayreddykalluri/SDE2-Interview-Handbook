package com.interviewbook.examples;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class EqualityAndHashingDemo {
    private EqualityAndHashingDemo() {}

    record UserId(String value) {
        UserId {
            value = Objects.requireNonNull(value).strip();
            if (value.isEmpty()) {
                throw new IllegalArgumentException("empty id");
            }
        }
    }

    public static void verify() {
        Map<UserId, String> owners = new HashMap<>();
        owners.put(new UserId("u-42"), "Ada");
        String found = owners.get(new UserId("u-42"));
        if (!"Ada".equals(found)) {
            throw new AssertionError("equal records must locate the same mapping");
        }
    }
}
