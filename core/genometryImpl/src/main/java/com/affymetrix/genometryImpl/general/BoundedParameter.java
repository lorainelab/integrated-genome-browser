package com.affymetrix.genometryImpl.general;

import java.util.List;

/**
 *
 * @author hiralv
 */
@SuppressWarnings("unchecked")
public class BoundedParameter<E> extends Parameter<E> {

    private final List<E> values;

    public BoundedParameter(List<E> values) {
        super(values.get(0));
        this.values = values;
    }

    @Override
    public boolean set(Object e) {
        if (values.contains(e)) {
            super.set(e);
        }
        return false;
    }

    public List<E> getValues() {
        return values;
    }
}
