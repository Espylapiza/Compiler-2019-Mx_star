package com.github.espylapiza.compiler_mxstar.nasm;

import java.util.ArrayList;
import java.util.List;

public class Operands {
    List<Operand> operandList = new ArrayList<Operand>();

    Operands() {
    }

    public Operands(Operand operand) {
        operandList.add(operand);
    }

    public Operands(Operand operand1, Operand operand2) {
        operandList.add(operand1);
        operandList.add(operand2);
    }
}
