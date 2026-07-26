package com.interviewbook.examples;

public final class SafePublicationDemo {
    private SafePublicationDemo() {}

    static final class Configuration {
        final String endpoint;
        final int timeoutMillis;

        Configuration(String endpoint, int timeoutMillis) {
            this.endpoint = endpoint;
            this.timeoutMillis = timeoutMillis;
        }
    }

    private volatile Configuration configuration;

    public void install(Configuration next) {
        configuration = next;
    }

    public Configuration current() {
        return configuration;
    }

    public static void verify() {
        SafePublicationDemo holder = new SafePublicationDemo();
        holder.install(new Configuration("https://service", 500));
        Configuration visible = holder.current();
        if (visible == null || visible.timeoutMillis != 500) {
            throw new AssertionError("published state not visible");
        }
    }
}
