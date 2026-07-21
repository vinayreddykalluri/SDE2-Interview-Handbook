int digits(int n) {
    if (n == 0) return 1;
    int c = 0;
    while (n != 0) { n /= 10; c++; }
    return c;
}
