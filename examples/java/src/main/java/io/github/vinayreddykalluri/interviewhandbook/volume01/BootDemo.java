// SPDX-License-Identifier: MIT
package io.github.vinayreddykalluri.interviewhandbook.volume01;

public class BootDemo {
    static {
        System.out.println("Loaded before main");
    }

    public static void main(String[] args) {
        BootDemo demo = new BootDemo();
        System.out.println(demo.getClass().getClassLoader());
    }
}
