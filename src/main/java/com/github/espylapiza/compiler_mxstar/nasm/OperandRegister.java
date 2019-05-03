package com.github.espylapiza.compiler_mxstar.nasm;

public abstract class OperandRegister extends Operand {
    String name;

    OperandRegister(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}