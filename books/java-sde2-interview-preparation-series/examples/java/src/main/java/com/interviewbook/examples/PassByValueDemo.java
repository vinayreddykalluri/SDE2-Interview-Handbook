package com.interviewbook.examples;

public final class PassByValueDemo {
    private PassByValueDemo() {}

    record Box(int value) {}

    static void replace(Box local) {
        local = new Box(99);
        if (local.value() != 99) {
            throw new AssertionError();
        }
    }

    static void increment(int local) {
        local++;
        if (local != 8) {
            throw new AssertionError();
        }
    }

    public static void verify() {
        Box callerReference = new Box(7);
        replace(callerReference);
        if (callerReference.value() != 7) {
            throw new AssertionError("reference value was not copied");
        }

        int callerPrimitive = 7;
        increment(callerPrimitive);
        if (callerPrimitive != 7) {
            throw new AssertionError("primitive value was not copied");
        }
    }
}
