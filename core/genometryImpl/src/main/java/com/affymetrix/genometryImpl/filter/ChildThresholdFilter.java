package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.Parameter;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;

/**
 *
 * @author Anuj This class is used to filter the given symmetry based on its
 * span length with the threshold value of the filter this is used in
 * FindJunctionOperator class
 */
public class ChildThresholdFilter extends SymmetryFilter {

    private final static String THRESHOLD = "threshold";
    private final static int DEFAULT_THRESHOLD = 5;

    private Parameter<Integer> threshold = new Parameter<>(DEFAULT_THRESHOLD);

    public ChildThresholdFilter() {
        super();
        parameters.addParameter(THRESHOLD, Integer.class, threshold);
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getDisplay() {
        return getName();
    }

    @Override
    public boolean filterSymmetry(BioSeq bioseq, SeqSymmetry ss) {
        SeqSpan span = ss.getSpan(bioseq);
        int spanLength = span.getMax() - span.getMin();
        return spanLength >= threshold.get();
    }
}
