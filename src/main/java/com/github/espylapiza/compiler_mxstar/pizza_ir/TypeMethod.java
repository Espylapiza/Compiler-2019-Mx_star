package com.github.espylapiza.compiler_mxstar.pizza_ir;

public class TypeMethod extends TypeSingle {
    public TypeMethod(String typeName, Class typeClass) {
        super(typeName, typeClass);
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TypeMethod)) {
            return false;
        }
        return false;
    }
}