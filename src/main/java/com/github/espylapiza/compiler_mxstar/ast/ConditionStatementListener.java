package com.github.espylapiza.compiler_mxstar.ast;

import com.github.espylapiza.compiler_mxstar.parser.*;

public class ConditionStatementListener extends Mx_starBaseListener {
    @Override
    public void enterConditionStatement(Mx_starParser.ConditionStatementContext ctx) {
        ObjectListener objLser = new ObjectListener();
        ctx.object().enterRule(objLser);

        if (!objLser.type.equals("bool")) {
            assert false;
        }

        ctx.statement().forEach(ch -> {
            PizzaIRBuilder.dom.enterCondition(-1);
            PizzaIRBuilder.code.packScope();

            StatementListener lser = new StatementListener();
            ch.enterRule(lser);

            PizzaIRBuilder.dom.exitCondition();
        });

        PizzaIRBuilder.code.packScope();
    }
}