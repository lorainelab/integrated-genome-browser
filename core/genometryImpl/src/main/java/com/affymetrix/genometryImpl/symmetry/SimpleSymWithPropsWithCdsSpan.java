package com.affymetrix.genometryImpl.symmetry;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SupportsCdsSpan;

/**
 *
 * @author hiralv
 */
public class SimpleSymWithPropsWithCdsSpan extends SimpleSymWithProps implements SupportsCdsSpan {

    private SeqSpan cdsSpan;

    public SimpleSymWithPropsWithCdsSpan() {
        super();
    }

    public SimpleSymWithPropsWithCdsSpan(int estimated_child_count) {
        super(estimated_child_count);
    }

    @Override
    public boolean hasCdsSpan() {
        return cdsSpan != null;
    }

    @Override
    public SeqSpan getCdsSpan() {
        return cdsSpan;
    }

    public void setCdsSpan(SeqSpan cdsSpan) {
        this.cdsSpan = cdsSpan;
    }
}
