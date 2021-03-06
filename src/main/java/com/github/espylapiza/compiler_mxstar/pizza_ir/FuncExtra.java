package com.github.espylapiza.compiler_mxstar.pizza_ir;

import java.util.ArrayList;
import java.util.List;

public class FuncExtra extends FuncDefinition implements PizzaIRPart {
    private VarList varList = new VarList();
    private final List<BasicBlock> scps = new ArrayList<BasicBlock>();

    @Override
    public void accept(PizzaIRPartBaseVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * Construct a func with params.
     * @param addr
     * @param name
     * @param rtype
     * @param params
     */
    public FuncExtra(FuncAddr addr, String name, Type rtype, ParamList params) {
        super(addr, name, rtype, params);
    }

    /**
     * Construct a func with params.
     * @param addr
     * @param name
     * @param rtype
     * @param params
     * @param ownerClass
     */
    public FuncExtra(FuncAddr addr, String name, Type rtype, ParamList params, Class ownerClass) {
        super(addr, name, rtype, params, ownerClass);
    }

    public void setVarList(VarList varList) {
        this.varList = varList;
    }

    /**
     * @return the varList
     */
    public VarList getVarList() {
        return varList;
    }

    /**
     * @return the scps
     */
    public List<BasicBlock> getBlocks() {
        return scps;
    }

    /**
     * @return the params
     */
    public ParamList getParams() {
        return params;
    }

    /**
     * @param params the params to set
     */
    public void setParams(ParamList params) {
        this.params = params;
    }

    /**
     * Allocate a variable for the func.
     * @param obj
     */
    public void allocateVariable(Object obj) {
        obj.setID(new ObjectID(varList.count()));
        varList.add(obj);
    }

    @Override
    public String toString() {
        if (scps.isEmpty()) {
            return "";
        }

        String result = "func " + getAddr() + " (\n";
        for (int i = 0; i < params.count(); i++) {
            result += "\t" + params.get(i) + ": " + params.get(i).type.getName() + "\n";
        }
        result += ") {\n";
        result += "\tvar: (\n";
        for (int i = params.count(); i < varList.count(); i++) {
            if (varList.get(i).name != null) {
                result += "\t\t" + varList.get(i) + ": " + varList.get(i).type.getName() + ", " + varList.get(i).name
                        + "\n";
            }
        }
        result += "\t), (\n";
        for (int i = params.count(); i < varList.count(); i++) {
            if (varList.get(i).name == null) {
                result += "\t\t" + varList.get(i) + ": " + varList.get(i).type.getName() + "\n";
            }
        }
        result += "\t)\n";
        boolean first = true;
        for (BasicBlock scp : scps) {
            if (first) {
                first = false;
            } else {
                result += "\n";
            }
            result += scp.toString();
        }
        result += "}";
        return result;
    }

    public VarList getDefinedVariables() {
        VarList result = new VarList();
        for (int i = 0; i < varList.count(); i++) {
            if (varList.get(i).name != null) {
                result.add(varList.get(i));
            }
        }
        return result;
    }
}
