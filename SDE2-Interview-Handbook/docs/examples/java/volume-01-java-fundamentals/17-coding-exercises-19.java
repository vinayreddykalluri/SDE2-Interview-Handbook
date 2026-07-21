boolean validPassword(String s) {
    if (s.length() < 6) return false;
    for (char c : s.toCharArray()) {
        if (Character.isDigit(c)) return true;
    }
    return false;
}
