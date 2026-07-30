package com.example.buildlab;

public final class PriceCalculator {
    public long total(long unitPriceInCents, int quantity) {
        if (unitPriceInCents < 0 || quantity < 0) {
            throw new IllegalArgumentException("values must be nonnegative");
        }
        return Math.multiplyExact(unitPriceInCents, quantity);
    }
}
