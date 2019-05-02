package com.github.espylapiza.compiler_mxstar.pizza_ir;

import java.util.ArrayList;
import java.util.List;

public class ParamList extends ProgramFragment {
    private List<Object> params;

    public ParamList() {
        params = new ArrayList<Object>();
    }

    public ParamList(Object obj) {
        params = new ArrayList<Object>();
        params.add(obj);
    }

    public ParamList(List<Object> objs) {
        params = objs;
    }


    public void add(Object obj) {
        params.add(obj);
    }

    public List<Object> get() {
        return params;
    }

    public int count() {
        return params.size();
    }

    public boolean match(ParamList rhs) {
        if (params.size() != rhs.get().size()) {
            return false;
        }
        for (int i = 0; i < params.size(); i++) {
            if (!params.get(i).type.equals(rhs.get().get(i).type)) {
                return false;
            }
        }
        return true;
    }
}
