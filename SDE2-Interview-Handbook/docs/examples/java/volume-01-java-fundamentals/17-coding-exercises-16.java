String status(int fails) {
    return fails >= 5 ? "LOCKED" : "OPEN";
}
