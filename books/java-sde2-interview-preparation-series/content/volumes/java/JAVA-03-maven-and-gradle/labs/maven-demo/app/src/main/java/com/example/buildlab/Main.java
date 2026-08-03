package com.example.buildlab;

public final class Main {
    private Main() {
    }

    public static void main(String[] arguments) {
        PriceCalculator calculator = new PriceCalculator();
        System.out.println("total=" + calculator.total(14L, 3));
    }
}
