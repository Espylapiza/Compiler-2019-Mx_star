package com.github.espylapiza.compiler_mxstar.nasm;

public class Label {
    private final String name;

    public Label(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}