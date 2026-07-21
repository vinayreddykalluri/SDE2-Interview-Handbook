int discount(int amount) {
    if (amount >= 10000) return 20;
    if (amount >= 5000) return 10;
    return 0;
}
