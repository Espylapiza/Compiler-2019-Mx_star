package com.github.espylapiza.compiler_mxstar.front_end;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import com.github.espylapiza.compiler_mxstar.parser.Mx_starBaseVisitor;
import com.github.espylapiza.compiler_mxstar.parser.Mx_starParser;
import com.github.espylapiza.compiler_mxstar.parser.Mx_starParser.ConstantContext;
import com.github.espylapiza.compiler_mxstar.parser.Mx_starParser.StatementContext;
import com.github.espylapiza.compiler_mxstar.pizza_ir.ObjectBool;
import com.github.espylapiza.compiler_mxstar.pizza_ir.ObjectFunction;
import com.github.espylapiza.compiler_mxstar.pizza_ir.Object;
import com.github.espylapiza.compiler_mxstar.pizza_ir.ObjectPtr;
import com.github.espylapiza.compiler_mxstar.pizza_ir.Class;
import com.github.espylapiza.compiler_mxstar.pizza_ir.Domain;
import com.github.espylapiza.compiler_mxstar.pizza_ir.FuncDefinition;
import com.github.espylapiza.compiler_mxstar.pizza_ir.FuncExtra;
import com.github.espylapiza.compiler_mxstar.pizza_ir.FuncAddr;
import com.github.espylapiza.compiler_mxstar.pizza_ir.FuncBuiltin;
import com.github.espylapiza.compiler_mxstar.pizza_ir.InstAlloc;
import com.github.espylapiza.compiler_mxstar.pizza_ir.InstBr;
import com.github.espylapiza.compiler_mxstar.pizza_ir.InstCall;
import com.github.espylapiza.compiler_mxstar.pizza_ir.InstJump;
import com.github.espylapiza.compiler_mxstar.pizza_ir.InstMov;
import com.github.espylapiza.compiler_mxstar.pizza_ir.InstOffset;
import com.github.espylapiza.compiler_mxstar.pizza_ir.InstLoad;
import com.github.espylapiza.compiler_mxstar.pizza_ir.InstRet;
import com.github.espylapiza.compiler_mxstar.pizza_ir.InstStore;
import com.github.espylapiza.compiler_mxstar.pizza_ir.ObjectInt;
import com.github.espylapiza.compiler_mxstar.pizza_ir.ObjectMethod;
import com.github.espylapiza.compiler_mxstar.pizza_ir.DomainLoop;
import com.github.espylapiza.compiler_mxstar.pizza_ir.Func;
import com.github.espylapiza.compiler_mxstar.pizza_ir.NullComparable;
import com.github.espylapiza.compiler_mxstar.pizza_ir.ObjectNull;
import com.github.espylapiza.compiler_mxstar.pizza_ir.PizzaIR;
import com.github.espylapiza.compiler_mxstar.pizza_ir.Pointer;
import com.github.espylapiza.compiler_mxstar.pizza_ir.ProgramFragment;
import com.github.espylapiza.compiler_mxstar.pizza_ir.BasicBlock;
import com.github.espylapiza.compiler_mxstar.pizza_ir.BlockType;
import com.github.espylapiza.compiler_mxstar.pizza_ir.ObjectString;
import com.github.espylapiza.compiler_mxstar.pizza_ir.ParamList;
import com.github.espylapiza.compiler_mxstar.pizza_ir.Type;
import com.github.espylapiza.compiler_mxstar.pizza_ir.TypeArray;
import com.github.espylapiza.compiler_mxstar.pizza_ir.TypeBool;
import com.github.espylapiza.compiler_mxstar.pizza_ir.TypeCustomClass;
import com.github.espylapiza.compiler_mxstar.pizza_ir.TypeEntity;
import com.github.espylapiza.compiler_mxstar.pizza_ir.TypeInt;
import com.github.espylapiza.compiler_mxstar.pizza_ir.TypeNull;
import com.github.espylapiza.compiler_mxstar.pizza_ir.TypeString;
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

    private FuncExtra mainFunc;
    private final FuncExtra initFunc;
    private final Class arrayClass;

    Mx_starParseTreeVisitor(PizzaIR ir) {
        this.ir = ir;
        initFunc = (FuncExtra) getFuncByAddr(FuncAddr.createGlobalFuncAddr("_init"));
        arrayClass = getClassByName("__array__");
    }

    @Override
    public ProgramFragment visitProgram(Mx_starParser.ProgramContext ctx) {
        LOGGER.info("TYPE_DECLARATION");
        state = VisitState.TYPE_DECLARATION;
        ctx.programSection().forEach(ch -> visit(ch));

        LOGGER.info("DECLARATION");
        state = VisitState.DECLARATION;
        ctx.programSection().forEach(ch -> visit(ch));

        mainFunc = (FuncExtra) getFuncByAddr(FuncAddr.createGlobalFuncAddr("main"));
        if (mainFunc == null || !(mainFunc.getRtype() instanceof TypeInt) || mainFunc.getParams().count() != 0) {
            assert false;
        }

        manager.enter(initFunc);
        manager.pushScope(manager.newScope(BlockType.FUNC_ENTRANCE));

        LOGGER.info("SEMANTIC_ANALYSIS");
        state = VisitState.SEMANTIC_ANALYSIS;
        ctx.programSection().forEach(ch -> visit(ch));

        manager.addInstruction(new InstRet());
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
            trace.enter(getClassByName(name));
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
            FuncExtra func = (FuncExtra) visit(ctx.functionDefinitionStatement());
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
        FuncExtra func = (FuncExtra) visit(ctx.constructionFunctionStatement());

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
        FuncExtra func = (FuncExtra) visit(ctx.functionDefinitionStatement());

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
        FuncAddr addr = FuncAddr.createMethodAddr(owner, name);

        FuncExtra func;
        if (state == VisitState.DECLARATION) {
            func = new FuncExtra(addr, name, rtype, null, owner);
        } else {
            func = (FuncExtra) getFuncByAddr(addr);
        }

        LOGGER.fine("enter construction function: " + name);
        trace.enter(func);

        if (state == VisitState.SEMANTIC_ANALYSIS) {
            defineVar(allocateVariable(new Object(func, "this", getTypeByName(trace.getCurrentClass().getName()))),
                    false);
        }

        ParamList params = (ParamList) visit(ctx.paramListDefinition());

        func.setParams(params);
        if (state == VisitState.SEMANTIC_ANALYSIS) {
            manager.enter(func);
            manager.pushScope(manager.newScope(BlockType.FUNC_ENTRANCE));

            if (ctx.statements() != null) {
                visit(ctx.statements());
            }

            manager.addInstruction(new InstRet());
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
        FuncAddr addr;
        if (owner == null && name.equals("main")) {
            addr = FuncAddr.createGlobalFuncAddr(name);
        } else {
            addr = FuncAddr.createMethodAddr(owner, name);
        }

        FuncExtra func;
        if (state == VisitState.DECLARATION) {
            func = new FuncExtra(addr, name, rtype, null, owner);
        } else {
            func = (FuncExtra) getFuncByAddr(addr);
        }

        LOGGER.fine("enter function: " + name);
        trace.enter(func);

        if (state == VisitState.SEMANTIC_ANALYSIS && owner != null) {
            defineVar(allocateVariable(new Object(func, "this", getTypeByName(trace.getCurrentClass().getName()))),
                    false);
        }

        ParamList params = (ParamList) visit(ctx.paramListDefinition());

        func.setParams(params);
        if (state == VisitState.SEMANTIC_ANALYSIS) {
            manager.enter(func);
            BasicBlock scope = manager.newScope(BlockType.FUNC_ENTRANCE);
            manager.pushScope(scope);

            if (func == mainFunc) {
                scope.addInstruction(new InstCall(initFunc, new ParamList()));
            }

            if (ctx.statements() != null) {
                visit(ctx.statements());
            }

            manager.addInstruction(new InstRet());
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
            params.add(variable);
        });
        return params;
    }

    @Override
    public ProgramFragment visitParamList(Mx_starParser.ParamListContext ctx) {
        ParamList params = new ParamList();
        ctx.object().forEach(member -> {
            Object variable = unwrap((Object) visit(member));
            params.add(variable);
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
    // public ProgramFragment visitStmtEmptyStatement(Mx_starParser.StmtEmptyStatementContext ctx) {
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
            Object obj = unwrap((Object) visit(ctx.object()));

            Type rtype = trace.getRtype();
            if (obj.type instanceof TypeNull) {
                if (!(rtype instanceof NullComparable)) {
                    assert false;
                    return null;
                }
                manager.addInstruction(new InstRet(obj));
            } else {
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

        BasicBlock scpLoop, scpLoopBody, scpLoopTail, scpEndLoop;

        scpLoop = manager.newScope(BlockType.LOOP);
        scpLoopBody = manager.newScope(BlockType.LOOPBODY);
        scpLoopTail = manager.newScope(BlockType.LOOPTAIL);
        scpEndLoop = manager.newScope(BlockType.ENDLOOP);

        manager.addInstruction(new InstJump(scpLoop));
        manager.popScope();
        manager.pushScope(scpEndLoop);
        manager.pushScope(scpLoopTail);
        manager.pushScope(scpLoop);

        Object obj = unwrap((Object) visit(ctx.object()));

        if (!(obj.type instanceof TypeBool)) {
            assert false;
        }

        manager.addInstruction(new InstBr(obj, scpLoopBody, scpEndLoop));

        manager.popScope();
        manager.pushScope(scpLoopBody);

        visit(ctx.statement());

        manager.popScope();
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

        BasicBlock scpLoop, scpLoopBody, scpLoopTail, scpEndLoop;

        scpLoop = manager.newScope(BlockType.LOOP);
        scpLoopBody = manager.newScope(BlockType.LOOPBODY);
        scpLoopTail = manager.newScope(BlockType.LOOPTAIL);
        scpEndLoop = manager.newScope(BlockType.ENDLOOP);

        manager.addInstruction(new InstJump(scpLoop));
        manager.popScope();
        manager.pushScope(scpEndLoop);
        manager.pushScope(scpLoopTail);
        manager.pushScope(scpLoop);

        if (ctx.forCondition().forCondition2() != null) {
            Object obj = unwrap((Object) visit(ctx.forCondition().forCondition2().object()));
            if (!(obj.type instanceof TypeBool)) {
                assert false;
            }
            manager.addInstruction(new InstBr(obj, scpLoopBody, scpEndLoop));
        } else {
            manager.addInstruction(new InstJump(scpLoopBody));
        }

        manager.popScope();
        manager.pushScope(scpLoopBody);

        visit(ctx.statement());

        manager.popScope();

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
        Object obj = unwrap((Object) visit(ctx.object()));

        if (!(obj.type instanceof TypeBool)) {
            assert false;
        }

        BasicBlock scpIfTrue, scpIfFalse, scpEndIf;

        scpIfTrue = manager.newScope(BlockType.IF);
        scpEndIf = manager.newScope(BlockType.ENDIF);

        if (ctx.else_stmt != null) {
            scpIfFalse = manager.newScope(BlockType.ELSE);
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

        Object dst;
        if (type instanceof TypeEntity) {
            dst = allocateVariable(new Object(trace.getCurrentFunc(), name, type));
        } else {
            dst = allocateVariable(new Object(trace.getCurrentFunc(), name, type));
        }
        defineVar(dst, false);

        return dst;
    }

    @Override
    public ProgramFragment visitVariableDefinition(Mx_starParser.VariableDefinitionContext ctx) {
        Object src = unwrap((Object) visit(ctx.object()));

        String name = ctx.Identifier().getText();
        Type type = getTypeByName(ctx.type().getText());

        if (type == null || type instanceof TypeVoid) {
            assert false;
            return null;
        }

        if (src.type instanceof TypeNull) {
            if (!(type instanceof NullComparable)) {
                assert false;
                return null;
            }
        } else {
            if (!type.equals(src.type)) {
                assert false;
                return null;
            }
        }

        Object dst;
        if (type instanceof TypeEntity) {
            dst = allocateVariable(new Object(trace.getCurrentFunc(), name, type));
        } else {
            dst = allocateVariable(new Object(trace.getCurrentFunc(), name, type));
        }

        defineVar(dst, false);
        manager.addInstruction(new InstMov(dst, src));

        return null;
    }

    @Override
    public ProgramFragment visitVariableAssignment(Mx_starParser.VariableAssignmentContext ctx) {
        Object src = unwrap((Object) visit(ctx.object()));
        Object dst = (Object) visit(ctx.lvalue());

        Type type = dst.type;

        if (src.type instanceof TypeNull) {
            if (!(type instanceof NullComparable)) {
                assert false;
                return null;
            }
        } else {
            if (!type.equals(src.type)) {
                assert false;
                return null;
            }
        }

        if (dst instanceof ObjectPtr) {
            manager.addInstruction(new InstStore(((ObjectPtr) dst).obj, src));
        } else {
            manager.addInstruction(new InstMov(dst, src));
        }

        return null;
    }

    ////////////////////////////// Object //////////////////////////////

    @Override
    public ProgramFragment visitThisLvalue(Mx_starParser.ThisLvalueContext ctx) {
        if (trace.isGlobal()) {
            assert false;
            return null;
        }
        return trace.getVar("this");
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
    public ProgramFragment visitIdentifierLvalue(Mx_starParser.IdentifierLvalueContext ctx) {
        String name = ctx.Identifier().getText();

        Object src = trace.getVar(name);
        FuncExtra func = trace.getCurrentFunc();

        if (src != null && src.belong == func) {
            // local variable
            return src;
        }

        if (!trace.isGlobal()) {
            Class class1 = trace.getCurrentClass();

            if (class1.hasVariable(name)) {
                // member variable
                FuncDefinition funcCurrentFunc = trace.getCurrentFunc();

                Object thisObj = trace.getVar("this");

                Object dst = allocateVariable(new Object(funcCurrentFunc, null, class1.getVarType(name)));
                int index = thisObj.type.getTypeClass().getVarIndex(name);
                manager.addInstruction(new InstOffset(dst, thisObj,
                        new ObjectInt(funcCurrentFunc, null, (TypeInt) getTypeByName("int"), index)));

                return new ObjectPtr(dst);
            }
            if (class1.hasMethod(name)) {
                assert false;
                return null;
            }
        }

        if (src != null) {
            // global variable
            return src;
        }

        assert false;
        return null;
    }

    @Override
    public ProgramFragment visitIdentifierObject(Mx_starParser.IdentifierObjectContext ctx) {
        String name = ctx.Identifier().getText();

        Object src = trace.getVar(name);
        FuncExtra currentFunc = trace.getCurrentFunc();

        if (src != null && src.belong == currentFunc) {
            // local variable
            return src;
        }

        if (!trace.isGlobal()) {
            Class class1 = trace.getCurrentClass();
            if (class1.hasVariable(name)) {
                // member variable
                FuncDefinition funcCurrentFunc = trace.getCurrentFunc();

                Object thisObj = trace.getVar("this");

                Object dst = allocateVariable(new Object(funcCurrentFunc, null, class1.getVarType(name)));
                int index = thisObj.type.getTypeClass().getVarIndex(name);
                manager.addInstruction(new InstOffset(dst, thisObj,
                        new ObjectInt(funcCurrentFunc, null, (TypeInt) getTypeByName("int"), index)));
                // manager.addInstruction(new InstLoad(dst, dst));

                return new ObjectPtr(dst);
            }
            if (class1.hasMethod(name)) {
                // member method
                Object thisObj = trace.getVar("this");
                return new ObjectMethod(class1.getMethod(name), currentFunc, name, getTypeByName("__method__"),
                        thisObj);
            }
        }

        if (src != null) {
            // global variable
            return src;
        }

        Func func = getFuncByAddr(FuncAddr.createFuncAddr(name));
        if (func != null) {
            // global function
            return new ObjectFunction(func, currentFunc, name, getTypeByName("__func__"));
        }

        assert false;
        return null;
    }

    @Override
    public ProgramFragment visitNewObject(Mx_starParser.NewObjectContext ctx) {
        int cntLeftBracket = 0;
        int cntBracket = 0;

        List<Object> subscripts = new ArrayList<Object>();

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

                Object obj = unwrap((Object) visit(ch));

                if (!(obj.type instanceof TypeInt)) {
                    assert false;
                    return null;
                }

                subscripts.add(obj);
            }
        }

        Type type = getTypeByName(ctx.type().getText());

        if (type instanceof TypeVoid) {
            assert false;
            return null;
        }

        for (int i = 0; i < cntLeftBracket; i++) {
            type = new TypeArray(type, arrayClass);
        }

        FuncExtra currentFunc = trace.getCurrentFunc();
        Object dst = allocateVariable(new Object(currentFunc, null, type));

        FuncDefinition funcAdd = getTypeByName("int").getTypeClass().getMethod("__add__"),
                funcLe = getTypeByName("int").getTypeClass().getMethod("__le__");

        if (subscripts.isEmpty()) {
            Object size = allocateVariable(
                    new ObjectInt(currentFunc, null, (TypeInt) getTypeByName("int"), type.getTypeClass().getSize()));
            manager.addInstruction(new InstAlloc(dst, size));

            if (type.getTypeClass().hasMethod(type.getName())) {
                manager.addInstruction(
                        new InstCall(null, type.getTypeClass().getMethod(type.getName()), new ParamList(), dst));
                return dst;
            }
        } else {
            Object size = allocateVariable(new Object(currentFunc, null, getTypeByName("int")));
            manager.addInstruction(new InstCall(size, funcAdd, new ParamList(subscripts.get(0),
                    new ObjectInt(currentFunc, null, (TypeInt) getTypeByName("int"), 1))));
            manager.addInstruction(new InstAlloc(dst, size));
            manager.addInstruction(new InstStore(dst, subscripts.get(0)));

            Object[] subs = new Object[subscripts.size() - 1];
            BasicBlock[] scpLoops = new BasicBlock[subscripts.size() - 1];

            Object last = dst;
            for (int i = 0; i < subscripts.size() - 1; i++) {
                subs[i] = allocateVariable(new Object(currentFunc, null, getTypeByName("int")));
                manager.addInstruction(
                        new InstMov(subs[i], new ObjectInt(currentFunc, null, (TypeInt) getTypeByName("int"), 1)));

                BasicBlock scpLoopBody, scpEndLoop;

                scpLoops[i] = manager.newScope(BlockType.LOOP);
                scpLoopBody = manager.newScope(BlockType.LOOPBODY);
                scpEndLoop = manager.newScope(BlockType.ENDLOOP);

                manager.addInstruction(new InstJump(scpLoops[i]));
                manager.popScope();
                manager.pushScope(scpEndLoop);
                manager.pushScope(scpLoops[i]);

                Object condition = allocateVariable(new Object(currentFunc, null, getTypeByName("bool")));
                manager.addInstruction(new InstCall(condition, funcLe, new ParamList(subs[i], subscripts.get(i))));
                manager.addInstruction(new InstBr(condition, scpLoopBody, scpEndLoop));

                manager.popScope();
                manager.pushScope(scpLoopBody);

                size = allocateVariable(new Object(currentFunc, null, getTypeByName("int")));
                manager.addInstruction(new InstCall(size, funcAdd, new ParamList(subscripts.get(i),
                        new ObjectInt(funcAdd, null, (TypeInt) getTypeByName("int"), 1))));

                Object addr = allocateVariable(new Object(currentFunc, null, ((TypeArray) last.type).getSubType()));
                manager.addInstruction(new InstOffset(addr, last, subs[i]));

                Object next = allocateVariable(new Object(currentFunc, null, ((TypeArray) last.type).getSubType()));

                manager.addInstruction(new InstAlloc(next, size));
                manager.addInstruction(new InstStore(next, subscripts.get(i)));
                manager.addInstruction(new InstStore(addr, next));
                last = next;
            }

            for (int i = subscripts.size() - 2; i >= 0; i--) {
                manager.addInstruction(new InstCall(subs[i], funcAdd,
                        new ParamList(subs[i], new ObjectInt(funcAdd, null, (TypeInt) getTypeByName("int"), 1))));
                manager.addInstruction(new InstJump(scpLoops[i]));
                manager.popScope();
            }
        }
        return dst;
    }

    @Override
    public ProgramFragment visitConstantObject(Mx_starParser.ConstantObjectContext ctx) {
        Object obj;

        ConstantContext constant = ctx.constant();
        if (constant instanceof Mx_starParser.NullContext) {
            obj = new ObjectNull(trace.getCurrentFunc(), null, (TypeNull) getTypeByName("null"));
        } else if (constant instanceof Mx_starParser.LogicalConstantContext) {
            Boolean value = constant.getText().equals("true");
            obj = new ObjectBool(trace.getCurrentFunc(), null, (TypeBool) getTypeByName("bool"), value);
        } else if (constant instanceof Mx_starParser.IntegerConstantContext) {
            Integer value = Integer.parseInt(constant.getText());
            obj = new ObjectInt(trace.getCurrentFunc(), null, (TypeInt) getTypeByName("int"), value);
        } else if (constant instanceof Mx_starParser.StringLiteralContext) {
            String value = constant.getText();
            obj = new ObjectString(trace.getCurrentFunc(), null, (TypeString) getTypeByName("string"), value);
        } else {
            assert false;
            return null;
        }
        return allocateVariable(obj);
    }

    @Override
    public ProgramFragment visitLvalueObject(Mx_starParser.LvalueObjectContext ctx) {
        Object obj = unwrap((Object) visit(ctx.lvalue()));
        Object dst = new Object(trace.getCurrentFunc(), obj.name, obj.type);
        return dst;
    }

    @Override
    public ProgramFragment visitMemberLvalue(Mx_starParser.MemberLvalueContext ctx) {
        FuncDefinition funcCurrentFunc = trace.getCurrentFunc();

        Object src = unwrap((Object) visit(ctx.lvalue()));

        String name = ctx.Identifier().getText();
        Type type = src.type.getTypeClass().getVarType(name);

        if (type == null) {
            assert false;
            return null;
        }

        Object dst = allocateVariable(new Object(funcCurrentFunc, null, type));
        int index = src.type.getTypeClass().getVarIndex(name);
        manager.addInstruction(
                new InstOffset(dst, src, new ObjectInt(funcCurrentFunc, null, (TypeInt) getTypeByName("int"), index)));

        return new ObjectPtr(dst);
    }

    @Override
    public ProgramFragment visitMemberObject(Mx_starParser.MemberObjectContext ctx) {
        FuncDefinition funcCurrentFunc = trace.getCurrentFunc();

        String name = ctx.Identifier().getText();
        Type type;

        Object src = unwrap((Object) visit(ctx.object()));

        Class class1 = getTypeByName(src.type.getName()).getTypeClass();

        if (class1.hasVariable(name)) {
            type = class1.getVarType(name);

            Object dst = allocateVariable(new Object(funcCurrentFunc, null, type));
            int index = src.type.getTypeClass().getVarIndex(name);
            manager.addInstruction(new InstOffset(dst, src,
                    new ObjectInt(funcCurrentFunc, null, (TypeInt) getTypeByName("int"), index)));

            return new ObjectPtr(dst);
        }

        if (class1.hasMethod(name)) {
            type = getTypeByName("__method__");

            return new ObjectMethod(class1.getMethod(name), trace.getCurrentFunc(), name, type, src);
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
        ObjectFunction obj = (ObjectFunction) visit(ctx.object());

        Func func = obj.func;

        ParamList params = (ParamList) visit(ctx.paramList());

        if (!params.match(func.getParams())) {
            assert false;
            return null;
        }

        Type rtype = func.getRtype();

        Object dst;

        if (rtype instanceof TypeVoid) {
            dst = null;
        } else if (rtype instanceof Pointer) {
            dst = allocateVariable(new Object(trace.getCurrentFunc(), null, rtype));
        } else {
            dst = allocateVariable(new Object(trace.getCurrentFunc(), null, rtype));
        }

        if (obj instanceof ObjectMethod) {
            manager.addInstruction(new InstCall(dst, func, params, ((ObjectMethod) obj).who));
        } else {
            manager.addInstruction(new InstCall(dst, func, params));
        }

        return dst;
    }

    @Override
    public ProgramFragment visitSubscriptLvalue(Mx_starParser.SubscriptLvalueContext ctx) {
        FuncDefinition funcCurrentFunc = trace.getCurrentFunc();

        Object array = unwrap((Object) visit(ctx.array));

        Type type = array.type;
        if (!(type instanceof TypeArray)) {
            assert false;
            return null;
        } else {
            type = ((TypeArray) type).getSubType();
        }

        Object sub = unwrap((Object) visit(ctx.subscript));
        if (!(sub.type instanceof TypeInt)) {
            assert false;
            return null;
        }

        Func funcAdd = getTypeByName("int").getTypeClass().getMethod("__add__");

        Object sub_1 = allocateVariable(new Object(array.belong, null, sub.type));
        manager.addInstruction(new InstCall(sub_1, funcAdd,
                new ParamList(sub, new ObjectInt(funcCurrentFunc, null, (TypeInt) getTypeByName("int"), 1))));

        Object dst = allocateVariable(new Object(funcCurrentFunc, null, ((TypeArray) array.type).getSubType()));
        manager.addInstruction(new InstOffset(dst, array, sub_1));

        return new ObjectPtr(dst);
    }

    @Override
    public ProgramFragment visitSubscriptObject(Mx_starParser.SubscriptObjectContext ctx) {
        FuncDefinition funcCurrentFunc = trace.getCurrentFunc();

        Object array = unwrap((Object) visit(ctx.array));

        Type type = array.type;
        if (!(type instanceof TypeArray)) {
            assert false;
            return null;
        } else {
            type = ((TypeArray) type).getSubType();
        }

        Object sub = unwrap((Object) visit(ctx.subscript));

        if (!(sub.type instanceof TypeInt)) {
            assert false;
            return null;
        }

        Func funcAdd = getTypeByName("int").getTypeClass().getMethod("__add__");

        Object sub_1 = allocateVariable(new Object(array.belong, null, sub.type));
        manager.addInstruction(new InstCall(sub_1, funcAdd,
                new ParamList(sub, new ObjectInt(funcCurrentFunc, null, (TypeInt) getTypeByName("int"), 1))));

        Object dst = allocateVariable(new Object(funcCurrentFunc, null, ((TypeArray) array.type).getSubType()));
        manager.addInstruction(new InstOffset(dst, array, sub_1));
        // manager.addInstruction(new InstLoad(dst, dst));

        return new ObjectPtr(dst);
    }

    @Override
    public ProgramFragment visitUnaryOperatorObject(Mx_starParser.UnaryOperatorObjectContext ctx) {
        boolean isPre;
        String op;
        if (ctx.pre_op != null) {
            op = ctx.pre_op.getText();
            isPre = true;
        } else {
            op = ctx.post_op.getText();
            isPre = false;
        }

        String method = null;

        switch (op) {
        case "++":
            if (isPre) {
                method = "__preinc__";
            } else {
                method = "__postinc__";
            }
            break;
        case "--":
            if (isPre) {
                method = "__predec__";
            } else {
                method = "__postdec__";
            }
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

        Object obj;

        if (ctx.lvalue() != null) {
            obj = (Object) visit(ctx.lvalue());
        } else {
            obj = (Object) visit(ctx.object());
        }

        if (!op.equals("++") && !op.equals("--")) {
            obj = unwrap(obj);
        }

        FuncDefinition func = obj.type.getTypeClass().getMethod(method);

        if (func == null) {
            assert false;
            return null;
        }

        ParamList params = new ParamList();

        if (!params.match(func.getParams())) {
            assert false;
            return null;
        }

        Type rtype = func.getRtype();

        Object dst = allocateVariable(new Object(trace.getCurrentFunc(), null, rtype));
        manager.addInstruction(new InstCall(dst, func, new ParamList(obj)));

        return dst;
    }

    @Override
    public ProgramFragment visitBinaryOperatorObject(Mx_starParser.BinaryOperatorObjectContext ctx) {
        String op = ctx.op.getText();
        Object lhs, rhs;
        if (op.equals("&&") || op.equals("||")) {
            BasicBlock scpIfTrue, scpIfFalse, scpEndIf;

            scpIfTrue = manager.newScope(BlockType.IF);
            scpIfFalse = manager.newScope(BlockType.ELSE);
            scpEndIf = manager.newScope(BlockType.ENDIF);

            Object dst = allocateVariable(new Object(trace.getCurrentFunc(), null, getTypeByName("bool")));

            lhs = unwrap((Object) visit(ctx.object(0)));
            if (op.equals("&&")) {
                manager.addInstruction(new InstBr(lhs, scpIfTrue, scpIfFalse));
                manager.popScope();
                manager.pushScope(scpEndIf);

                manager.pushScope(scpIfTrue);
                rhs = unwrap((Object) visit(ctx.object(1)));
                manager.addInstruction(new InstMov(dst, rhs));
                manager.popScope();

                manager.pushScope(scpIfFalse);
                manager.addInstruction(new InstMov(dst,
                        new ObjectBool(trace.getCurrentFunc(), null, (TypeBool) getTypeByName("bool"), false)));
                manager.popScope();
            } else {
                manager.addInstruction(new InstBr(lhs, scpIfTrue, scpIfFalse));
                manager.popScope();
                manager.pushScope(scpEndIf);

                manager.pushScope(scpIfTrue);
                manager.addInstruction(new InstMov(dst,
                        new ObjectBool(trace.getCurrentFunc(), null, (TypeBool) getTypeByName("bool"), true)));
                manager.popScope();

                manager.pushScope(scpIfFalse);
                rhs = unwrap((Object) visit(ctx.object(1)));
                manager.addInstruction(new InstMov(dst, rhs));
                manager.popScope();
            }

            return dst;
        }

        Type type;
        lhs = unwrap((Object) visit(ctx.object(0)));
        Type typel = lhs.type;

        rhs = unwrap((Object) visit(ctx.object(1)));
        Type typer = rhs.type;

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
                Object dst = allocateVariable(new Object(trace.getCurrentFunc(), null, getTypeByName("bool")));
                FuncBuiltin func = (FuncBuiltin) getFuncByAddr(FuncAddr.createFuncAddr("addrEq"));
                manager.addInstruction(new InstCall(dst, func, new ParamList(lhs, rhs)));
                return dst;
            }
            break;
        case "!=":
            method = "__ne__";
            if (typel instanceof TypeNull || typer instanceof TypeNull) {
                if (!(typel instanceof NullComparable && typer instanceof NullComparable)) {
                    assert false;
                }
                Object dst = allocateVariable(new Object(trace.getCurrentFunc(), null, getTypeByName("bool")));
                FuncBuiltin func = (FuncBuiltin) getFuncByAddr(FuncAddr.createFuncAddr("addrNe"));
                manager.addInstruction(new InstCall(dst, func, new ParamList(lhs, rhs)));
                return dst;
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
        default:
            assert false;
            return null;
        }

        FuncDefinition func = lhs.type.getTypeClass().getMethod(method);

        if (func == null) {
            assert false;
            return null;
        }

        ParamList params = new ParamList(rhs);

        if (!params.match(func.getParams())) {
            assert false;
            return null;
        }

        type = func.getRtype();

        Object dst = allocateVariable(new Object(trace.getCurrentFunc(), null, type));
        manager.addInstruction(new InstCall(dst, func, new ParamList(lhs, rhs)));

        return dst;
    }

    private Type getTypeByName(String name) {
        if (name.endsWith("[]")) {
            return new TypeArray(getTypeByName(name.substring(0, name.length() - 2)), arrayClass);
        } else {
            Type type = ir.typeTable.get(name);
            if (type == null) {
                assert false;
            }
            return type;
        }
    }

    private Func getFuncByAddr(FuncAddr addr) {
        return ir.funcList.get(addr);
    }

    private Class getClassByName(String name) {
        return ir.classList.get(name);
    }

    private Object allocateVariable(Object obj) {
        if (state != VisitState.SEMANTIC_ANALYSIS) {
            return obj;
        }

        FuncExtra func = trace.getCurrentFunc();
        if (func == null) {
            func = (FuncExtra) initFunc;
        }
        LOGGER.fine("alloc " + obj.name + ": " + obj.type);
        func.allocateVariable(obj);
        return obj;
    }

    private void defineVar(Object obj, boolean shadowing) {
        if (state != VisitState.SEMANTIC_ANALYSIS) {
            return;
        }

        if (!shadowing && !trace.canAllocate(obj.name)) {
            assert false;
        }
        trace.addVar(obj);
    }

    private Object unwrap(Object obj) {
        if (obj instanceof ObjectPtr) {
            Object obj_load = allocateVariable(new Object(trace.getCurrentFunc(), null, obj.type));
            manager.addInstruction(new InstLoad(obj_load, ((ObjectPtr) obj).obj));
            obj = obj_load;
        }
        return obj;
    }
}
