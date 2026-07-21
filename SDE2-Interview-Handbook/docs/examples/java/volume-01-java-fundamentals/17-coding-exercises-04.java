String type(char c) {
    if (Character.isLetter(c)) return "Letter";
    if (Character.isDigit(c)) return "Digit";
    return "Other";
}
