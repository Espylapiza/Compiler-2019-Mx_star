package com.github.espylapiza.compiler_mxstar.pizza_ir;

public final class TypeString extends TypeFundamental implements Pointer {
    TypeString(String typeName, Class typeClass) {
        super(typeName, typeClass);
    }
}