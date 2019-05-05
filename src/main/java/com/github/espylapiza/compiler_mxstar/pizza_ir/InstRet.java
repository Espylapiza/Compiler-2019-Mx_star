package com.github.espylapiza.compiler_mxstar.pizza_ir;

public final class InstRet extends InstBaseJump {
    public Object obj;

    @Override
    public void accept(PizzaIRPartBaseVisitor visitor) {
        visitor.visit(this);
    }

    public InstRet() {
        super();
    }

    public InstRet(Object obj) {
        super();
        this.obj = obj;
    }

    @Override
    public String toString() {
        if (obj == null) {
            return "ret";
        } else {
            return "ret " + obj;
        }
    }
}
