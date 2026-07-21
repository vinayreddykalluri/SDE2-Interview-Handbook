// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.volume01;

public final class DaysInMonth {
    private DaysInMonth() {
    }

    public static int days(int month, int year) {
        int[] d = {31,28,31,30,31,30,31,31,30,31,30,31};
        if (month == 2 && isLeap(year)) return 29;
        return d[month - 1];
    }

    public static boolean isLeap(int y) { return (y%4==0 && y%100!=0) || y%400==0; }
}
