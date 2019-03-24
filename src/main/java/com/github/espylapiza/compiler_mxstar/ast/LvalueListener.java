package com.github.espylapiza.compiler_mxstar.ast;

import com.github.espylapiza.compiler_mxstar.parser.*;

public class LvalueListener extends Mx_starBaseListener {
    String name, type;

    @Override
    public void enterIdentifierLvalue(Mx_starParser.IdentifierLvalueContext ctx) {
        name = ctx.Identifier().getText();

        if (PizzaIRVisitor.dom.hasVar(name)) {
            type = PizzaIRVisitor.dom.getVarType(name);
        } else {
            assert false;
        }
    }

    @Override
    public void enterMemberLvalue(Mx_starParser.MemberLvalueContext ctx) {
        String identifier_typename;

        if (ctx.This() != null) {
            if (PizzaIRVisitor.dom.isGlobal()) {
                assert false;
            }
            identifier_typename = PizzaIRVisitor.dom.getClassTrace();
        } else {
            LvalueListener lser = new LvalueListener();
            ctx.lvalue().enterRule(lser);

            identifier_typename = lser.type;
        }

        name = ctx.Identifier().getText();

        Type identifier_type = PizzaIRVisitor.typeList.getType(identifier_typename);

        if (identifier_type.hasMember(name)) {
            type = identifier_type.getVarType(name);
        } else {
            assert false;
        }
    }

    @Override
    public void enterSubscriptLvalue(Mx_starParser.SubscriptLvalueContext ctx) {
        LvalueListener lvalueLser = new LvalueListener();
        ctx.lvalue().enterRule(lvalueLser);

        name = lvalueLser.name;
        type = lvalueLser.type;
        if (!type.endsWith("[]")) {
            assert false;
        }

        ObjectListener objLser = new ObjectListener();
        ctx.object().enterRule(objLser);
        if (!objLser.type.equals("int")) {
            assert false;
        }

        type = type.substring(0, type.length() - 2);
    }
}