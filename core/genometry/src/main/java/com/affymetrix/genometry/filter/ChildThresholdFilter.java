package com.affymetrix.genometry.filter;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.general.Parameter;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;

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
