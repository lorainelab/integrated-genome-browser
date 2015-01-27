package com.affymetrix.genometry.general;

/**
 *
 * @author hiralv
 */
@SuppressWarnings("unchecked")
public class Parameter<E> {

    private final E default_value;
    private E e;

    public Parameter(Object default_value) {
        this.e = (E) default_value;
        this.default_value = (E) default_value;
    }

    public E get() {
        return e;
    }

    public boolean set(Object e) {
        this.e = (E) e;
        return true;
    }

    public void reset() {
        e = default_value;
    }

    @Override
    public String toString() {
        if (e == null) {
            return "";
        }
        return e.toString();
    }
}
