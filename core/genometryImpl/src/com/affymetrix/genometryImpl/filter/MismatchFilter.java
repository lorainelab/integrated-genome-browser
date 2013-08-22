package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithResidues;
import java.util.BitSet;

/**
 *
 * @author hiralv
 */
public class MismatchFilter extends AbstractFilter {
	
	@Override
    public String getName() {
        return "Mismatch Filter";
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
		int child_count = ss.getChildCount();
		if (child_count > 0) {
			SeqSymmetry child;
			for (int i = 0; i < child_count; i++) {
				child = ss.getChild(i);
				if (child instanceof SymWithResidues) {
					if(filter((SymWithResidues)child)){
						return true;
					}
				}
			}
		} else if (ss instanceof SymWithResidues) {
			return filter((SymWithResidues)ss);
		}
		return false;
    }
  
	private boolean filter(SymWithResidues swr) {
		BitSet bitSet = swr.getResidueMask();
		if (bitSet == null) {
			return true;
		} else if (bitSet != null && bitSet.cardinality() > 0) {
			return true;
		}
		return false;
	}
}