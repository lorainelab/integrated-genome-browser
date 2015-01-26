package com.affymetrix.genometry.symmetry.impl;

import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.SupportsCdsSpan;

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
