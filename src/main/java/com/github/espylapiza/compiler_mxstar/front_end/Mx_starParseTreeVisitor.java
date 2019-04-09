package com.github.espylapiza.compiler_mxstar.front_end;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import com.github.espylapiza.compiler_mxstar.parser.Mx_starBaseVisitor;
import com.github.espylapiza.compiler_mxstar.parser.Mx_starParser;
import com.github.espylapiza.compiler_mxstar.parser.Mx_starParser.ConstantContext;
import com.github.espylapiza.compiler_mxstar.parser.Mx_starParser.StatementContext;
import com.github.espylapiza.compiler_mxstar.pizza_ir.ObjectBool;
import com.github.espylapiza.compiler_mxstar.pizza_ir.Class;
import com.github.espylapiza.compiler_mxstar.pizza_ir.Domain;
import com.github.espylapiza.compiler_mxstar.pizza_ir.Func;
import com.github.espylapiza.compiler_mxstar.pizza_ir.FuncAddr;
import com.github.espylapiza.compiler_mxstar.pizza_ir.FuncExtra;
import com.github.espylapiza.compiler_mxstar.pizza_ir.InstAssignment;
import com.github.espylapiza.compiler_mxstar.pizza_ir.InstBr;
import com.github.espylapiza.compiler_mxstar.pizza_ir.InstCall;
import com.github.espylapiza.compiler_mxstar.pizza_ir.InstJump;
import com.github.espylapiza.compiler_mxstar.pizza_ir.InstLoad;
import com.github.espylapiza.compiler_mxstar.pizza_ir.InstMember;
import com.github.espylapiza.compiler_mxstar.pizza_ir.InstRet;
import com.github.espylapiza.compiler_mxstar.pizza_ir.InstStore;
import com.github.espylapiza.compiler_mxstar.pizza_ir.ObjectInt;
import com.github.espylapiza.compiler_mxstar.pizza_ir.DomainLoop;
import com.github.espylapiza.compiler_mxstar.pizza_ir.NullComparable;
import com.github.espylapiza.compiler_mxstar.pizza_ir.ObjectNull;
import com.github.espylapiza.compiler_mxstar.pizza_ir.Object;
import com.github.espylapiza.compiler_mxstar.pizza_ir.ParamList;
import com.github.espylapiza.compiler_mxstar.pizza_ir.PizzaIR;
import com.github.espylapiza.compiler_mxstar.pizza_ir.ProgramFragment;
import com.github.espylapiza.compiler_mxstar.pizza_ir.Scope;
import com.github.espylapiza.compiler_mxstar.pizza_ir.ScopeType;
import com.github.espylapiza.compiler_mxstar.pizza_ir.ObjectString;
import com.github.espylapiza.compiler_mxstar.pizza_ir.Type;
import com.github.espylapiza.compiler_mxstar.pizza_ir.TypeArray;
import com.github.espylapiza.compiler_mxstar.pizza_ir.TypeBool;
import com.github.espylapiza.compiler_mxstar.pizza_ir.TypeCustomClass;
import com.github.espylapiza.compiler_mxstar.pizza_ir.TypeFunc;
import com.github.espylapiza.compiler_mxstar.pizza_ir.TypeInt;
import com.github.espylapiza.compiler_mxstar.pizza_ir.TypeMethod;
import com.github.espylapiza.compiler_mxstar.pizza_ir.TypeNull;
import com.github.espylapiza.compiler_mxstar.pizza_ir.TypeVoid;

import org.antlr.v4.runtime.tree.ParseTree;

enum VisitState {
    TYPE_DECLARATION, DECLARATION, SEMANTIC_ANALYSIS
}

class Mx_starParseTreeVisitor extends Mx_starBaseVisitor<ProgramFragment> {
    private final static Logger LOGGER = Logger.getLogger(Mx_starParseTreeVisitor.class.getName());

    private PizzaIR ir;

    private VisitState state;
    private ScopeManager manager = new ScopeManager();
    private DomainTrace trace = new DomainTrace();

    Mx_starParseTreeVisitor(PizzaIR ir) {
        this.ir = ir;
    }

    @Override
    public ProgramFragment visitProgram(Mx_starParser.ProgramContext ctx) {
        LOGGER.info("TYPE_DECLARATION");
        state = VisitState.TYPE_DECLARATION;
        ctx.programSection().forEach(ch -> ch.accept(this));

        LOGGER.info("DECLARATION");
        state = VisitState.DECLARATION;
        ctx.programSection().forEach(ch -> ch.accept(this));

        Func mainFunc = ir.funcList.get(new FuncAddr().add("main"));
        if (mainFunc == null || !(mainFunc.getRtype() instanceof TypeInt) || mainFunc.getParams().count() != 0) {
            assert false;
        }

        manager.enter((FuncExtra) ir.funcList.get(new FuncAddr().add("__init__")));
        manager.pushScope(manager.newScope(ScopeType.FUNC));

        LOGGER.info("SEMANTIC_ANALYSIS");
        state = VisitState.SEMANTIC_ANALYSIS;
        ctx.programSection().forEach(ch -> ch.accept(this));

        manager.popScope();
        manager.exit();

        return null;
    }

    ////////////////////////////// Program Section //////////////////////////////

    @Override
    public ProgramFragment visitProgramVariableDeclarationStatement(
            Mx_starParser.ProgramVariableDeclarationStatementContext ctx) {
        switch (state) {
        case TYPE_DECLARATION:
            break;
        case DECLARATION:
            break;
        case SEMANTIC_ANALYSIS:
            visit(ctx.variableDeclarationStatement());
            break;
        }
        return null;
    }

    @Override
    public ProgramFragment visitProgramVariableDefinitionStatement(
            Mx_starParser.ProgramVariableDefinitionStatementContext ctx) {
        switch (state) {
        case TYPE_DECLARATION:
            break;
        case DECLARATION:
            break;
        case SEMANTIC_ANALYSIS:
            visit(ctx.variableDefinitionStatement());
            break;
        }
        return null;
    }

    @Override
    public ProgramFragment visitProgramClassDefinitionStatement(
            Mx_starParser.ProgramClassDefinitionStatementContext ctx) {
        String name = ctx.classDefinitionStatement().Identifier().getText();

        switch (state) {
        case TYPE_DECLARATION:
            Class class1 = new Class(name);
            ir.classList.add(class1);
            ir.typeTable.add(new TypeCustomClass(name, class1));
            break;
        case DECLARATION:
        case SEMANTIC_ANALYSIS:
            trace.enter(ir.classList.get(name));
            ctx.classDefinitionStatement().classMember().forEach(ch -> ch.accept(this));
            trace.exit();
            break;
        }

        return null;
    }

    @Override
    public ProgramFragment visitProgramFunctionDefinitionStatement(
            Mx_starParser.ProgramFunctionDefinitionStatementContext ctx) {
        switch (state) {
        case TYPE_DECLARATION:
            break;
        case DECLARATION:
            Func func = (Func) visit(ctx.functionDefinitionStatement());
            ir.funcList.addFunc(func);
            break;
        case SEMANTIC_ANALYSIS:
            visit(ctx.functionDefinitionStatement());
            break;
        }
        return null;
    }

    ////////////////////////////// Class //////////////////////////////

    @Override
    public ProgramFragment visitClassVariableDeclarationStatement(
            Mx_starParser.ClassVariableDeclarationStatementContext ctx) {
        switch (state) {
        case TYPE_DECLARATION:
            break;
        case DECLARATION:
            String name = ctx.variableDeclarationStatement().variableDeclaration().Identifier().getText();
            Type type = getTypeByName(ctx.variableDeclarationStatement().variableDeclaration().type().getText());
            trace.getCurrentClass().addVariable(name, type);
            break;
        case SEMANTIC_ANALYSIS:
            visit(ctx.variableDeclarationStatement());
            break;
        }
        return null;
    }

    @Override
    public ProgramFragment visitClassConstructionFunctionStatement(
            Mx_starParser.ClassConstructionFunctionStatementContext ctx) {
        Func func = (Func) visit(ctx.constructionFunctionStatement());

        if (state == VisitState.DECLARATION) {
            String name = func.getName();

            if (!trace.getCurrentClass().getName().equals(name)) {
                assert false;
            }

            trace.getCurrentClass().addMethod(func);
            ir.funcList.addFunc(func);
        }
        return null;
    }

    @Override
    public ProgramFragment visitClassFunctionDefinitionStatement(
            Mx_starParser.ClassFunctionDefinitionStatementContext ctx) {
        Func func = (Func) visit(ctx.functionDefinitionStatement());

        if (state == VisitState.DECLARATION) {
            String name = func.getName();

            if (trace.getCurrentClass().getName().equals(name)) {
                assert false;
            }

            trace.getCurrentClass().addMethod(func);
            ir.funcList.addFunc(func);
        }
        return null;
    }

    ////////////////////////////// Function //////////////////////////////

    @Override
    public ProgramFragment visitConstructionFunctionStatement(Mx_starParser.ConstructionFunctionStatementContext ctx) {
        Class owner = trace.getCurrentClass();
        String name = ctx.Identifier().getText();
        Type rtype = getTypeByName("void");
        FuncAddr addr = new FuncAddr().addClass(owner).add(name);

        FuncExtra func;
        if (state == VisitState.DECLARATION) {
            func = new FuncExtra(addr, name, rtype);
        } else {
            func = (FuncExtra) ir.funcList.get(addr);
        }

        LOGGER.fine("enter construction function: " + name);
        trace.enter(func);

        defineVar(allocateVariable(new Object(owner, "this", getTypeByName(trace.getClass().getName()))));

        ParamList params = (ParamList) visit(ctx.paramListDefinition());

        if (state == VisitState.DECLARATION) {
            func.setParams(params);
        } else {
            manager.enter(func);
            manager.pushScope(manager.newScope(ScopeType.FUNC));

            if (ctx.statements() != null) {
                visit(ctx.statements());
            }

            manager.popScope();
            manager.exit();
        }

        trace.exit();
        LOGGER.fine("exit construction function: " + name);

        return func;
    }

    @Override
    public ProgramFragment visitFunctionDefinitionStatement(Mx_starParser.FunctionDefinitionStatementContext ctx) {
        Class owner = trace.getCurrentClass();
        String name = ctx.Identifier().getText();
        Type rtype = getTypeByName(ctx.type().getText());
        FuncAddr addr = new FuncAddr().addClass(owner).add(name);

        FuncExtra func;
        if (state == VisitState.DECLARATION) {
            func = new FuncExtra(addr, name, rtype);
        } else {
            func = (FuncExtra) ir.funcList.get(addr);
        }

        LOGGER.fine("enter function: " + name);
        trace.enter(func);

        if (owner != null) {
            defineVar(allocateVariable(new Object(owner, "this", getTypeByName(trace.getClass().getName()))));
        }

        ParamList params = (ParamList) visit(ctx.paramListDefinition());

        if (state == VisitState.DECLARATION) {
            func.setParams(params);
        } else {
            manager.enter(func);
            manager.pushScope(manager.newScope(ScopeType.FUNC));

            if (ctx.statements() != null) {
                visit(ctx.statements());
            }

            manager.popScope();
            manager.exit();
        }

        trace.exit();
        LOGGER.fine("exit function: " + name);
        return func;
    }

    @Override
    public ProgramFragment visitParamListDefinition(Mx_starParser.ParamListDefinitionContext ctx) {
        ParamList params = new ParamList();
        ctx.variableDeclaration().forEach(member -> {
            Object variable = (Object) visit(member);
            params.add(variable.type);
        });
        return params;
    }

    @Override
    public ProgramFragment visitParamList(Mx_starParser.ParamListContext ctx) {
        ParamList params = new ParamList();
        ctx.object().forEach(member -> {
            Object variable = (Object) visit(member);
            params.add(variable.type);
        });
        return params;
    }

    ////////////////////////////// Statement //////////////////////////////
    // SEMANTIC

    @Override
    public ProgramFragment visitStatements(Mx_starParser.StatementsContext ctx) {
        ctx.statement().forEach(ch -> visit(ch));
        return null;
    }

    // @Override
    // public Node visitStmtEmptyStatement(Mx_starParser.StmtEmptyStatementContext ctx) {
    //     return null;
    // }

    @Override
    public ProgramFragment visitStmtVariableDeclarationStatement(
            Mx_starParser.StmtVariableDeclarationStatementContext ctx) {
        return visit(ctx.variableDeclarationStatement());
    }

    @Override
    public ProgramFragment visitStmtVariableDefinitionStatement(
            Mx_starParser.StmtVariableDefinitionStatementContext ctx) {
        return visit(ctx.variableDefinitionStatement());
    }

    @Override
    public ProgramFragment visitVariableAssignmentStatement(Mx_starParser.VariableAssignmentStatementContext ctx) {
        return visit(ctx.variableAssignment());
    }

    @Override
    public ProgramFragment visitStmtVariableAssignmentStatement(
            Mx_starParser.StmtVariableAssignmentStatementContext ctx) {
        return visit(ctx.variableAssignmentStatement());
    }

    @Override
    public ProgramFragment visitStmtObjectStatement(Mx_starParser.StmtObjectStatementContext ctx) {
        return visit(ctx.objectStatement());
    }

    @Override
    public ProgramFragment visitStmtLoopStatement(Mx_starParser.StmtLoopStatementContext ctx) {
        return visit(ctx.loopStatement());
    }

    @Override
    public ProgramFragment visitStmtConditionStatement(Mx_starParser.StmtConditionStatementContext ctx) {
        return visit(ctx.conditionStatement());
    }

    @Override
    public ProgramFragment visitStmtJumpStatement(Mx_starParser.StmtJumpStatementContext ctx) {
        return visit(ctx.jumpStatement());
    }

    @Override
    public ProgramFragment visitStmtCompoundStatement(Mx_starParser.StmtCompoundStatementContext ctx) {
        return visit(ctx.compoundStatement());
    }

    @Override
    public ProgramFragment visitJumpReturn(Mx_starParser.JumpReturnContext ctx) {
        if (ctx.object() == null) {
            // return
            if (!(trace.getRtype() instanceof TypeVoid)) {
                assert false;
                return null;
            }
            manager.addInstruction(new InstRet());
        } else {
            Object obj = (Object) visit(ctx.object());

            Type rtype = trace.getRtype();
            if (obj.type instanceof TypeNull) {
                // return null
                if (!(rtype instanceof NullComparable)) {
                    assert false;
                    return null;
                }
                manager.addInstruction(new InstRet(obj));
            } else {
                // return object
                if (!obj.type.equals(rtype)) {
                    assert false;
                    return null;
                }
                manager.addInstruction(new InstRet(obj));
            }
        }
        return null;
    }

    @Override
    public ProgramFragment visitJumpBreak(Mx_starParser.JumpBreakContext ctx) {
        if (!trace.inLoop()) {
            assert false;
        }
        manager.jumpBreak();
        return null;
    }

    @Override
    public ProgramFragment visitJumpContinue(Mx_starParser.JumpContinueContext ctx) {
        if (!trace.inLoop()) {
            assert false;
        }
        manager.jumpContinue();
        return null;
    }

    @Override
    public ProgramFragment visitWhileLoop(Mx_starParser.WhileLoopContext ctx) {
        trace.enter(new DomainLoop());

        Scope scpLoop, scpLoopBody, scpEndLoop;

        scpLoop = manager.newScope(ScopeType.LOOP);
        scpLoopBody = manager.newScope(ScopeType.LOOPBODY);
        scpEndLoop = manager.newScope(ScopeType.ENDLOOP);

        manager.addInstruction(new InstJump(scpLoop));
        manager.popScope();
        manager.pushScope(scpEndLoop);
        manager.pushScope(scpLoop);

        Object obj = (Object) visit(ctx.object());

        if (!(obj.type instanceof TypeBool)) {
            assert false;
        }

        manager.addInstruction(new InstBr(obj, scpLoop, scpEndLoop));

        manager.popScope();
        manager.pushScope(scpLoopBody);

        visit(ctx.statement());

        manager.addInstruction(new InstJump(scpLoop));
        manager.popScope();

        trace.exit();

        return null;
    }

    @Override
    public ProgramFragment visitForLoop(Mx_starParser.ForLoopContext ctx) {
        trace.enter(new DomainLoop());

        if (ctx.forCondition().forCondition1() != null) {
            visit(ctx.forCondition().forCondition1());
        }

        Scope scpLoop, scpLoopBody, scpEndLoop;

        scpLoop = manager.newScope(ScopeType.LOOP);
        scpLoopBody = manager.newScope(ScopeType.LOOPBODY);
        scpEndLoop = manager.newScope(ScopeType.ENDLOOP);

        manager.addInstruction(new InstJump(scpLoop));
        manager.popScope();
        manager.pushScope(scpEndLoop);
        manager.pushScope(scpLoop);

        if (ctx.forCondition().forCondition2() != null) {
            Object obj = (Object) visit(ctx.forCondition().forCondition2().object());
            if (!(obj.type instanceof TypeBool)) {
                assert false;
            }
            manager.addInstruction(new InstBr(obj, scpLoop, scpEndLoop));
        } else {
            manager.addInstruction(new InstJump(scpLoop));
        }

        manager.popScope();
        manager.pushScope(scpLoopBody);

        visit(ctx.statement());

        if (ctx.forCondition().forCondition3() != null) {
            visit(ctx.forCondition().forCondition3());
        }

        manager.addInstruction(new InstJump(scpLoop));
        manager.popScope();

        trace.exit();

        return null;
    }

    @Override
    public ProgramFragment visitForCdt1VariableDeclaration(Mx_starParser.ForCdt1VariableDeclarationContext ctx) {
        return visit(ctx.variableDeclaration());
    }

    @Override
    public ProgramFragment visitForCdt1VariableDefinition(Mx_starParser.ForCdt1VariableDefinitionContext ctx) {
        return visit(ctx.variableDefinition());
    }

    @Override
    public ProgramFragment visitForCdt1VariableAssignment(Mx_starParser.ForCdt1VariableAssignmentContext ctx) {
        return visit(ctx.variableAssignment());
    }

    @Override
    public ProgramFragment visitForCdt1Object(Mx_starParser.ForCdt1ObjectContext ctx) {
        return visit(ctx.object());
    }

    @Override
    public ProgramFragment visitForCdt3VariableAssignment(Mx_starParser.ForCdt3VariableAssignmentContext ctx) {
        return visit(ctx.variableAssignment());
    }

    @Override
    public ProgramFragment visitForCdt3Object(Mx_starParser.ForCdt3ObjectContext ctx) {
        return visit(ctx.object());
    }

    @Override
    public ProgramFragment visitConditionStatement(Mx_starParser.ConditionStatementContext ctx) {
        Object obj = (Object) visit(ctx.object());

        if (!(obj.type instanceof TypeBool)) {
            assert false;
        }

        Scope scpIfTrue, scpIfFalse, scpEndIf;

        scpIfTrue = manager.newScope(ScopeType.IF);
        scpEndIf = manager.newScope(ScopeType.ENDIF);

        if (ctx.else_stmt != null) {
            scpIfFalse = manager.newScope(ScopeType.ELSE);
        } else {
            scpIfFalse = scpEndIf;
        }

        manager.addInstruction(new InstBr(obj, scpIfTrue, scpIfFalse));
        manager.popScope();

        manager.pushScope(scpEndIf);

        for (StatementContext stmt : ctx.statement()) {
            trace.enter(new Domain());
            if (stmt == ctx.if_stmt) {
                manager.pushScope(scpIfTrue);
            } else {
                manager.pushScope(scpIfFalse);
            }
            visit(stmt);
            manager.popScope();
            trace.exit();
        }

        return null;
    }

    @Override
    public ProgramFragment visitObjectStatement(Mx_starParser.ObjectStatementContext ctx) {
        visit(ctx.object());
        return null;
    }

    @Override
    public ProgramFragment visitVariableDeclarationStatement(Mx_starParser.VariableDeclarationStatementContext ctx) {
        visit(ctx.variableDeclaration());
        return null;
    }

    @Override
    public ProgramFragment visitVariableDefinitionStatement(Mx_starParser.VariableDefinitionStatementContext ctx) {
        visit(ctx.variableDefinition());
        return null;
    }

    public ProgramFragment visitCompoundStatement(Mx_starParser.CompoundStatementContext ctx) {
        if (ctx.statements() != null) {
            trace.enter(new Domain());

            visit(ctx.statements());

            trace.exit();
        }
        return null;
    }

    @Override
    public ProgramFragment visitVariableDeclaration(Mx_starParser.VariableDeclarationContext ctx) {
        String name = ctx.Identifier().getText();
        Type type = getTypeByName(ctx.type().getText());

        if (type == null || type instanceof TypeVoid) {
            assert false;
            return null;
        }

        Object obj = allocateVariable(new Object(trace.getCurrentClass(), name, type));
        defineVar(obj);

        return obj;
    }

    @Override
    public ProgramFragment visitVariableDefinition(Mx_starParser.VariableDefinitionContext ctx) {
        Object obj = (Object) visit(ctx.object());

        String name = ctx.Identifier().getText();
        Type type = getTypeByName(ctx.type().getText());

        if (type == null || type instanceof TypeVoid) {
            assert false;
            return null;
        }

        if (obj.type instanceof TypeNull) {
            if (!(type instanceof NullComparable)) {
                assert false;
                return null;
            }
        } else {
            if (!type.equals(obj.type)) {
                assert false;
                return null;
            }
        }

        Object src;
        if (obj.name != null) {
            src = allocateVariable(new Object(trace.getCurrentClass(), name, type));
            manager.addInstruction(new InstAssignment(src, obj));
        } else {
            src = obj;
            src.type = type;
        }

        Object dst = allocateVariable(new Object(trace.getCurrentClass(), name, type));
        defineVar(dst);

        manager.addInstruction(new InstStore(dst, src));

        return null;
    }

    @Override
    public ProgramFragment visitVariableAssignment(Mx_starParser.VariableAssignmentContext ctx) {
        Object obj = (Object) visit(ctx.object());
        Object dst = (Object) visit(ctx.lvalue());

        Type type = dst.type;

        if (obj.type instanceof TypeNull) {
            if (!(type instanceof NullComparable)) {
                assert false;
                return null;
            }
        } else {
            if (!type.equals(obj.type)) {
                assert false;
                return null;
            }
        }

        Object src;
        if (obj.name != null) {
            src = allocateVariable(new Object(trace.getCurrentClass(), null, type));
            manager.addInstruction(new InstAssignment(src, obj));
        } else {
            src = obj;
        }

        manager.addInstruction(new InstStore(dst, src));

        return null;
    }

    ////////////////////////////// Object //////////////////////////////

    @Override
    public ProgramFragment visitIdentifierLvalue(Mx_starParser.IdentifierLvalueContext ctx) {
        String name = ctx.Identifier().getText();

        Object variable = (Object) trace.getVar(name);

        if (variable != null && variable.owner == trace.getCurrentClass()) {
            // local variable
            return variable;
        }

        if (!trace.isGlobal()) {
            Class class1 = trace.getCurrentClass();
            if (class1.hasVariable(name)) {
                // member variable
                Object dst = new Object(class1, null, class1.getVarType(name));
                manager.addInstruction(new InstMember(dst, variable, name));
                return dst;
            }
            if (class1.hasMethod(name)) {
                assert false;
                return null;
            }
        }

        if (variable != null) {
            // global variable
            return new Object(null, name, variable.type);
        }

        assert false;
        return null;
    }

    @Override
    public ProgramFragment visitMemberLvalue(Mx_starParser.MemberLvalueContext ctx) {
        // Type identifier_type;
        Class identifierClass;

        if (ctx.This() != null) {
            if (trace.isGlobal()) {
                assert false;
                return null;
            }
            identifierClass = trace.getCurrentClass();
        } else {
            Object node = (Object) visit(ctx.lvalue());
            identifierClass = node.type.getTypeClass();
        }

        String name = ctx.Identifier().getText();

        if (!identifierClass.hasVariable(name)) {
            assert false;
            return null;
        }

        Type type = identifierClass.getVarType(name);

        return new Object(null, null, type);
    }

    @Override
    public ProgramFragment visitSubscriptLvalue(Mx_starParser.SubscriptLvalueContext ctx) {
        Object array = (Object) visit(ctx.array);

        if (!(array.type instanceof TypeArray)) {
            assert false;
            return null;
        }
        TypeArray arrayType = (TypeArray) array.type;
        Type type = arrayType.getSubType();

        Object sub = (Object) visit(ctx.subscript);

        if (!(sub.type instanceof TypeInt)) {
            assert false;
            return null;
        }

        return new Object(null, null, type);
    }

    @Override
    public ProgramFragment visitThisObject(Mx_starParser.ThisObjectContext ctx) {
        if (trace.isGlobal()) {
            assert false;
            return null;
        }
        return trace.getVar("this");
    }

    @Override
    public ProgramFragment visitIdentifierObject(Mx_starParser.IdentifierObjectContext ctx) {
        String name = ctx.Identifier().getText();

        Object obj = trace.getVar(name);

        if (obj != null && obj.owner == trace.getCurrentClass()) {
            // local variable
            return obj;
        }

        if (!trace.isGlobal()) {
            Class class1 = trace.getCurrentClass();
            if (class1.hasVariable(name)) {
                // member variable
                return new Object(class1, name, class1.getVarType(name));
            }
            if (class1.hasMethod(name)) {
                // member method
                return new Object(class1, name, getTypeByName("__method__"));
            }
        }

        if (obj != null) {
            // global variable
            return obj;
        }

        Func func = ir.funcList.get(new FuncAddr().add(name));
        if (func != null) {
            // global function
            return new Object(null, name, getTypeByName("__func__"));
        }

        assert false;
        return null;
    }

    @Override
    public ProgramFragment visitNewObject(Mx_starParser.NewObjectContext ctx) {
        int cntLeftBracket = 0;
        int cntBracket = 0;

        for (ParseTree ch : ctx.children) {
            if (ch.getText().equals("[")) {
                cntLeftBracket++;
                cntBracket++;
            } else if (ch instanceof Mx_starParser.ObjectContext) {
                cntBracket--;

                if (cntBracket > 0) {
                    assert false;
                    return null;
                }

                Object node = (Object) visit(ch);

                if (!(node.type instanceof TypeInt)) {
                    assert false;
                    return null;
                }
            }
        }

        Type type = getTypeByName(ctx.type().getText());

        if (type instanceof TypeVoid) {
            assert false;
            return null;
        }

        for (int i = 0; i < cntLeftBracket; i++) {
            type = new TypeArray(type, ir.classList.get("__array__"));
        }

        // TODO
        Object ret = allocateVariable(new Object(trace.getCurrentClass(), null, type));
        // code.addInstruction(Instruction.newCall(id, func.getAddr(), new Vector<Integer>(Arrays.asList(objId))));

        return ret;
    }

    @Override
    public ProgramFragment visitConstantObject(Mx_starParser.ConstantObjectContext ctx) {
        Object obj;

        ConstantContext constant = ctx.constant();
        if (constant instanceof Mx_starParser.NullContext) {
            obj = new ObjectNull(trace.getCurrentClass(), null, getTypeByName("null"));
        } else if (constant instanceof Mx_starParser.LogicalConstantContext) {
            Boolean value = constant.getText().equals("true");
            obj = new ObjectBool(trace.getCurrentClass(), null, getTypeByName("bool"), value);
        } else if (constant instanceof Mx_starParser.IntegerConstantContext) {
            Integer value = Integer.parseInt(constant.getText());
            obj = new ObjectInt(trace.getCurrentClass(), null, getTypeByName("int"), value);
        } else if (constant instanceof Mx_starParser.StringLiteralContext) {
            String value = constant.getText();
            obj = new ObjectString(trace.getCurrentClass(), null, getTypeByName("string"), value);
        } else {
            assert false;
            return null;
        }
        return obj;
    }

    @Override
    public ProgramFragment visitLvalueObject(Mx_starParser.LvalueObjectContext ctx) {
        Object obj = (Object) visit(ctx.lvalue());
        Object ret = new Object(trace.getCurrentClass(), obj.name, obj.type);
        return ret;
    }

    @Override
    public ProgramFragment visitMemberObject(Mx_starParser.MemberObjectContext ctx) {
        Class owner = null;
        String member = ctx.Identifier().getText();
        Type type;

        Object node = (Object) visit(ctx.object());

        Class class1 = getTypeByName(node.type.getName()).getTypeClass();

        if (class1.hasVariable(member)) {
            type = class1.getVarType(member);

            // TODO
            return allocateVariable(new Object(owner, null, type));
        }

        if (class1.hasMethod(member)) {
            type = getTypeByName("__method__");
            if (node.type instanceof TypeArray) {
                owner = ir.classList.get("__array__");
            } else {
                owner = node.type.getTypeClass();
            }

            return new Object(owner, member, type);
        }

        assert false;
        return null;
    }

    @Override
    public ProgramFragment visitBracketObject(Mx_starParser.BracketObjectContext ctx) {
        return (Object) visit(ctx.object());
    }

    @Override
    public ProgramFragment visitFunctionReturnObject(Mx_starParser.FunctionReturnObjectContext ctx) {
        Object obj = (Object) visit(ctx.object());

        if (!(obj.type instanceof TypeFunc) && !(obj.type instanceof TypeMethod)) {
            assert false;
            return null;
        }

        FuncAddr addr = new FuncAddr().addClass(obj.owner).add(obj.name);

        Func func = ir.funcList.get(addr);

        ParamList params = (ParamList) visit(ctx.paramList());

        if (!func.getParams().match(params)) {
            assert false;
            return null;
        }

        Type rtype = func.getRtype();

        if (rtype instanceof TypeVoid) {
            return null;
        }

        Object ret = allocateVariable(new Object(trace.getCurrentClass(), null, rtype));
        // code.addInstruction(new InstLoad(id, arrayNode.id, subNode.id));
        return ret;
    }

    @Override
    public ProgramFragment visitSubscriptObject(Mx_starParser.SubscriptObjectContext ctx) {
        Object array = (Object) visit(ctx.array);

        Type type = array.type;
        if (!(type instanceof TypeArray)) {
            assert false;
            return null;
        } else {
            type = ((TypeArray) type).getSubType();
        }

        Object sub = (Object) visit(ctx.subscript);
        if (!(sub.type instanceof TypeInt)) {
            assert false;
            return null;
        }

        Object ret = allocateVariable(new Object(trace.getCurrentClass(), null, type));
        manager.addInstruction(new InstLoad(ret, array, sub));

        return ret;
    }

    @Override
    public ProgramFragment visitUnaryOperatorObject(Mx_starParser.UnaryOperatorObjectContext ctx) {
        String op = ctx.op.getText();
        String method = null;

        switch (op) {
        case "++":
            method = "__preinc__";
            break;
        case "--":
            method = "__predec__";
            break;
        case "+":
            method = "__pos__";
            break;
        case "-":
            method = "__neg__";
            break;
        case "!":
            method = "__lgcnot__";
            break;
        case "~":
            method = "__bitinv__";
            break;
        default:
            assert false;
            return null;
        }

        Type objType = null;
        Object node;

        if (ctx.lvalue() != null) {
            node = (Object) visit(ctx.lvalue());
            objType = node.type;
            // TODO
        } else {
            node = (Object) visit(ctx.object());
            objType = node.type;
        }

        Func func = objType.getTypeClass().getMethod(method);

        if (func == null) {
            assert false;
            return null;
        }

        ParamList params = new ParamList();

        if (!func.getParams().match(params)) {
            assert false;
            return null;
        }

        Type rtype = func.getRtype();

        Object ret = allocateVariable(new Object(trace.getCurrentClass(), null, rtype));
        manager.addInstruction(new InstCall(ret, func.getAddr(), new ArrayList<Object>(Arrays.asList(node))));

        return ret;
    }

    @Override
    public ProgramFragment visitBinaryOperatorObject(Mx_starParser.BinaryOperatorObjectContext ctx) {
        Class owner = null;
        String name = null;
        Type type;
        Object lhs = (Object) visit(ctx.object(0));
        Type typel = lhs.type;

        Object rhs = (Object) visit(ctx.object(1));
        Type typer = rhs.type;

        String op = ctx.op.getText();
        String method = "";
        switch (op) {
        case "*":
            method = "__mul__";
            break;
        case "/":
            method = "__div__";
            break;
        case "%":
            method = "__mod__";
            break;
        case "+":
            method = "__add__";
            break;
        case "-":
            method = "__sub__";
            break;
        case "<<":
            method = "__shl__";
            break;
        case ">>":
            method = "__shr__";
            break;
        case "<":
            method = "__lt__";
            break;
        case ">":
            method = "__gt__";
            break;
        case "<=":
            method = "__le__";
            break;
        case ">=":
            method = "__ge__";
            break;
        case "==":
            method = "__eq__";
            if (typel instanceof TypeNull || typer instanceof TypeNull) {
                if (!(typel instanceof NullComparable && typer instanceof NullComparable)) {
                    assert false;
                }
                type = getTypeByName("bool");
                Object ret = allocateVariable(new Object(owner, name, type));
                return ret;
                // code.addInstruction(new InstCall(id, type, new Vector<ObjectID>(Arrays.asList(lhs.id, rhs.id))));
            }
            break;
        case "!=":
            method = "__ne__";
            if (typel instanceof TypeNull || typer instanceof TypeNull) {
                if (!(typel instanceof NullComparable && typer instanceof NullComparable)) {
                    assert false;
                }
                type = getTypeByName("bool");
                Object ret = allocateVariable(new Object(owner, name, type));
                // code.addInstruction(new InstCall(id, type, new Vector<ObjectID>(Arrays.asList(lhs.id, rhs.id))));
                return ret;
            }
            break;
        case "&":
            method = "__bitand__";
            break;
        case "|":
            method = "__bitor__";
            break;
        case "^":
            method = "__bitxor__";
            break;
        case "&&":
            method = "__lgcand__";
            break;
        case "||":
            method = "__lgcor__";
            break;
        default:
            assert false;
            return null;
        }

        Func func = lhs.type.getTypeClass().getMethod(method);

        if (func == null) {
            assert false;
            return null;
        }

        ParamList params = new ParamList(rhs.type);

        if (!func.getParams().match(params)) {
            assert false;
            return null;
        }

        type = func.getRtype();

        // TODO a && b
        Object ret = allocateVariable(new Object(trace.getCurrentClass(), null, type));
        manager.addInstruction(new InstCall(ret, func.getAddr(), new ArrayList<Object>(Arrays.asList(lhs, rhs))));

        return ret;
    }

    private Type getTypeByName(String typeName) {
        if (typeName.endsWith("[]")) {
            return new TypeArray(getTypeByName(typeName.substring(0, typeName.length() - 2)),
                    ir.classList.get("__array__"));
        } else {
            Type type = ir.typeTable.get(typeName);
            if (type == null) {
                assert false;
            }
            return type;
        }
    }

    private Object allocateVariable(Object obj) {
        LOGGER.fine("alloc " + obj.name + ": " + obj.type);
        FuncExtra func = trace.getCurrentFunc();
        if (func == null) {
            func = (FuncExtra) ir.funcList.get(new FuncAddr().add("__init__"));
        }
        return func.allocate(obj);
    }

    private void defineVar(Object obj) {
        if (state != VisitState.SEMANTIC_ANALYSIS) {
            return;
        }

        if (!trace.canAllocate(obj.name)) {
            assert false;
        }
        trace.addVar(obj);
    }
}