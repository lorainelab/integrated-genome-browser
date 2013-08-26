package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithBaseQuality;

/**
 *
 * @author hiralv
 */
public class QualityScoreFilter extends AbstractFilter {

	@Override
    public String getName() {
        return "Average Quality Score > 33";
    }

	@Override
	public String getDisplay() {
		return getName();
	}
	
    @Override
    public boolean setParam(Object o) {
        return false;
    }

    @Override
    public Object getParam() {
        return null;
    }

    @Override
    public boolean filterSymmetry(BioSeq bioseq, SeqSymmetry ss) {
		if (ss instanceof SymWithBaseQuality) {
			return ((SymWithBaseQuality)ss).getAverageQuality() > 33;
		}
		return false;
    }
}
