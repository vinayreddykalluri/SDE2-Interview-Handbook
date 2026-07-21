// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.volume01;

public class JmmDemo {
    private volatile boolean ready;
    private int value;

    public void produce() {
        value = 42;
        ready = true;
    }

    public int consume() {
        if (ready) return value;
        return -1;
    }
}
