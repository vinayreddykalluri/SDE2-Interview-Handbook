package com.interviewbook.examples;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class StreamAggregationDemo {
    private StreamAggregationDemo() {}

    record Order(String customer, long cents) {}

    public static Map<String, Long> spendByCustomer(List<Order> orders) {
        return orders.stream().collect(Collectors.groupingBy(
                Order::customer,
                Collectors.summingLong(Order::cents)));
    }

    public static void verify() {
        Map<String, Long> totals = spendByCustomer(List.of(
                new Order("A", 500), new Order("B", 300), new Order("A", 250)));
        if (!totals.equals(Map.of("A", 750L, "B", 300L))) {
            throw new AssertionError(totals);
        }
    }
}
