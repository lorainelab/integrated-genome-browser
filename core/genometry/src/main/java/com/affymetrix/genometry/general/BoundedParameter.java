package com.affymetrix.genometry.general;

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
            return super.set(e); //IGBF-3640: Bug Fix, Added return statement when the value is set.Otherwise, this function returns false irrespective of the outcome.
        }
        return false;
    }

    public List<E> getValues() {
        return values;
    }
}
