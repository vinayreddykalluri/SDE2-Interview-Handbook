public final class ObjectOrientedFoundationsExample {
    interface DiscountPolicy {
        int apply(int priceCents);
    }

    static final class FixedDiscount implements DiscountPolicy {
        private final int discountCents;

        FixedDiscount(int discountCents) {
            this.discountCents = discountCents;
        }

        @Override
        public int apply(int priceCents) {
            return Math.max(0, priceCents - discountCents);
        }
    }

    static final class Checkout {
        private final DiscountPolicy policy;

        Checkout(DiscountPolicy policy) {
            this.policy = policy;
        }

        int total(int priceCents) {
            return policy.apply(priceCents);
        }
    }

    public static void main(String[] args) {
        DiscountPolicy policy = new FixedDiscount(250);
        Checkout checkout = new Checkout(policy);
        System.out.println(checkout.total(1_000));
    }
}
