package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

/**
 *
 * @author Anuj
 * This class is used to filter the given symmetry based on its span length 
 * with the threshold value of the filter this is used in FindJunctionOperator class
 */
public class ChildThresholdFilter extends AbstractFilter{

    int threshold;
    
    @Override
    public String getName() {
        return null;
    }

	@Override
	public String getDisplay() {
		return getName();
	}
	
    @Override
    public boolean setParam(Object o) {
        threshold = (Integer)o;
        return true;
    }

    @Override
    public Object getParam() {
        return threshold;
    }

    @Override
    public boolean filterSymmetry(BioSeq bioseq, SeqSymmetry ss) {
		SeqSpan span = ss.getSpan(bioseq);
        if((span.getMax() - span.getMin()) < threshold)
            return false;
        else
            return true;
    }
}
