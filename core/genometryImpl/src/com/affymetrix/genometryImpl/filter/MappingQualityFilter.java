package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.BAMSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

/**
 *
 * @author hiralv
 */
public class MappingQualityFilter extends AbstractFilter {

	@Override
    public String getName() {
        return "Mapping Quality Score > 33";
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
    public boolean filterSymmetry(BioSeq bioseq, SeqSymmetry sym) {
		if(sym instanceof BAMSym) {
			int score = ((BAMSym) sym).getMapq();
			return score != BAMSym.NO_MAPQ && score > 33;
		}
		return false;
	}
	
}
