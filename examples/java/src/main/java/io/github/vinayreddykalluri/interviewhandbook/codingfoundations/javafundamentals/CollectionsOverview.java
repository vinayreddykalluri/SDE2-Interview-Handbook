// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.codingfoundations.javafundamentals;

import java.util.*;

public class CollectionsOverview {
    public static void main(String[] args) {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("apple", 3);
        counts.merge("apple", 1, Integer::sum);
        System.out.println(counts);
    }
}
