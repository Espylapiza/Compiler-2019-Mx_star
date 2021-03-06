package com.github.espylapiza.compiler_mxstar.pizza_ir;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ParamList extends ProgramFragment implements Iterable<Object> {
    private List<Object> params;

    public ParamList() {
        params = new ArrayList<Object>();
    }

    public ParamList(Object obj) {
        params = new ArrayList<Object>();
        params.add(obj);
    }

    public ParamList(Object obj1, Object obj2) {
        params = new ArrayList<Object>();
        params.add(obj1);
        params.add(obj2);
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

    public Object get(int n) {
        return params.get(n);
    }

    public Object set(int n, Object obj) {
        return params.set(n, obj);
    }

    public int count() {
        return params.size();
    }

    public boolean match(ParamList rhs) {
        if (params.size() != rhs.get().size()) {
            return false;
        }
        for (int i = 0; i < params.size(); i++) {
            Type typeActual = params.get(i).type, typeFormal = rhs.get().get(i).type;
            if (typeActual instanceof TypeNull) {
                if (!(typeFormal instanceof NullComparable)) {
                    return false;
                }
            } else if (!params.get(i).type.equals(rhs.get().get(i).type)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Iterator<Object> iterator() {
        return params.iterator();
    }
}
